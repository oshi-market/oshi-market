package com.oshimarket.domain.chat.controller;

import com.oshimarket.domain.chat.dto.ChatMessageResponse;
import com.oshimarket.domain.chat.dto.ChatRoomResponse;
import com.oshimarket.domain.chat.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    // 새로 만든 방이면 201, 이미 있던 방을 돌려주면 200
    @PostMapping("/items/{itemId}/chatrooms")
    public ResponseEntity<ChatRoomResponse> openChatRoom(@PathVariable Long itemId,
                                                         @AuthenticationPrincipal Long memberId) {
        ChatService.OpenResult result = chatService.openChatRoom(itemId, memberId);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.room());
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
