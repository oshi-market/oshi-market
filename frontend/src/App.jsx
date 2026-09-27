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
      <Link to="/">홈</Link>
      {isLoading ? null : isAuthenticated ? (
        <>
          <span>{member?.nickname}님</span>
          <button type="button" onClick={handleLogout}>
            로그아웃
          </button>
        </>
      ) : (
        <>
          <Link to="/login">로그인</Link>
          <Link to="/signup">회원가입</Link>
        </>
      )}
    </nav>
  );
}

function Home() {
  return <h1>오시마켓</h1>;
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
