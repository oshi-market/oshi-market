package com.oshimarket.domain.chat.repository;

import com.oshimarket.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);

    /** 같은 상품·같은 구매자의 방은 하나뿐 ((item_id, buyer_id) UNIQUE). 채팅 시작 시 기존 방을 돌려주는 데 쓴다. */
    Optional<ChatRoom> findByItemIdAndBuyerId(Long itemId, Long buyerId);

}
