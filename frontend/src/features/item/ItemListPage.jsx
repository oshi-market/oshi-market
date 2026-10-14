import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import { fetchItems, fetchWorks } from './api';
import ItemThumb from './ItemThumb';
import WorkCombobox from './WorkCombobox';
import { CATEGORY_LABEL, CONDITION_LABEL, SORT_OPTIONS, STATUS_LABEL } from './constants';

const EMPTY_PAGE = { content: [], number: 0, totalPages: 0 };

/** 판매 상태 필터의 "전체" 값. 주소창에 status가 없으면 판매중만 보여준다(기본값). */
const ALL_STATUS = 'ALL';
const DEFAULT_STATUS = 'SELLING';
const DEFAULT_SORT = 'latest';

/** 주소창 값이 목록에 없으면(직접 고친 주소 등) 무시하고 기본값으로 처리해 400을 피한다. */
function readFilters(searchParams) {
  const category = searchParams.get('category') ?? '';
  const status = searchParams.get('status') ?? DEFAULT_STATUS;
  const sort = searchParams.get('sort') ?? DEFAULT_SORT;
  return {
    keyword: searchParams.get('keyword') ?? '',
    work: searchParams.get('work') ?? '',
    category: category in CATEGORY_LABEL ? category : '',
    status: status === ALL_STATUS || status in STATUS_LABEL ? status : DEFAULT_STATUS,
    sort: sort in SORT_OPTIONS ? sort : DEFAULT_SORT,
  };
}

function ItemListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(EMPTY_PAGE);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const [works, setWorks] = useState([]);

  const { keyword, work, category, status, sort } = readFilters(searchParams);
  const pageNumber = Number(searchParams.get('page') ?? 0);
  const hasFilter = Boolean(work || category) || status !== DEFAULT_STATUS || sort !== DEFAULT_SORT;

  // 작품 입력칸은 드롭다운이라 제어 컴포넌트로 두고, 주소창 값이 바뀌면(뒤로 가기 등) 따라간다
  const [workInput, setWorkInput] = useState(work);
  const [prevWork, setPrevWork] = useState(work);
  if (work !== prevWork) {
    setPrevWork(work);
    setWorkInput(work);
  }

  useEffect(() => {
    fetchWorks()
      .then(({ data }) => setWorks(data))
      // 작품 목록을 못 불러와도 직접 입력으로 필터는 가능하니 조용히 넘어간다
      .catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError('');

    fetchItems({
      keyword: keyword || undefined,
      work: work || undefined,
      category: category || undefined,
      status: status === ALL_STATUS ? undefined : status,
      sort: SORT_OPTIONS[sort].sort,
      page: pageNumber,
      size: 20,
    })
      .then(({ data }) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getErrorMessage(err, '상품 목록을 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [keyword, work, category, status, sort, pageNumber]);

  function goToPage(next) {
    const params = new URLSearchParams(searchParams);
    params.set('page', String(next));
    setSearchParams(params);
  }

  /** 필터를 바꾸면 결과가 달라지니 1페이지로 돌아간다. 기본값과 같은 값은 주소창에서 지워 깔끔하게 유지. */
  function updateFilters(changes) {
    const params = new URLSearchParams(searchParams);
    Object.entries(changes).forEach(([key, value]) => {
      if (value) params.set(key, value);
      else params.delete(key);
    });
    if (params.get('status') === DEFAULT_STATUS) params.delete('status');
    if (params.get('sort') === DEFAULT_SORT) params.delete('sort');
    params.delete('page');
    setSearchParams(params);
  }

  function handleWorkSubmit(event) {
    event.preventDefault();
    updateFilters({ work: workInput.trim() });
  }

  /** 검색어는 상단 검색창에서 입력한 값이라 남겨두고, 필터·정렬만 초기화. */
  function resetFilters() {
    setWorkInput('');
    setSearchParams(keyword ? { keyword } : {});
  }

  return (
    <div className="page">
      <div className="page-header">
        <h2>{keyword ? `"${keyword}" 검색 결과` : '상품 둘러보기'}</h2>
        <Link to="/items/new" className="btn btn-primary btn-sm">
          상품 등록
        </Link>
      </div>

      <form className="item-filter" onSubmit={handleWorkSubmit}>
        <select
          className="input"
          aria-label="카테고리"
          value={category}
          onChange={(event) => updateFilters({ category: event.target.value })}
        >
          <option value="">전체 카테고리</option>
          {Object.entries(CATEGORY_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <WorkCombobox
          value={workInput}
          onChange={setWorkInput}
          options={works.map((w) => w.name)}
          placeholder="작품명 검색"
        />
        <button type="submit" className="btn btn-ghost btn-sm">
          작품 적용
        </button>
        <select
          className="input"
          aria-label="판매 상태"
          value={status}
          onChange={(event) => updateFilters({ status: event.target.value })}
        >
          <option value={ALL_STATUS}>전체 상태</option>
          {Object.entries(STATUS_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <select
          className="input"
          aria-label="정렬"
          value={sort}
          onChange={(event) => updateFilters({ sort: event.target.value })}
        >
          {Object.entries(SORT_OPTIONS).map(([value, option]) => (
            <option key={value} value={value}>
              {option.label}
            </option>
          ))}
        </select>
        {hasFilter && (
          <button type="button" className="btn btn-ghost btn-sm" onClick={resetFilters}>
            초기화
          </button>
        )}
      </form>

      {isLoading && <p className="muted">불러오는 중...</p>}
      {error && <p className="form-error">{error}</p>}
      {!isLoading && !error && page.content.length === 0 && (
        <p className="muted">조건에 맞는 상품이 없습니다.</p>
      )}

      <div className="item-grid">
        {page.content.map((item) => (
          <Link to={`/items/${item.id}`} key={item.id} className="item-card">
            <ItemThumb url={item.thumbnailUrl} alt={item.title} />
            <div className="item-card-body">
              <p className="item-card-title">{item.title}</p>
              <p className="item-card-price">{item.price.toLocaleString()}원</p>
              <div className="item-card-meta">
                <span className={`badge badge-status-${item.status.toLowerCase()}`}>
                  {STATUS_LABEL[item.status]}
                </span>
                <span className="badge badge-outline">{CONDITION_LABEL[item.condition]}</span>
              </div>
            </div>
          </Link>
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

export default ItemListPage;
