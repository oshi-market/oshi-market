package com.oshimarket.domain.chat.repository;

import com.oshimarket.domain.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
    Page<Message> findByChatRoomId(Long chatRoomId, Pageable pageable);
}
