package com.oshimarket.domain.chat.controller;

import com.oshimarket.domain.chat.dto.ChatMessageRequest;
import com.oshimarket.domain.chat.dto.ChatMessageResponse;
import com.oshimarket.domain.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessageController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{chatRoomId}")
    public void sendMessage(
            @DestinationVariable Long chatRoomId,
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor accessor
    ) {
        Long senderId = (Long) accessor.getSessionAttributes().get("userId");

        ChatMessageResponse response = chatService.sendMessage(chatRoomId, senderId, request.getContent());
        messagingTemplate.convertAndSend("/sub/chat/" + chatRoomId, response);
    }

    @MessageExceptionHandler
    public void handleException(Exception e, SimpMessageHeaderAccessor accessor) {
        String userId = String.valueOf(accessor.getSessionAttributes().get("userId"));
        messagingTemplate.convertAndSendToUser(userId, "/queue/errors", e.getMessage());
    }
}
