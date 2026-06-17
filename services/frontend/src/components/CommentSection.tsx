'use client';
import { useState } from 'react';

interface Comment {
  id: string;
  username: string;
  content: string;
  createdAt: string;
}

export default function CommentSection({ articleId }: { articleId: string }) {
  const [comments, setComments] = useState<Comment[]>([
    { id: '1', username: '투자고수', content: '이번 금리 동결은 예상된 수순이죠. 하반기 인하에 베팅합니다.', createdAt: '10분 전' },
    { id: '2', username: '주린이', content: '초보인데 금리 인하되면 주식에 좋은 건가요?', createdAt: '8분 전' },
    { id: '3', username: '차트장인', content: '코스피 2900 돌파 시 매수 진입 예정입니다', createdAt: '5분 전' },
  ]);
  const [newComment, setNewComment] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newComment.trim()) return;

    setComments(prev => [...prev, {
      id: Date.now().toString(),
      username: '나',
      content: newComment,
      createdAt: '방금',
    }]);
    setNewComment('');
  };

  return (
    <div className="mt-6">
      <h3 className="text-lg font-bold mb-4">💬 토론 ({comments.length})</h3>

      {/* 댓글 입력 */}
      <form onSubmit={handleSubmit} className="mb-6">
        <div className="flex gap-2">
          <input
            type="text"
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="의견을 남겨보세요..."
            className="flex-1 bg-dark-secondary border border-dark-border rounded-xl px-4 py-3 text-sm text-text-primary placeholder-text-secondary focus:outline-none focus:border-accent"
          />
          <button
            type="submit"
            className="px-5 py-3 bg-accent text-white text-sm font-medium rounded-xl hover:bg-accent/90 transition-colors"
          >
            등록
          </button>
        </div>
      </form>

      {/* 댓글 목록 */}
      <div className="space-y-3">
        {comments.map(comment => (
          <div key={comment.id} className="bg-dark-secondary rounded-xl p-4 border border-dark-border">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-accent">{comment.username}</span>
              <span className="text-xs text-text-secondary">{comment.createdAt}</span>
            </div>
            <p className="text-sm text-text-primary">{comment.content}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
