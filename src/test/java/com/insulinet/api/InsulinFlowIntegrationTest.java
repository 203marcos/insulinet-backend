package com.insulinet.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o fluxo principal do sistema de ponta a ponta via HTTP, contra um
 * Postgres real (Testcontainers), validando o mesmo contrato de API do
 * backend Python: nomes de campo em snake_case, decimais como string,
 * status codes e regras de negocio de estoque/doses/projecao.
 */
class InsulinFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullUserJourney() throws Exception {
        String email = "flow-" + System.nanoTime() + "@example.com";

        // registro
        String registerBody = """
                {"name":"Maria Silva","email":"%s","password":"senha-forte-123"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.id").exists());

        // e-mail duplicado -> 409
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict());

        // acesso sem token -> 401
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").exists());

        // login
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", email)
                        .param("password", "senha-forte-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("access_token").asText();
        String authHeader = "Bearer " + token;

        // senha errada -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", email)
                        .param("password", "senha-errada"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // cria insulina
        String insulinBody = """
                {"name":"Lantus","concentration_units_per_ml":100,"container_volume_ml":3}
                """;

        String insulinResponse = mockMvc.perform(post("/api/insulins")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(insulinBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.concentration_units_per_ml").value("100.00"))
                .andReturn().getResponse().getContentAsString();

        long insulinId = objectMapper.readTree(insulinResponse).get("id").asLong();

        // entrada de 2 canetas -> 100 * 3 = 300.00 U cada
        String stockInResponse = mockMvc.perform(post("/api/insulins/" + insulinId + "/stock")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"containers\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].quantity_units").value("300.00"))
                .andExpect(jsonPath("$[0].movement_type").value("STOCK_IN"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(stockInResponse)).hasSize(2);

        mockMvc.perform(get("/api/insulins/" + insulinId + "/stock").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_stock_units").value("600.00"));

        mockMvc.perform(get("/api/insulins/" + insulinId + "/containers").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("SEALED"))
                .andExpect(jsonPath("$[0].remaining_units").value("300.00"));

        // dose de 350 U -> deve sangrar pelo primeiro container (300) e pegar
        // 50 do segundo, agrupados pelo mesmo group_id
        String doseResponse = mockMvc.perform(post("/api/insulins/" + insulinId + "/doses")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"units\":350}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].quantity_units").value("-300.00"))
                .andExpect(jsonPath("$[1].quantity_units").value("-50.00"))
                .andReturn().getResponse().getContentAsString();

        JsonNode doseMovements = objectMapper.readTree(doseResponse);
        long firstMovementId = doseMovements.get(0).get("id").asLong();
        assertThat(doseMovements.get(1).get("group_id").asLong()).isEqualTo(firstMovementId);

        mockMvc.perform(get("/api/insulins/" + insulinId + "/stock").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_stock_units").value("250.00"));

        // dose maior que o estoque disponivel -> 409
        mockMvc.perform(post("/api/insulins/" + insulinId + "/doses")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"units\":999999}"))
                .andExpect(status().isConflict());

        // com menos de 3 dias de historico nao ha projecao
        mockMvc.perform(get("/api/insulins/" + insulinId + "/summary").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projection_available").value(false))
                .andExpect(jsonPath("$.current_stock_units").value("250.00"));

        // ajuste para o mesmo valor atual -> 400
        mockMvc.perform(post("/api/insulins/" + insulinId + "/adjustments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual_stock_units\":250,\"notes\":\"contagem manual\"}"))
                .andExpect(status().isBadRequest());

        // ajuste para um valor diferente -> 201
        mockMvc.perform(post("/api/insulins/" + insulinId + "/adjustments")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual_stock_units\":240,\"notes\":\"contagem manual\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/insulins/" + insulinId + "/stock").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current_stock_units").value("240.00"));

        // atualiza cadastro da insulina
        mockMvc.perform(patch("/api/insulins/" + insulinId)
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Lantus Solostar","concentration_units_per_ml":100,"container_volume_ml":3,"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lantus Solostar"));

        // historico em ordem decrescente (mais recente primeiro)
        mockMvc.perform(get("/api/insulins/" + insulinId + "/history").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movement_type").value("ADJUSTMENT"));
    }
}
