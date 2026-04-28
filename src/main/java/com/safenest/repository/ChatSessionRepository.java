package com.safenest.repository;

import com.safenest.model.ChatSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSessionRecord, Long> {

    List<ChatSessionRecord> findByVictimIdOrderByUpdatedAtDesc(Long victimId);

    List<ChatSessionRecord> findByAgentIdOrderByUpdatedAtDesc(Long agentId);
}
