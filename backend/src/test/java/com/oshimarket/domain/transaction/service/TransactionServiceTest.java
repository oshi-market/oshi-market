package com.oshimarket.domain.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.chat.entity.ChatRoom;
import com.oshimarket.domain.chat.repository.ChatRoomRepository;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.entity.ItemStatus;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.domain.transaction.dto.TransactionResponse;
import com.oshimarket.domain.transaction.entity.Transaction;
import com.oshimarket.domain.transaction.entity.TransactionStatus;
import com.oshimarket.domain.transaction.event.TransactionChangedEvent;
import com.oshimarket.domain.transaction.repository.TransactionRepository;
import com.oshimarket.global.exception.BusinessException;
import com.oshimarket.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class TransactionServiceTest {

    private static final Long SELLER = 1L;
    private static final Long BUYER = 2L;
    private static final Long STRANGER = 3L;
    private static final Long ITEM_ID = 10L;
    private static final Long ROOM_ID = 20L;
    private static final Long TX_ID = 30L;

    private TransactionRepository transactionRepository;
    private ChatRoomRepository chatRoomRepository;
    private ItemRepository itemRepository;
    private ApplicationEventPublisher eventPublisher;
    private TransactionService service;
    private Item item;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        chatRoomRepository = mock(ChatRoomRepository.class);
        itemRepository = mock(ItemRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TransactionService(transactionRepository, chatRoomRepository, itemRepository, eventPublisher);

        item = withId(Item.register(SELLER, "나루토 피규어", "미개봉", ItemCategory.FIGURE, "나루토", "나루토", 50_000,
                ItemCondition.NEW), ITEM_ID);
        when(itemRepository.findByIdForUpdate(ITEM_ID)).thenReturn(Optional.of(item));
        when(chatRoomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(withId(ChatRoom.create(ITEM_ID, BUYER, SELLER), ROOM_ID)));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), TX_ID));
    }

    @Test
    void request_구매자가_요청하면_REQUESTED로_만들고_상품을_거래중으로_잠근다() {
        TransactionResponse response = service.request(ROOM_ID, BUYER);

        assertThat(response.status()).isEqualTo(TransactionStatus.REQUESTED);
        assertThat(response.buyerId()).isEqualTo(BUYER);
        assertThat(response.sellerId()).isEqualTo(SELLER);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.IN_TRANSACTION);
    }

    @Test
    void request_판매자는_거래를_요청할_수_없다() {
        assertThatThrownBy(() -> service.request(ROOM_ID, SELLER))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TRANSACTION_REQUEST_FORBIDDEN);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void request_채팅방_참여자가_아니면_거절한다() {
        assertThatThrownBy(() -> service.request(ROOM_ID, STRANGER))
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    @Test
    void request_이미_거래중인_상품이면_거절한다_동시요청에서_두번째_요청() {
        item.startTransaction(); // 먼저 들어온 요청이 락을 잡고 상태를 바꾼 상황

        assertThatThrownBy(() -> service.request(ROOM_ID, BUYER))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_SELLING);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void changeStatus_판매자가_수락하면_IN_PROGRESS() {
        givenTransaction(TransactionStatus.REQUESTED);

        assertThat(service.changeStatus(TX_ID, SELLER, TransactionStatus.IN_PROGRESS).status())
                .isEqualTo(TransactionStatus.IN_PROGRESS);
    }

    @Test
    void changeStatus_구매자는_수락할_수_없다() {
        givenTransaction(TransactionStatus.REQUESTED);

        assertThatThrownBy(() -> service.changeStatus(TX_ID, BUYER, TransactionStatus.IN_PROGRESS))
                .extracting("errorCode").isEqualTo(ErrorCode.TRANSACTION_ACCEPT_FORBIDDEN);
    }

    @Test
    void changeStatus_판매자는_완료할_수_없다() {
        givenTransaction(TransactionStatus.IN_PROGRESS);

        assertThatThrownBy(() -> service.changeStatus(TX_ID, SELLER, TransactionStatus.COMPLETED))
                .extracting("errorCode").isEqualTo(ErrorCode.TRANSACTION_COMPLETE_FORBIDDEN);
        assertThat(item.getStatus()).isNotEqualTo(ItemStatus.SOLD_OUT);
    }

    @Test
    void changeStatus_완료하면_완료시각을_남기고_상품을_판매완료로() {
        givenTransaction(TransactionStatus.IN_PROGRESS);

        TransactionResponse response = service.changeStatus(TX_ID, BUYER, TransactionStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.transactedAt()).isNotNull();
        assertThat(item.getStatus()).isEqualTo(ItemStatus.SOLD_OUT);
    }

    @Test
    void changeStatus_취소하면_상품을_다시_판매중으로() {
        item.startTransaction();
        givenTransaction(TransactionStatus.REQUESTED);

        service.changeStatus(TX_ID, BUYER, TransactionStatus.CANCELLED);

        assertThat(item.getStatus()).isEqualTo(ItemStatus.SELLING);
    }

    @Test
    void changeStatus_요청_상태에서_바로_완료할_수_없다() {
        givenTransaction(TransactionStatus.REQUESTED);

        assertThatThrownBy(() -> service.changeStatus(TX_ID, SELLER, TransactionStatus.COMPLETED))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TRANSACTION_STATUS);
    }

    @Test
    void changeStatus_완료된_거래는_취소할_수_없다() {
        givenTransaction(TransactionStatus.COMPLETED);

        assertThatThrownBy(() -> service.changeStatus(TX_ID, SELLER, TransactionStatus.CANCELLED))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TRANSACTION_STATUS);
    }

    @Test
    void changeStatus_거래_row를_잠근_뒤에_상태를_확인한다() {
        givenTransaction(TransactionStatus.REQUESTED);

        service.changeStatus(TX_ID, SELLER, TransactionStatus.IN_PROGRESS);

        verify(transactionRepository).findByIdForUpdate(TX_ID);
        verify(transactionRepository, never()).findById(any());
    }

    @Test
    void request_성공하면_거래_변경_이벤트를_남긴다() {
        service.request(ROOM_ID, BUYER);

        ArgumentCaptor<TransactionChangedEvent> captor = ArgumentCaptor.forClass(TransactionChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().transaction().status()).isEqualTo(TransactionStatus.REQUESTED);
        assertThat(captor.getValue().transaction().chatRoomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void changeStatus_성공하면_바뀐_상태로_이벤트를_남긴다() {
        givenTransaction(TransactionStatus.REQUESTED);

        service.changeStatus(TX_ID, SELLER, TransactionStatus.IN_PROGRESS);

        ArgumentCaptor<TransactionChangedEvent> captor = ArgumentCaptor.forClass(TransactionChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().transaction().status()).isEqualTo(TransactionStatus.IN_PROGRESS);
    }

    @Test
    void changeStatus_거절되면_이벤트를_남기지_않는다() {
        givenTransaction(TransactionStatus.REQUESTED);

        assertThatThrownBy(() -> service.changeStatus(TX_ID, BUYER, TransactionStatus.IN_PROGRESS))
                .isInstanceOf(BusinessException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void get_거래_당사자가_아니면_거절한다() {
        givenTransaction(TransactionStatus.REQUESTED);

        assertThatThrownBy(() -> service.get(TX_ID, STRANGER))
                .extracting("errorCode").isEqualTo(ErrorCode.TRANSACTION_FORBIDDEN);
    }

    private void givenTransaction(TransactionStatus status) {
        Transaction transaction = withId(Transaction.request(ITEM_ID, ROOM_ID, BUYER, SELLER), TX_ID);
        if (status == TransactionStatus.IN_PROGRESS || status == TransactionStatus.COMPLETED) {
            transaction.changeStatus(TransactionStatus.IN_PROGRESS);
        }
        if (status == TransactionStatus.COMPLETED) {
            transaction.changeStatus(TransactionStatus.COMPLETED);
        }
        when(transactionRepository.findById(TX_ID)).thenReturn(Optional.of(transaction));
        when(transactionRepository.findByIdForUpdate(TX_ID)).thenReturn(Optional.of(transaction));
    }

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
