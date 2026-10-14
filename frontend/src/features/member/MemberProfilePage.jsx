import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { getErrorMessage } from '../../api/errors';
import ItemCard from '../item/ItemCard';
import { fetchItems } from '../item/api';
import { fetchMemberProfile } from './api';
import { useAuth } from './useAuth';

const EMPTY_PAGE = { content: [], number: 0, totalPages: 0, totalElements: 0 };

/** 판매 상품 탭. 주소창 ?tab=all이면 거래중·판매완료까지 전체, 없으면 판매중만. */
const TABS = {
  selling: { label: '판매중', status: 'SELLING' },
  all: { label: '전체', status: undefined },
};

function formatDate(isoString) {
  return isoString ? isoString.slice(0, 10) : '-';
}

/** 판매자 공개 프로필 (/members/:memberId). 상품 상세에서 판매자 닉네임을 누르면 들어온다. */
function MemberProfilePage() {
  const { memberId } = useParams();
  const { member } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const [profile, setProfile] = useState(null);
  const [profileError, setProfileError] = useState('');
  const [page, setPage] = useState(EMPTY_PAGE);
  const [isLoadingItems, setIsLoadingItems] = useState(true);
  const [itemsError, setItemsError] = useState('');

  const tab = searchParams.get('tab') === 'all' ? 'all' : 'selling';
  const pageNumber = Number(searchParams.get('page') ?? 0);
  const isMe = member?.id === Number(memberId);
  // 서버는 숫자 id만 공개 프로필로 받는다. 그 외 주소는 요청하지 않고 바로 안내 (비로그인이면 401이 와서 엉뚱한 문구가 나옴)
  const isValidId = /^\d+$/.test(memberId);

  useEffect(() => {
    if (!isValidId) return undefined;
    let cancelled = false;
    setProfileError('');
    fetchMemberProfile(memberId)
      .then(({ data }) => {
        if (!cancelled) setProfile(data);
      })
      .catch((err) => {
        if (!cancelled) setProfileError(getErrorMessage(err, '판매자 정보를 불러오지 못했습니다.'));
      });
    return () => {
      cancelled = true;
    };
  }, [memberId, isValidId]);

  useEffect(() => {
    if (!isValidId) return undefined;
    let cancelled = false;
    setIsLoadingItems(true);
    setItemsError('');

    fetchItems({ sellerId: memberId, status: TABS[tab].status, page: pageNumber, size: 20 })
      .then(({ data }) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setItemsError(getErrorMessage(err, '판매 상품을 불러오지 못했습니다.'));
      })
      .finally(() => {
        if (!cancelled) setIsLoadingItems(false);
      });

    return () => {
      cancelled = true;
    };
  }, [memberId, isValidId, tab, pageNumber]);

  function changeTab(next) {
    setSearchParams(next === 'all' ? { tab: 'all' } : {});
  }

  function goToPage(next) {
    const params = new URLSearchParams(searchParams);
    params.set('page', String(next));
    setSearchParams(params);
  }

  if (!isValidId || profileError) {
    return (
      <div className="page">
        <p className="form-error">{isValidId ? profileError : '회원을 찾을 수 없습니다.'}</p>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="member-profile-header">
        <div>
          <h2>{profile ? profile.nickname : '불러오는 중...'}</h2>
          {profile && <p className="muted">가입일 {formatDate(profile.createdAt)}</p>}
        </div>
        {isMe && (
          <Link to="/my/profile" className="btn btn-ghost btn-sm">
            내 프로필 수정
          </Link>
        )}
      </div>

      <div className="member-profile-tabs" role="tablist">
        {Object.entries(TABS).map(([key, { label }]) => (
          <button
            type="button"
            role="tab"
            key={key}
            aria-selected={tab === key}
            className={`btn btn-sm ${tab === key ? 'btn-primary' : 'btn-ghost'}`}
            onClick={() => changeTab(key)}
          >
            {label}
          </button>
        ))}
        {!isLoadingItems && !itemsError && <span className="muted">{page.totalElements}개</span>}
      </div>

      {isLoadingItems && <p className="muted">불러오는 중...</p>}
      {itemsError && <p className="form-error">{itemsError}</p>}
      {!isLoadingItems && !itemsError && page.content.length === 0 && (
        <p className="muted">{tab === 'all' ? '등록한 상품이 없습니다.' : '판매 중인 상품이 없습니다.'}</p>
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

export default MemberProfilePage;
