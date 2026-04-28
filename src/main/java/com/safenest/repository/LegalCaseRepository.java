package com.safenest.repository;

import com.safenest.model.LegalCaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalCaseRepository extends JpaRepository<LegalCaseRecord, Long> {

    List<LegalCaseRecord> findByAdvisorIdOrderByCreatedAtDesc(Long advisorId);

    List<LegalCaseRecord> findByVictimIdOrderByCreatedAtDesc(Long victimId);
}
