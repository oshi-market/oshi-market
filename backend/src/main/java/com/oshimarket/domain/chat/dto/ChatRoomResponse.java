package com.oshimarket.domain.chat.dto;

import com.oshimarket.domain.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {
    private Long id;
    private Long itemId;
    private Long buyerId;
    private Long sellerId;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(chatRoom.getId(),
                chatRoom.getItemId(), chatRoom.getBuyerId(),
                chatRoom.getSellerId(), chatRoom.getCreatedAt());
    }
}
