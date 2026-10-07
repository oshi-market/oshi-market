package com.oshimarket.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oshimarket.domain.chat.entity.ChatRoom;
import com.oshimarket.domain.chat.repository.ChatRoomRepository;
import com.oshimarket.domain.chat.repository.MessageRepository;
import com.oshimarket.domain.item.entity.Item;
import com.oshimarket.domain.item.entity.ItemCategory;
import com.oshimarket.domain.item.entity.ItemCondition;
import com.oshimarket.domain.item.repository.ItemRepository;
import com.oshimarket.global.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    private static final Long SELLER = 1L;
    private static final Long BUYER = 2L;
    private static final Long ITEM_ID = 10L;

    private ChatRoomRepository chatRoomRepository;
    private ItemRepository itemRepository;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatRoomRepository = mock(ChatRoomRepository.class);
        itemRepository = mock(ItemRepository.class);
        chatService = new ChatService(chatRoomRepository, mock(MessageRepository.class), itemRepository);
        Item item = withId(Item.register(SELLER, "피규어", "설명", ItemCategory.FIGURE, "작품", "캐릭터", 10_000,
                ItemCondition.NEW), ITEM_ID);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
    }

    @Test
    void openChatRoom_처음이면_방을_만든다() {
        ChatRoom inserted = withId(ChatRoom.create(ITEM_ID, BUYER, SELLER), 100L);
        when(chatRoomRepository.findByItemIdAndBuyerId(ITEM_ID, BUYER))
                .thenReturn(Optional.empty(), Optional.of(inserted));
        when(chatRoomRepository.insertIfAbsent(ITEM_ID, BUYER, SELLER)).thenReturn(1);

        ChatService.OpenResult result = chatService.openChatRoom(ITEM_ID, BUYER);

        assertThat(result.created()).isTrue();
        assertThat(result.room().getSellerId()).isEqualTo(SELLER);
    }

    @Test
    void openChatRoom_이미_있으면_그_방을_돌려준다() {
        ChatRoom existing = withId(ChatRoom.create(ITEM_ID, BUYER, SELLER), 100L);
        when(chatRoomRepository.findByItemIdAndBuyerId(ITEM_ID, BUYER)).thenReturn(Optional.of(existing));

        ChatService.OpenResult result = chatService.openChatRoom(ITEM_ID, BUYER);

        assertThat(result.created()).isFalse();
        assertThat(result.room().getId()).isEqualTo(100L);
        verify(chatRoomRepository, never()).insertIfAbsent(any(), any(), any());
    }

    @Test
    void openChatRoom_동시에_두번_눌러_다른_요청이_먼저_만들었으면_그_방을_돌려준다() {
        ChatRoom existing = withId(ChatRoom.create(ITEM_ID, BUYER, SELLER), 100L);
        when(chatRoomRepository.findByItemIdAndBuyerId(ITEM_ID, BUYER))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(chatRoomRepository.insertIfAbsent(ITEM_ID, BUYER, SELLER)).thenReturn(0); // ON CONFLICT DO NOTHING

        ChatService.OpenResult result = chatService.openChatRoom(ITEM_ID, BUYER);

        assertThat(result.created()).isFalse();
        assertThat(result.room().getId()).isEqualTo(100L);
    }

    @Test
    void openChatRoom_본인_상품이면_거절한다() {
        assertThatThrownBy(() -> chatService.openChatRoom(ITEM_ID, SELLER))
                .extracting("errorCode").isEqualTo(ErrorCode.CHAT_SELF_ITEM);
    }

    @Test
    void isParticipant_구매자_판매자만_참() {
        when(chatRoomRepository.findById(100L))
                .thenReturn(Optional.of(withId(ChatRoom.create(ITEM_ID, BUYER, SELLER), 100L)));

        assertThat(chatService.isParticipant(100L, BUYER)).isTrue();
        assertThat(chatService.isParticipant(100L, SELLER)).isTrue();
        assertThat(chatService.isParticipant(100L, 99L)).isFalse();
        assertThat(chatService.isParticipant(999L, BUYER)).isFalse();
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
