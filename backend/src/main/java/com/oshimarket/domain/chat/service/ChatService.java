package com.oshimarket.domain.chat.service;

import com.oshimarket.domain.chat.dto.ChatMessageResponse;
import com.oshimarket.domain.chat.dto.ChatRoomResponse;
import com.oshimarket.domain.chat.entity.ChatRoom;
import com.oshimarket.domain.chat.entity.Message;
import com.oshimarket.domain.chat.repository.ChatRoomRepository;
import com.oshimarket.domain.chat.repository.MessageRepository;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    public ChatService(ChatRoomRepository chatRoomRepository, MessageRepository messageRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long chatRoomId, Long senderId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다. id=" + chatRoomId));

        if (!senderId.equals(chatRoom.getBuyerId()) && !senderId.equals(chatRoom.getSellerId())) {
            throw new IllegalArgumentException("채팅방 참여자만 메시지를 보낼 수 있습니다.");
        }

        Message message = Message.create(chatRoomId, senderId, content);
        Message saved = messageRepository.save(message);
        return ChatMessageResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(Long memberId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(memberId, memberId);

        return chatRooms.stream()
                .map(ChatRoomResponse::from)
                .toList();
    }
}
