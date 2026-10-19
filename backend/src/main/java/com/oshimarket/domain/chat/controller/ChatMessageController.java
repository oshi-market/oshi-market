package com.oshimarket.domain.chat.controller;

import com.oshimarket.domain.chat.dto.ChatMessageRequest;
import com.oshimarket.domain.chat.dto.ChatMessageResponse;
import com.oshimarket.domain.chat.service.ChatService;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatMessageController {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);

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

    /**
     * 메시지 처리 중 예외를 보낸 사람에게만 알린다(/user/queue/errors).
     * BusinessException은 사용자에게 보여 줄 문구라 그대로 보내고, 그 밖의 예외(DB 오류 등)는 메시지에
     * SQL·테이블 이름이 들어갈 수 있어서 로그에만 남기고 공통 문구로 바꾼다 (GlobalExceptionHandler와 같은 기준).
     */
    @MessageExceptionHandler
    public void handleException(Exception e, SimpMessageHeaderAccessor accessor) {
        String userId = String.valueOf(accessor.getSessionAttributes().get("userId"));
        String message;
        if (e instanceof BusinessException) {
            message = e.getMessage();
        } else {
            log.error("채팅 메시지 처리 중 예외", e);
            message = ErrorCode.INTERNAL_SERVER_ERROR.getMessage();
        }
        messagingTemplate.convertAndSendToUser(userId, "/queue/errors", message);
    }
}
