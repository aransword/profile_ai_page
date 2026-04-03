import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

export default function AiMessage({ content, isGenerating }) {
  return (
    <div className="flex justify-start">
      {/* AI 아이콘 */}
      <div className="w-8 h-8 rounded-full bg-blue-500 text-white shrink-0 flex items-center justify-center mr-4 mt-1">
        ✨
      </div>
      
      {/* 메시지 및 마크다운 영역 */}
      <div className="max-w-[85%] rounded-2xl px-5 py-3 text-base leading-relaxed bg-transparent text-gray-800">
        <div className="prose prose-slate max-w-none prose-p:leading-relaxed prose-pre:p-0 prose-pre:bg-transparent">
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            components={{
              code({ node, inline, className, children, ...props }) {
                const match = /language-(\w+)/.exec(className || '');
                return !inline && match ? (
                  <div className="rounded-lg overflow-hidden my-4 text-sm">
                    <SyntaxHighlighter
                      style={vscDarkPlus}
                      language={match[1]}
                      PreTag="div"
                      customStyle={{ margin: 0, padding: '1rem' }}
                      {...props}
                    >
                      {String(children).replace(/\n$/, '')}
                    </SyntaxHighlighter>
                  </div>
                ) : (
                  <code className="bg-gray-100 text-red-500 px-1.5 py-0.5 rounded text-sm font-mono" {...props}>
                    {children}
                  </code>
                );
              }
            }}
          >
            {content}
          </ReactMarkdown>

          {/* 타이핑 효과 커서 */}
          {isGenerating && (
            <span className="inline-block w-2 h-4 ml-1 bg-blue-400 animate-pulse align-middle"></span>
          )}
        </div>
      </div>
    </div>
  );
}