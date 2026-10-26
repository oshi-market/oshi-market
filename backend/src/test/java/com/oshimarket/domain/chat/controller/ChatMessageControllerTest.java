package com.oshimarket.domain.chat.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.oshimarket.domain.chat.service.ChatService;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class ChatMessageControllerTest {

    private SimpMessagingTemplate messagingTemplate;
    private ChatMessageController controller;
    private SimpMessageHeaderAccessor accessor;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new ChatMessageController(mock(ChatService.class), messagingTemplate);
        accessor = SimpMessageHeaderAccessor.create();
        Map<String, Object> session = new HashMap<>();
        session.put("userId", 2L);
        accessor.setSessionAttributes(session);
    }

    @Test
    void BusinessException은_그_문구를_그대로_보낸다() {
        controller.handleException(new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN), accessor);

        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/errors"),
                eq(ErrorCode.CHAT_ROOM_FORBIDDEN.getMessage()));
    }

    @Test
    void DB_제약_위반은_DATA_CONFLICT_문구를_보낸다() {
        controller.handleException(
                new DataIntegrityViolationException("ERROR: insert or update on table \"message\" violates ..."),
                accessor);

        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/errors"),
                eq(ErrorCode.DATA_CONFLICT.getMessage()));
    }

    @Test
    void 그_밖의_예외는_내부_메시지_대신_공통_문구를_보낸다() {
        controller.handleException(new IllegalStateException("내부 상태 오류: table member ..."), accessor);

        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/errors"),
                eq(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
