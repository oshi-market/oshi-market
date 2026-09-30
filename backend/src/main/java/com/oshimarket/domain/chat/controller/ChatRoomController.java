package com.oshimarket.domain.chat.controller;

import com.oshimarket.domain.chat.dto.ChatRoomResponse;
import com.oshimarket.domain.chat.service.ChatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatRoomController {

    private final ChatService chatService;

    public ChatRoomController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/members/me/chatrooms")
    public List<ChatRoomResponse> getMyChatRooms(@AuthenticationPrincipal Long memberId) {
        return chatService.getMyChatRooms(memberId);
    }

}
