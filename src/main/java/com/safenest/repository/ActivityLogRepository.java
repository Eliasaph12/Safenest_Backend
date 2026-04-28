package com.safenest.repository;

import com.safenest.model.ActivityLogRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLogRecord, Long> {

    List<ActivityLogRecord> findTop20ByOrderByCreatedAtDesc();
}
