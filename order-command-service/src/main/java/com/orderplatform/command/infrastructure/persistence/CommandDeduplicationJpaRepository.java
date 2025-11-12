package com.orderplatform.command.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandDeduplicationJpaRepository extends JpaRepository<CommandDeduplicationEntity, String> {
}
