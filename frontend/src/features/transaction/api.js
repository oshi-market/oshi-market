import client from '../../api/client';

/** role: 'BUY' | 'SELL' | undefined(전체) */
export function fetchMyTransactions(params) {
  return client.get('/members/me/transactions', { params });
}

/** 거래 완료 후 구매자가 판매자에게 별점(1~5). 한 번 등록하면 수정·삭제 불가. */
export function createReview(transactionId, rating) {
  return client.post(`/transactions/${transactionId}/reviews`, { rating });
}
