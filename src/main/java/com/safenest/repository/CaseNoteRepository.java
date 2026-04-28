package com.safenest.repository;

import com.safenest.model.CaseNoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseNoteRepository extends JpaRepository<CaseNoteRecord, Long> {

    List<CaseNoteRecord> findByCounsellorIdOrderByCreatedAtDesc(Long counsellorId);

    List<CaseNoteRecord> findByVictimIdOrderByCreatedAtDesc(Long victimId);
}
