package com.oshimarket.domain.chat.service;

import com.oshimarket.domain.chat.dto.ChatMessageResponse;
import com.oshimarket.domain.chat.dto.ChatRoomResponse;
import com.oshimarket.domain.chat.entity.ChatRoom;
import com.oshimarket.domain.chat.entity.Message;
import com.oshimarket.domain.chat.repository.ChatRoomRepository;
import com.oshimarket.domain.chat.repository.MessageRepository;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.repository.ItemRepository;

import java.util.List;
import java.util.Optional;

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
    //  - 판매 완료(SOLD_OUT) 상품은 새 방만 막는다. 이미 대화하던 사람(거래한 구매자 포함)은 기존 방을 계속 연다
    //  - 같은 사람이 '채팅하기'를 동시에 두 번 눌러도 INSERT ... ON CONFLICT DO NOTHING이라 방은 하나만 생긴다
    @Transactional
    public OpenResult openChatRoom(Long itemId, Long buyerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (item.isOwnedBy(buyerId)) {
            throw new BusinessException(ErrorCode.CHAT_SELF_ITEM);
        }
        Optional<ChatRoom> existing = chatRoomRepository.findByItemIdAndBuyerId(itemId, buyerId);
        if (existing.isPresent()) {
            return new OpenResult(ChatRoomResponse.from(existing.get()), false);
        }
        if (item.isSoldOut()) {
            throw new BusinessException(ErrorCode.CHAT_ITEM_SOLD_OUT);
        }
        boolean created = chatRoomRepository.insertIfAbsent(itemId, buyerId, item.getSellerId()) == 1;
        ChatRoom room = chatRoomRepository.findByItemIdAndBuyerId(itemId, buyerId)
                .orElseThrow(() -> new IllegalStateException("채팅방 생성 직후 조회 실패 itemId=" + itemId));
        return new OpenResult(ChatRoomResponse.from(room), created);
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
