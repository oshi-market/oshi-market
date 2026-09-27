import { BrowserRouter, Link, Route, Routes, useNavigate } from 'react-router-dom';
import './App.css';
import oshiHeroIcon from './assets/oshi-hero-icon.png';
import oshiLogo from './assets/oshi-nav-logo.png';
import { AuthProvider } from './features/member/AuthProvider';
import LoginPage from './features/member/LoginPage';
import SignupPage from './features/member/SignupPage';
import { useAuth } from './features/member/useAuth';

/**
 * 아직 화면이 없는 도메인(상품/큐레이션/채팅) 라우트의 임시 자리표시자.
 * 각 담당자가 features/*에 실제 화면을 만들면 이 라우트를 교체한다.
 */
function ComingSoon({ label }) {
  return (
    <div className="coming-soon">
      <p>{label} 화면은 아직 준비 중입니다 🚧</p>
    </div>
  );
}

function Nav() {
  const { isAuthenticated, member, isLoading, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <nav>
      <Link to="/" className="brand">
        <img src={oshiLogo} alt="오시마켓" className="brand-logo" />
      </Link>

      <div className="nav-menu">
        <Link to="/items">탐색</Link>
        <Link to="/curations">큐레이션</Link>
        <Link to="/chat">채팅</Link>
      </div>

      <div className="nav-actions">
        <Link to="/items/new" className="btn btn-primary btn-sm">
          판매하기
        </Link>
        {isLoading ? null : isAuthenticated ? (
          <>
            <span className="user-chip">{member?.nickname}님</span>
            <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
              로그아웃
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn btn-ghost btn-sm">
              로그인
            </Link>
            <Link to="/signup" className="btn btn-ghost btn-sm">
              회원가입
            </Link>
          </>
        )}
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
      <p className="tagline">
        서브컬쳐 굿즈 마니아를 위한 신뢰 기반 중고거래 + 구매처 큐레이션 플랫폼
      </p>
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
          <Route path="/items" element={<ComingSoon label="상품 목록" />} />
          <Route path="/items/new" element={<ComingSoon label="상품 등록" />} />
          <Route path="/curations" element={<ComingSoon label="큐레이션" />} />
          <Route path="/chat" element={<ComingSoon label="채팅" />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
