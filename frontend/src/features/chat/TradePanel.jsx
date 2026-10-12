import { useState } from 'react';
import { getErrorMessage } from '../../api/errors';
import { TRANSACTION_STATUS_LABEL } from '../transaction/constants';
import { changeTransactionStatus, requestTransaction } from './api';

/**
 * 채팅방 안의 거래 버튼. 백엔드 규칙(Transaction.canChangeTo)과 같은 조건으로 버튼을 보여 준다.
 * - 거래 없음/취소된 거래 + 상품이 판매 중: 구매자만 '거래 요청'
 *   (완료된 거래나, 다른 구매자와 거래 중인 상품은 서버가 409로 거절하므로 버튼 대신 안내)
 * - REQUESTED: 판매자 '수락', 둘 다 '취소'
 * - IN_PROGRESS: 구매자만 '거래 완료'(물건을 받은 쪽이 확정), 둘 다 '취소'
 * 최종 판단은 서버가 한다(동시에 눌러도 거래 row 락으로 하나만 성공).
 */
function TradePanel({ chatRoomId, isBuyer, isSeller, itemStatus, transaction, onChange, onRefresh }) {
  const [error, setError] = useState('');
  const [isBusy, setIsBusy] = useState(false);
  const status = transaction?.status;
  const isActive = status === 'REQUESTED' || status === 'IN_PROGRESS';
  const canRequest = isBuyer && (!transaction || status === 'CANCELLED') && itemStatus === 'SELLING';
  // 내 거래는 없는데 상품이 판매 중이 아니면 = 다른 구매자와 거래 중이거나 이미 팔림
  const unavailableNotice = isBuyer && !isActive && status !== 'COMPLETED' && itemStatus && itemStatus !== 'SELLING'
    ? (itemStatus === 'SOLD_OUT' ? '판매가 끝난 상품이에요' : '다른 구매자와 거래 중인 상품이에요')
    : '';

  async function run(action) {
    setError('');
    setIsBusy(true);
    try {
      const { data } = await action();
      onChange(data);
    } catch (err) {
      setError(getErrorMessage(err, '거래 처리에 실패했습니다.'));
      onRefresh();
    } finally {
      setIsBusy(false);
    }
  }

  return (
    <div className="chat-trade">
      <span className="chat-trade-status">
        {transaction ? (
          <>
            거래 상태
            {/* key가 바뀌면 다시 그려져서, 상태가 바뀔 때마다 배지가 한 번 튄다 */}
            <span key={status} className={`badge badge-transaction-${status.toLowerCase()} chat-trade-badge`}>
              {TRANSACTION_STATUS_LABEL[status] ?? status}
            </span>
          </>
        ) : (
          '아직 거래 요청이 없어요'
        )}
      </span>

      {unavailableNotice && <span className="muted">{unavailableNotice}</span>}
      {canRequest && (
        <button type="button" className="btn btn-primary btn-sm" disabled={isBusy}
          onClick={() => run(() => requestTransaction(chatRoomId))}>
          거래 요청
        </button>
      )}
      {status === 'REQUESTED' && isSeller && (
        <button type="button" className="btn btn-primary btn-sm" disabled={isBusy}
          onClick={() => run(() => changeTransactionStatus(transaction.id, 'IN_PROGRESS'))}>
          수락
        </button>
      )}
      {status === 'IN_PROGRESS' && isBuyer && (
        <button type="button" className="btn btn-primary btn-sm" disabled={isBusy}
          onClick={() => run(() => changeTransactionStatus(transaction.id, 'COMPLETED'))}>
          거래 완료
        </button>
      )}
      {isActive && (
        <button type="button" className="btn btn-ghost btn-sm" disabled={isBusy}
          onClick={() => run(() => changeTransactionStatus(transaction.id, 'CANCELLED'))}>
          취소
        </button>
      )}
      <button type="button" className="btn btn-ghost btn-sm" disabled={isBusy} onClick={onRefresh}
        title="거래 상태 다시 확인 (상대방 변경은 실시간으로 반영됨)">
        ↻
      </button>

      {error && (
        <p className="form-error" role="alert" style={{ width: '100%', margin: 0 }}>
          {error}
        </p>
      )}
    </div>
  );
}

export default TradePanel;
