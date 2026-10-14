package com.oshimarket.domain.wishlist.repository;

import com.oshimarket.domain.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * 아직 찜하지 않았을 때만 저장한다. ♡를 연타해도 UNIQUE 위반 예외 없이 0을 돌려준다(멱등).
     * 채팅방 생성(ChatRoomRepository.insertIfAbsent)과 같은 이유로 save() 대신 DB에서 처리한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO wishlist (user_id, item_id)
            VALUES (:userId, :itemId)
            ON CONFLICT (user_id, item_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("itemId") Long itemId);

    /** 찜하지 않은 상태에서 불러도 0을 돌려줄 뿐 에러가 아니다(멱등). */
    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.userId = :userId AND w.itemId = :itemId")
    int deleteByUserIdAndItemId(@Param("userId") Long userId, @Param("itemId") Long itemId);

    boolean existsByUserIdAndItemId(Long userId, Long itemId);

    long countByItemId(Long itemId);

    /** 내 찜 목록. 같은 시각에 찜한 것끼리도 순서가 하나로 정해지게 id를 함께 정렬한다. */
    Page<Wishlist> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
