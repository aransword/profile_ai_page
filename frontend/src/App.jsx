import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ChatPage from './pages/ChatPage';       // 💡 깔끔하게 분리된 채팅 페이지
import UploadPage from './pages/UploadPage';   // 💡 문서 업로드 페이지

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 루트 경로(/) 접속 시 채팅 페이지 렌더링 */}
        <Route path="/" element={<ChatPage />} />
        
        {/* /upload 경로 접속 시 문서 업로드 페이지 렌더링 */}
        <Route path="/upload" element={<UploadPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;