package com.oshimarket.domain.chat.controller;

import com.oshimarket.domain.chat.dto.ChatMessageResponse;
import com.oshimarket.domain.chat.dto.ChatRoomResponse;
import com.oshimarket.domain.chat.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/chatrooms/{chatRoomId}/messages")
    public Page<ChatMessageResponse> getMessages(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return chatService.getMessages(chatRoomId, memberId, pageable);
    }
}
