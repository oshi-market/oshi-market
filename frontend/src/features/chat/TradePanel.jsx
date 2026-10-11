import { useState } from 'react';
import { getErrorMessage } from '../../api/errors';
import { TRANSACTION_STATUS_LABEL } from '../transaction/constants';
import { changeTransactionStatus, requestTransaction } from './api';

/**
 * 채팅방 안의 거래 버튼. 백엔드 규칙(Transaction.canChangeTo)과 같은 조건으로 버튼을 보여 준다.
 * - 거래 없음/끝난 거래: 구매자만 '거래 요청'
 * - REQUESTED: 판매자 '수락', 둘 다 '취소'
 * - IN_PROGRESS: 둘 다 '거래 완료'·'취소'
 * 최종 판단은 서버가 한다(동시에 눌러도 거래 row 락으로 하나만 성공).
 */
function TradePanel({ chatRoomId, isBuyer, isSeller, transaction, onChange, onRefresh }) {
  const [error, setError] = useState('');
  const [isBusy, setIsBusy] = useState(false);
  const status = transaction?.status;
  const isActive = status === 'REQUESTED' || status === 'IN_PROGRESS';

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

      {!isActive && isBuyer && (
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
      {status === 'IN_PROGRESS' && (
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
