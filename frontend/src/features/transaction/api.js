import client from '../../api/client';

/** role: 'BUY' | 'SELL' | undefined(전체) */
export function fetchMyTransactions(params) {
  return client.get('/members/me/transactions', { params });
}
