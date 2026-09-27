import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './useAuth';

/** 로그인 안 된 사용자가 접근하면 로그인 페이지로 보낸다. */
function RequireAuth({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}

export default RequireAuth;
