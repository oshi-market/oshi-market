package com.oshimarket.domain.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.transaction.dto.TransactionHistoryResponse;
import com.oshimarket.domain.transaction.dto.TransactionRole;
import com.oshimarket.domain.transaction.entity.Transaction;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import com.oshimarket.domain.transaction.repository.TransactionRepository;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class TransactionHistoryServiceTest {

    private static final Long ME = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    private TransactionRepository transactionRepository;
    private ItemRepository itemRepository;
    private TransactionHistoryService transactionHistoryService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        itemRepository = mock(ItemRepository.class);
        transactionHistoryService = new TransactionHistoryService(transactionRepository, itemRepository);
    }

    @Test
    void role이_없으면_구매와_판매_거래를_모두_조회한다() {
        Transaction bought = withId(Transaction.request(10L, 100L, ME, OTHER_MEMBER_ID), 1L);
        Transaction sold = withId(Transaction.request(20L, 200L, OTHER_MEMBER_ID, ME), 2L);
        when(transactionRepository.findByBuyerIdOrSellerId(ME, ME, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(bought, sold), PAGEABLE, 2));
        when(itemRepository.findAllById(any())).thenReturn(List.of(
                withId(sampleItem(OTHER_MEMBER_ID, "나루토 피규어", 50_000), 10L),
                withId(sampleItem(ME, "귀멸 아크릴 스탠드", 12_000), 20L)
        ));

        Page<TransactionHistoryResponse> page = transactionHistoryService.getMyTransactions(ME, null, PAGEABLE);

        assertThat(page.getTotalElements()).isEqualTo(2);
        TransactionHistoryResponse first = page.getContent().get(0);
        assertThat(first.role()).isEqualTo(TransactionRole.BUY);
        assertThat(first.itemTitle()).isEqualTo("나루토 피규어");
        assertThat(first.itemPrice()).isEqualTo(50_000);
        assertThat(first.status()).isEqualTo(TransactionStatus.REQUESTED);
        TransactionHistoryResponse second = page.getContent().get(1);
        assertThat(second.role()).isEqualTo(TransactionRole.SELL);
        assertThat(second.itemTitle()).isEqualTo("귀멸 아크릴 스탠드");
    }

    @Test
    void role이_BUY면_구매_거래만_조회한다() {
        when(transactionRepository.findByBuyerId(ME, PAGEABLE)).thenReturn(Page.empty(PAGEABLE));

        transactionHistoryService.getMyTransactions(ME, TransactionRole.BUY, PAGEABLE);

        verify(transactionRepository).findByBuyerId(ME, PAGEABLE);
        verify(transactionRepository, never()).findBySellerId(anyLong(), any(Pageable.class));
        verify(transactionRepository, never()).findByBuyerIdOrSellerId(anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    void role이_SELL이면_판매_거래만_조회한다() {
        when(transactionRepository.findBySellerId(ME, PAGEABLE)).thenReturn(Page.empty(PAGEABLE));

        transactionHistoryService.getMyTransactions(ME, TransactionRole.SELL, PAGEABLE);

        verify(transactionRepository).findBySellerId(ME, PAGEABLE);
        verify(transactionRepository, never()).findByBuyerId(anyLong(), any(Pageable.class));
        verify(transactionRepository, never()).findByBuyerIdOrSellerId(anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    void 상품을_찾을_수_없으면_상품_정보는_null로_반환한다() {
        Transaction transaction = withId(Transaction.request(10L, 100L, ME, OTHER_MEMBER_ID), 1L);
        when(transactionRepository.findByBuyerId(ME, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(transaction), PAGEABLE, 1));
        when(itemRepository.findAllById(any())).thenReturn(List.of());

        Page<TransactionHistoryResponse> page =
                transactionHistoryService.getMyTransactions(ME, TransactionRole.BUY, PAGEABLE);

        TransactionHistoryResponse response = page.getContent().get(0);
        assertThat(response.itemId()).isEqualTo(10L);
        assertThat(response.itemTitle()).isNull();
        assertThat(response.itemPrice()).isNull();
    }

    private static Item sampleItem(Long sellerId, String title, int price) {
        return Item.register(sellerId, title, "설명", ItemCategory.FIGURE, "작품", "캐릭터", price, ItemCondition.NEW);
    }

    /** BIGSERIAL로 생성되는 id는 리플렉션으로 채워서 저장 이후 상태를 흉내낸다 (DB 없이 단위 테스트). */
    private static <T> T withId(T entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
