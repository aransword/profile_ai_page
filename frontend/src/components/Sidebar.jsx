export default function Sidebar() {
  return (
    <div className="w-64 bg-gemini-sidebar h-full flex-col hidden md:flex p-4 border-r border-gray-800">
      <button className="flex items-center gap-2 bg-[#1a1a1c] hover:bg-gray-800 p-3 rounded-full w-max text-sm font-medium transition-colors">
        <span className="text-xl">+</span> 새 채팅
      </button>
      
      <div className="mt-8">
        <p className="text-xs text-gray-400 font-semibold mb-3 px-2">최근</p>
        <div className="flex flex-col gap-1">
          <button className="text-left truncate text-sm p-2 rounded-lg hover:bg-gray-800 text-gray-300">
            WebFlux 웹소켓 연동 테스트
          </button>
        </div>
      </div>
    </div>
  );
}