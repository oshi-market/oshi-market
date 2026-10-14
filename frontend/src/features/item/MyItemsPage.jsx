import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { useAuth } from '../member/useAuth';
import { fetchItems } from './api';
import ItemCard from './ItemCard';
import { STATUS_LABEL } from './constants';

const EMPTY_PAGE = { content: [], number: 0, totalPages: 0, totalElements: 0 };

/** 상태 탭. 주소창 ?status=가 없으면 전체 (내 상품은 거래 중·판매완료도 관리해야 해서 전체가 기본). */
const STATUS_TABS = [{ value: undefined, label: '전체' }].concat(
  Object.entries(STATUS_LABEL).map(([value, label]) => ({ value, label })),
);

function MyItemsPage() {
  const { member } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(EMPTY_PAGE);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const statusParam = searchParams.get('status');
  // 주소창을 직접 고쳐 없는 상태가 들어오면 전체로 처리 (서버 400 방지)
  const status = statusParam in STATUS_LABEL ? statusParam : undefined;
  const pageNumber = Number(searchParams.get('page') ?? 0);

  useEffect(() => {
    if (!member?.id) {
      return undefined;
    }
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchItems({ sellerId: member.id, status, page: pageNumber, size: 20 })
      .then(({ data }) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '내 상품을 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [member?.id, status, pageNumber]);

  /** 탭을 바꾸면 첫 페이지부터 다시 본다. */
  function selectStatus(next) {
    setSearchParams(next ? { status: next } : {});
  }

  function goToPage(next) {
    const params = new URLSearchParams(searchParams);
    params.set('page', String(next));
    setSearchParams(params);
  }

  return (
    <div className="page">
      <div className="page-header">
        <h2>내 상품</h2>
        <Link to="/items/new" className="btn btn-primary btn-sm">
          상품 등록
        </Link>
      </div>

      <div className="status-tabs" role="tablist">
        {STATUS_TABS.map((tab) => (
          <button
            type="button"
            role="tab"
            key={tab.label}
            aria-selected={status === tab.value}
            className={`btn btn-sm ${status === tab.value ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => selectStatus(tab.value)}
          >
            {tab.label}
          </button>
        ))}
        {!isLoading && !error && <span className="muted">{page.totalElements}개</span>}
      </div>

      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && page.content.length === 0 && (
        <p className="muted">{status ? `${STATUS_LABEL[status]} 상품이 없습니다.` : '등록한 상품이 없습니다.'}</p>
      )}

      <div className="item-grid">
        {page.content.map((item) => (
          <ItemCard key={item.id} item={item} />
        ))}
      </div>

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

export default MyItemsPage;
