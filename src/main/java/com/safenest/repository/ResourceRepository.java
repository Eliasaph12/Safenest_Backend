package com.safenest.repository;

import com.safenest.model.ResourceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<ResourceRecord, Long> {

    List<ResourceRecord> findAllByOrderByPriorityLevelDescUpdatedAtDesc();
}
