package com.oshimarket.domain.chat.repository;

import com.oshimarket.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);

    /** 같은 상품·같은 구매자의 방은 하나뿐 ((item_id, buyer_id) UNIQUE). 채팅 시작 시 기존 방을 돌려주는 데 쓴다. */
    Optional<ChatRoom> findByItemIdAndBuyerId(Long itemId, Long buyerId);

    /**
     * 방이 없을 때만 만든다. 이미 있으면(동시에 두 번 눌러도) 아무것도 하지 않고 0을 돌려준다.
     * save()로 UNIQUE 위반 예외를 잡는 방식은 예외 순간 트랜잭션이 rollback-only가 되어 결국 500이 나기 때문에 DB에서 처리한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO chat_room (item_id, buyer_id, seller_id)
            VALUES (:itemId, :buyerId, :sellerId)
            ON CONFLICT (item_id, buyer_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("itemId") Long itemId, @Param("buyerId") Long buyerId, @Param("sellerId") Long sellerId);

}
