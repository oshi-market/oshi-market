package com.oshimarket.domain.chat.dto;

import com.oshimarket.domain.chat.entity.Message;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(Message message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoomId(),
                message.getSenderId(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
