package com.insulinet.api.repository;

import com.insulinet.api.model.entity.Insulin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsulinRepository extends JpaRepository<Insulin, Long> {

    Optional<Insulin> findByIdAndUserId(Long id, Long userId);

    List<Insulin> findByUserIdOrderById(Long userId);
}
