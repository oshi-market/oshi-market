import { useEffect, useRef, useState } from 'react';
import { BrowserRouter, Link, Route, Routes, useNavigate } from 'react-router-dom';
import './App.css';
import oshiHeroIcon from './assets/oshi-hero-icon.png';
import oshiLogo from './assets/oshi-nav-logo.png';
import HomeItemGrid from './features/item/HomeItemGrid';
import ItemDetailPage from './features/item/ItemDetailPage';
import ItemFormPage from './features/item/ItemFormPage';
import ItemListPage from './features/item/ItemListPage';
import MyItemsPage from './features/item/MyItemsPage';
import { AuthProvider } from './features/member/AuthProvider';
import LoginPage from './features/member/LoginPage';
import ProfilePage from './features/member/ProfilePage';
import RequireAuth from './features/member/RequireAuth';
import SignupPage from './features/member/SignupPage';
import { useAuth } from './features/member/useAuth';

/**
 * 아직 화면이 없는 도메인(상품/큐레이션/채팅/마이페이지) 라우트의 임시 자리표시자.
 * 각 담당자가 features/*에 실제 화면을 만들면 이 라우트를 교체한다.
 */
function ComingSoon({ label }) {
  return (
    <div className="coming-soon">
      <p>{label} 화면은 아직 준비 중입니다 🚧</p>
    </div>
  );
}

function SearchForm() {
  const [keyword, setKeyword] = useState('');
  const navigate = useNavigate();

  function handleSubmit(event) {
    event.preventDefault();
    const trimmed = keyword.trim();
    navigate(trimmed ? `/items?keyword=${encodeURIComponent(trimmed)}` : '/items');
  }

  return (
    <form className="nav-search" role="search" onSubmit={handleSubmit}>
      <input
        type="search"
        className="nav-search-input"
        placeholder="찾는 굿즈를 검색해보세요"
        value={keyword}
        onChange={(event) => setKeyword(event.target.value)}
      />
    </form>
  );
}

/** 로그인 상태면 마이페이지 드롭다운(내 거래/내 상품/프로필), 아니면 로그인 버튼 하나. */
function AccountMenu() {
  const { isAuthenticated, isLoading, member, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleLogout() {
    setOpen(false);
    logout();
    navigate('/');
  }

  if (isLoading) {
    return null;
  }

  if (!isAuthenticated) {
    return (
      <Link to="/login" className="btn btn-ghost btn-sm">
        로그인
      </Link>
    );
  }

  return (
    <div className="account-menu" ref={menuRef}>
      <button type="button" className="btn btn-ghost btn-sm" onClick={() => setOpen((prev) => !prev)}>
        마이페이지 ▾
      </button>
      {open && (
        <div className="account-menu-dropdown">
          <p className="account-menu-greeting">{member?.nickname}님</p>
          <Link to="/my/transactions" onClick={() => setOpen(false)}>
            내 거래
          </Link>
          <Link to="/my/items" onClick={() => setOpen(false)}>
            내 상품
          </Link>
          <Link to="/my/profile" onClick={() => setOpen(false)}>
            프로필
          </Link>
          <button type="button" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      )}
    </div>
  );
}

function Nav() {
  return (
    <nav>
      <div className="nav-discover">
        <Link to="/" className="brand">
          <img src={oshiLogo} alt="오시마켓" className="brand-logo" />
        </Link>
        <SearchForm />
      </div>

      <div className="nav-menu">
        <Link to="/curations">구매 가이드</Link>
        <Link to="/chat">채팅</Link>
      </div>

      <div className="nav-actions">
        <AccountMenu />
        <Link to="/items/new" className="btn btn-primary btn-sm">
          판매하기
        </Link>
      </div>
    </nav>
  );
}

function Home() {
  const { isAuthenticated, member } = useAuth();

  return (
    <div className="home">
      <div className="hero-spotlight">
        <img src={oshiHeroIcon} alt="" className="hero-icon" />
      </div>
      <h1 className="brand-mark">推しマーケット</h1>
      <p className="tagline">오시마켓에서, 믿고 사고파는 덕질 굿즈부터 살 곳 찾기까지 한번에</p>
      {!isAuthenticated && (
        <div className="cta-group">
          <Link to="/signup" className="btn btn-primary">
            회원가입하고 시작하기
          </Link>
          <Link to="/login" className="btn btn-ghost">
            로그인
          </Link>
        </div>
      )}
      {isAuthenticated && <p className="tagline">반갑습니다, {member?.nickname}님 👋</p>}
      <HomeItemGrid />
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Nav />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          {/* 각 도메인 라우트는 담당자가 features/* 에 추가 */}
          <Route path="/items" element={<ItemListPage />} />
          <Route
            path="/items/new"
            element={
              <RequireAuth>
                <ItemFormPage />
              </RequireAuth>
            }
          />
          <Route path="/items/:itemId" element={<ItemDetailPage />} />
          <Route
            path="/items/:itemId/edit"
            element={
              <RequireAuth>
                <ItemFormPage />
              </RequireAuth>
            }
          />
          <Route path="/curations" element={<ComingSoon label="구매 가이드" />} />
          <Route path="/chat" element={<ComingSoon label="채팅" />} />
          <Route
            path="/my/items"
            element={
              <RequireAuth>
                <MyItemsPage />
              </RequireAuth>
            }
          />
          <Route path="/my/transactions" element={<ComingSoon label="내 거래" />} />
          <Route
            path="/my/profile"
            element={
              <RequireAuth>
                <ProfilePage />
              </RequireAuth>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;