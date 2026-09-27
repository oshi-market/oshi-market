import { BrowserRouter, Link, Route, Routes, useNavigate } from 'react-router-dom';
import './App.css';
import { AuthProvider } from './features/member/AuthProvider';
import LoginPage from './features/member/LoginPage';
import SignupPage from './features/member/SignupPage';
import { useAuth } from './features/member/useAuth';

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
        오시마켓
      </Link>
      {isLoading ? null : (
        <div className="nav-links">
          {isAuthenticated ? (
            <>
              <span className="user-chip">{member?.nickname}님</span>
              <button type="button" className="btn btn-ghost" onClick={handleLogout}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost">
                로그인
              </Link>
              <Link to="/signup" className="btn btn-primary">
                회원가입
              </Link>
            </>
          )}
        </div>
      )}
    </nav>
  );
}

function Home() {
  const { isAuthenticated, member } = useAuth();

  return (
    <div className="home">
      <h1 className="brand-mark">오시마켓</h1>
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
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
