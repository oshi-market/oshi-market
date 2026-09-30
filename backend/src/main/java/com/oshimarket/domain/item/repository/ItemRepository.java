package com.oshimarket.domain.item.repository;

import com.oshimarket.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /**
     * 이 상품으로 열린 채팅방이나 거래가 있는지. 있으면 FK(chat_room/transaction → item) 때문에 삭제할 수 없고,
     * 거래 기록 보존을 위해서도 삭제를 막는다. chat 도메인 엔티티에 의존하지 않도록 native query로 확인.
     */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM chat_room WHERE item_id = :itemId)
                OR EXISTS (SELECT 1 FROM transaction WHERE item_id = :itemId)
            """, nativeQuery = true)
    boolean hasChatOrTransaction(@Param("itemId") Long itemId);
}
