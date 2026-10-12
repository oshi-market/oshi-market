package com.oshimarket.domain.item.repository;

import com.oshimarket.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * 거래 요청·상태 변경 시 Item row를 SELECT ... FOR UPDATE로 잠근다. 같은 상품에 동시에 거래 요청이 들어와도
     * 한 명만 SELLING → IN_TRANSACTION에 성공한다 (4_DB분석서.md 3. 동시성 제어).
     */
    // 같은 상품에 거래 요청이 동시에 들어와도 하나만 통과하도록 비관적 락(SELECT ... FOR UPDATE)으로 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);
}
