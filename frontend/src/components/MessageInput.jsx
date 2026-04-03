import { useState, useRef, useEffect } from 'react';

export default function MessageInput({ onSendMessage, isGenerating }) {
  const [text, setText] = useState('');
  const textareaRef = useRef(null);

  const handleSend = () => {
    if (text.trim() && !isGenerating) {
      onSendMessage(text);
      setText('');
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto'; // 전송 후 높이 초기화
      }
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // 내용 길이에 따라 textarea 높이 자동 조절
  const handleInput = (e) => {
    setText(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = `${e.target.scrollHeight}px`;
  };

  return (
    <div className="px-4 max-w-3xl mx-auto w-full">
      {/* 💡 입력창 래퍼: 연한 회색 배경, 포커스 시 약간의 그림자 */}
      <div className="relative flex items-end bg-gemini-input rounded-3xl border border-transparent focus-within:bg-white focus-within:border-gray-300 focus-within:shadow-sm transition-all duration-200">
        <textarea
          ref={textareaRef}
          value={text}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          placeholder={isGenerating ? "답변을 생성하는 중입니다..." : "여기에 프롬프트를 입력하세요"}
          disabled={isGenerating}
          className="w-full bg-transparent text-gray-800 placeholder-gray-500 rounded-3xl pl-6 pr-14 py-4 focus:outline-none resize-none max-h-48 overflow-y-auto"
          rows="1"
        />
        <button
          onClick={handleSend}
          disabled={!text.trim() || isGenerating}
          // 💡 버튼: 검은색 배경에 흰색 아이콘
          className="absolute right-3 bottom-3 p-2 bg-black text-white rounded-full disabled:bg-gray-200 disabled:text-gray-400 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-4 h-4">
            <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 10.5 12 3m0 0 7.5 7.5M12 3v18" />
          </svg>
        </button>
      </div>
      <p className="text-center text-xs text-gray-400 mt-3">
        AI가 생성한 정보는 부정확할 수 있습니다.
      </p>
    </div>
  );
}