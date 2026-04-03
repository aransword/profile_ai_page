import { useState } from 'react';

export default function UploadPage() {
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState('');

  const handleFileChange = (e) => {
    // 💡 여러 파일 중 첫 번째 파일만 선택
    setFile(e.target.files[0]);
  };

  const handleUpload = async () => {
    if (!file) {
      alert("업로드할 파일을 먼저 선택해주세요!");
      return;
    }

    // 💡 FormData 객체를 생성하여 파일을 담습니다.
    const formData = new FormData();
    
    // 🔥 중요: 여기서 'file'이라는 이름이 백엔드의 @RequestPart("file")과 정확히 일치해야 합니다!
    formData.append('file', file); 

    setStatus("업로드 진행 중...");

    try {
      const response = await fetch('http://localhost:9090/api/rag/upload', {
        method: 'POST',
        // 💡 fetch에 FormData를 넘길 때는 Content-Type을 직접 지정하지 마세요! 
        // 브라우저가 알아서 boundary가 포함된 'multipart/form-data'로 세팅해 줍니다.
        body: formData, 
      });

      if (response.ok) {
        setStatus("✅ 업로드 성공! 이제 AI가 이 문서를 참고할 수 있습니다.");
        setFile(null); // 성공 후 파일 초기화 (선택사항)
      } else {
        setStatus(`❌ 업로드 실패: ${response.status}`);
      }
    } catch (error) {
      console.error("업로드 에러:", error);
      setStatus("❌ 서버 연결 에러가 발생했습니다.");
    }
  };

  return (
    <div className="p-8 max-w-lg mx-auto mt-20 font-sans border rounded-xl shadow-sm bg-white">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">RAG 문서 업로드</h1>
      
      <div className="mb-6">
        <input 
          type="file" 
          onChange={handleFileChange} 
          className="block w-full text-sm text-gray-500
            file:mr-4 file:py-2 file:px-4
            file:rounded-full file:border-0
            file:text-sm file:font-semibold
            file:bg-blue-50 file:text-blue-700
            hover:file:bg-blue-100 cursor-pointer"
        />
      </div>

      <button
        onClick={handleUpload}
        disabled={!file}
        className="w-full bg-black text-white px-4 py-3 rounded-lg font-medium disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
      >
        문서 업로드
      </button>

      {status && (
        <div className="mt-6 p-4 rounded bg-gray-50 text-gray-700 text-sm font-medium">
          {status}
        </div>
      )}
    </div>
  );
}