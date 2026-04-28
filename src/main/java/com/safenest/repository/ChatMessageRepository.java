package com.safenest.repository;

import com.safenest.model.ChatMessageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageRecord, Long> {

    List<ChatMessageRecord> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
        Long senderId,
        Long receiverId,
        Long reverseSenderId,
        Long reverseReceiverId
    );

    List<ChatMessageRecord> findBySenderIdOrReceiverIdOrderByTimestampDesc(Long senderId, Long receiverId);

    List<ChatMessageRecord> findBySessionIdOrderByTimestampAsc(Long sessionId);

    List<ChatMessageRecord> findBySessionIdInOrderByTimestampDesc(List<Long> sessionIds);

    void deleteBySessionId(Long sessionId);
}
