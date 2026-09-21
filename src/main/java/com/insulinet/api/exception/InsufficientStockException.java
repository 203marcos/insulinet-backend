package com.insulinet.api.exception;

import java.math.BigDecimal;

/**
 * Erro interno de dominio: sinaliza que o estoque distribuido entre os
 * containers disponiveis nao cobre a quantidade solicitada. E sempre
 * capturada na camada de service que orquestra a operacao (dose, ajuste,
 * etc.) e traduzida para um ConflictException com uma mensagem especifica
 * do contexto, assim como no backend Python original.
 */
public class InsufficientStockException extends RuntimeException {

    private final BigDecimal missingUnits;

    public InsufficientStockException(BigDecimal missingUnits) {
        super("Estoque insuficiente entre as canetas/frascos disponiveis.");
        this.missingUnits = missingUnits;
    }

    public BigDecimal getMissingUnits() {
        return missingUnits;
    }
}
