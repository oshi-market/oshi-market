package com.oshimarket.domain.chat.repository;

import com.oshimarket.domain.chat.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
}
