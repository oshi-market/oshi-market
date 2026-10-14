import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import ReviewForm from './ReviewForm';
import { fetchMyTransactions } from './api';
import { ROLE_LABEL, ROLE_TABS, TRANSACTION_STATUS_LABEL } from './constants';

const EMPTY_PAGE = { content: [], number: 0, totalPages: 0 };

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString('ko-KR') : '-';
}

function MyTransactionsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(EMPTY_PAGE);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  /** 별점 입력창을 펼친 거래 id (한 번에 하나만). */
  const [reviewingId, setReviewingId] = useState(null);

  const role = searchParams.get('role') ?? undefined;
  const pageNumber = Number(searchParams.get('page') ?? 0);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchMyTransactions({ role, page: pageNumber, size: 20 })
      .then(({ data }) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '거래 내역을 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [role, pageNumber]);

  /** 탭을 바꾸면 첫 페이지부터 다시 본다. */
  function selectRole(next) {
    setSearchParams(next ? { role: next } : {});
  }

  /** 별점 등록에 성공하면 다시 불러오지 않고 그 거래만 별점이 있는 상태로 바꾼다. */
  function handleReviewDone(transactionId, rating) {
    setPage((prev) => ({
      ...prev,
      content: prev.content.map((t) => (t.id === transactionId ? { ...t, rating } : t)),
    }));
    setReviewingId(null);
  }

  function goToPage(next) {
    const params = new URLSearchParams(searchParams);
    params.set('page', String(next));
    setSearchParams(params);
  }

  return (
    <div className="page">
      <div className="page-header">
        <h2>내 거래</h2>
      </div>

      <div className="transaction-tabs" role="tablist">
        {ROLE_TABS.map((tab) => (
          <button
            type="button"
            role="tab"
            key={tab.label}
            aria-selected={role === tab.value}
            className={`btn btn-sm ${role === tab.value ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => selectRole(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && page.content.length === 0 && <p className="muted">거래 내역이 없습니다.</p>}

      <ul className="transaction-list">
        {page.content.map((transaction) => {
          const canReview =
            transaction.role === 'BUY' && transaction.status === 'COMPLETED' && transaction.rating == null;
          return (
            <li key={transaction.id}>
              <Link to={`/items/${transaction.itemId}`} className="transaction-row">
                <span className="badge badge-outline">{ROLE_LABEL[transaction.role]}</span>
                <div className="transaction-row-body">
                  <p className="transaction-row-title">{transaction.itemTitle ?? '삭제된 상품'}</p>
                  <p className="muted">
                    {transaction.itemPrice != null && `${transaction.itemPrice.toLocaleString()}원 · `}
                    {transaction.status === 'COMPLETED'
                      ? `${formatDate(transaction.transactedAt)} 완료`
                      : `${formatDate(transaction.createdAt)} 요청`}
                  </p>
                </div>
                <span className={`badge badge-transaction-${transaction.status.toLowerCase()}`}>
                  {TRANSACTION_STATUS_LABEL[transaction.status]}
                </span>
              </Link>
              {transaction.status === 'COMPLETED' && (
                <div className="transaction-review">
                  {transaction.rating != null && (
                    <span className="muted">
                      {transaction.role === 'BUY' ? '내가 준 별점' : '받은 별점'}{' '}
                      <span className="review-stars-static" aria-label={`${transaction.rating}점`}>
                        {'★'.repeat(transaction.rating)}
                        <span className="is-off">{'★'.repeat(5 - transaction.rating)}</span>
                      </span>
                    </span>
                  )}
                  {canReview && reviewingId !== transaction.id && (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => setReviewingId(transaction.id)}
                    >
                      별점 주기
                    </button>
                  )}
                  {canReview && reviewingId === transaction.id && (
                    <ReviewForm
                      transactionId={transaction.id}
                      onDone={(rating) => handleReviewDone(transaction.id, rating)}
                      onCancel={() => setReviewingId(null)}
                    />
                  )}
                </div>
              )}
            </li>
          );
        })}
      </ul>

      {page.totalPages > 1 && (
        <div className="pagination">
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={page.number <= 0}
            onClick={() => goToPage(page.number - 1)}
          >
            이전
          </button>
          <span className="muted">
            {page.number + 1} / {page.totalPages}
          </span>
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            disabled={page.number >= page.totalPages - 1}
            onClick={() => goToPage(page.number + 1)}
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}

export default MyTransactionsPage;
