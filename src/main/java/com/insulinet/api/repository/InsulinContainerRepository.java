package com.insulinet.api.repository;

import com.insulinet.api.model.entity.InsulinContainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsulinContainerRepository extends JpaRepository<InsulinContainer, Long> {

    List<InsulinContainer> findByInsulinIdOrderByCreatedAtAscIdAsc(Long insulinId);

    Optional<InsulinContainer> findByIdAndInsulinId(Long id, Long insulinId);
}
