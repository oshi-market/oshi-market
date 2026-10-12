import client from '../../api/client';

/** 상품에서 '채팅하기' — 이미 방이 있으면 그 방, 없으면 새로 만든 방을 돌려준다. */
export function openChatRoom(itemId) {
  return client.post(`/items/${itemId}/chatrooms`);
}

/** 내가 구매자·판매자로 참여한 채팅방 목록. */
export function fetchMyChatRooms() {
  return client.get('/members/me/chatrooms');
}

/** 채팅방 메시지 내역. 서버 기본 정렬은 최신순(createdAt DESC). */
export function fetchMessages(chatRoomId, params) {
  return client.get(`/chatrooms/${chatRoomId}/messages`, { params });
}

/** 채팅방에서 거래 요청 (구매자만). */
export function requestTransaction(chatRoomId) {
  return client.post(`/chatrooms/${chatRoomId}/transactions`);
}

/** 거래 상태 변경. status: 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' */
export function changeTransactionStatus(transactionId, status) {
  return client.patch(`/transactions/${transactionId}/status`, { status });
}
