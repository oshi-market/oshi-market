import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';

function Home() {
  return <h1>오시마켓</h1>;
}

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/">홈</Link>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        {/* 각 도메인 라우트는 담당자가 features/* 에 추가 */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
