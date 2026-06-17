'use client';
import { useState } from 'react';

interface Comment {
  id: string; username: string; content: string; createdAt: string;
}

export default function CommentSection({ articleId }: { articleId: string }) {
  const [comments, setComments] = useState<Comment[]>([
    { id: '1', username: '투자고수', content: '이번 결정은 예상된 수순이죠. 하반기가 관건입니다.', createdAt: '10분 전' },
    { id: '2', username: '주린이', content: '초보인데 이게 주식에 어떤 영향이 있나요?', createdAt: '8분 전' },
    { id: '3', username: '차트쟁이', content: '기술적으로 봤을 때 아직 관망이 맞는 것 같습니다.', createdAt: '5분 전' },
  ]);
  const [input, setInput] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    setComments(prev => [...prev, { id: Date.now().toString(), username: '나', content: input, createdAt: '방금' }]);
    setInput('');
  };

  return (
    <div className="pt-4">
      <h4 className="text-sm font-bold text-text-main mb-3">댓글 {comments.length}개</h4>

      {/* 입력 */}
      <form onSubmit={handleSubmit} className="flex gap-2 mb-4">
        <input
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="의견을 남겨보세요..."
          className="flex-1 px-3 py-2 text-sm border border-border rounded-md bg-white focus:outline-none focus:border-primary"
        />
        <button type="submit" className="px-4 py-2 text-sm font-medium text-white bg-primary rounded-md hover:bg-primary-dark">
          등록
        </button>
      </form>

      {/* 목록 */}
      <div className="space-y-2">
        {comments.map(c => (
          <div key={c.id} className="bg-white rounded-md p-3 border border-border">
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-medium text-primary">{c.username}</span>
              <span className="text-xs text-text-light">{c.createdAt}</span>
            </div>
            <p className="text-sm text-text-main">{c.content}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
