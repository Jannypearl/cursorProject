package com.example.datamigration.repository;

import com.example.datamigration.entity.MigrationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MigrationJobRepository extends JpaRepository<MigrationJob, Long> {

    List<MigrationJob> findAllByOrderByCreatedAtDesc();
}
