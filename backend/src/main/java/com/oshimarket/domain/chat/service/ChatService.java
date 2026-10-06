package com.oshimarket.domain.chat.service;

import com.oshimarket.domain.chat.dto.ChatMessageResponse;
import com.oshimarket.domain.chat.dto.ChatRoomResponse;
import com.oshimarket.domain.chat.entity.ChatRoom;
import com.oshimarket.domain.chat.entity.Message;
import com.oshimarket.domain.chat.repository.ChatRoomRepository;
import com.oshimarket.domain.chat.repository.MessageRepository;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.repository.ItemRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ItemRepository itemRepository;

    public ChatService(ChatRoomRepository chatRoomRepository, MessageRepository messageRepository,
                       ItemRepository itemRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.itemRepository = itemRepository;
    }

    /** 채팅 시작 결과. created=false면 이미 있던 방을 돌려준 것 (컨트롤러가 201/200을 고른다). */
    public record OpenResult(ChatRoomResponse room, boolean created) {
    }

    // POST /api/items/{itemId}/chatrooms
    //  - 본인 상품이면 거절, 이미 방이 있으면 그 방 반환, 없으면 생성
    //  - 같은 사람이 '채팅하기'를 동시에 두 번 눌러 (item_id, buyer_id) UNIQUE에 걸리면 먼저 만들어진 방을 돌려준다
    @Transactional
    public OpenResult openChatRoom(Long itemId, Long buyerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (item.isOwnedBy(buyerId)) {
            throw new BusinessException(ErrorCode.CHAT_SELF_ITEM);
        }
        return chatRoomRepository.findByItemIdAndBuyerId(itemId, buyerId)
                .map(room -> new OpenResult(ChatRoomResponse.from(room), false))
                .orElseGet(() -> create(itemId, buyerId, item.getSellerId()));
    }

    private OpenResult create(Long itemId, Long buyerId, Long sellerId) {
        try {
            ChatRoom saved = chatRoomRepository.saveAndFlush(ChatRoom.create(itemId, buyerId, sellerId));
            return new OpenResult(ChatRoomResponse.from(saved), true);
        } catch (DataIntegrityViolationException e) {
            ChatRoom existing = chatRoomRepository.findByItemIdAndBuyerId(itemId, buyerId).orElseThrow(() -> e);
            return new OpenResult(ChatRoomResponse.from(existing), false);
        }
    }

    /** 채팅방 참여자(구매자/판매자)인지. STOMP 구독 권한 검사에 쓴다. */
    @Transactional(readOnly = true)
    public boolean isParticipant(Long chatRoomId, Long memberId) {
        return chatRoomRepository.findById(chatRoomId)
                .map(room -> memberId.equals(room.getBuyerId()) || memberId.equals(room.getSellerId()))
                .orElse(false);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long chatRoomId, Long senderId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!senderId.equals(chatRoom.getBuyerId()) && !senderId.equals(chatRoom.getSellerId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
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

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(Long chatRoomId, Long memberId, Pageable pageable) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!memberId.equals(chatRoom.getBuyerId()) && !memberId.equals(chatRoom.getSellerId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        return messageRepository.findByChatRoomId(chatRoom.getId(), pageable)
                .map(ChatMessageResponse::from);
    }
}
