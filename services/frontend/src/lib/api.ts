import axios from 'axios';

const NEWS_API = process.env.NEXT_PUBLIC_NEWS_API_URL || 'http://localhost:8083';
const ENGINE_API = process.env.NEXT_PUBLIC_ENGINE_API_URL || 'http://localhost:8080';

export const newsApi = axios.create({ baseURL: NEWS_API });
export const engineApi = axios.create({ baseURL: ENGINE_API });

// 인증 토큰 설정
export function setAuthToken(token: string) {
  engineApi.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  newsApi.defaults.headers.common['Authorization'] = `Bearer ${token}`;
}

// News API
export async function fetchHotNews() {
  const res = await newsApi.get('/api/news/hot');
  return res.data;
}

export async function fetchNewsByCategory(category?: string, page = 0) {
  const params: any = { page, size: 10, sort: 'publishedAt,desc' };
  if (category && category !== 'ALL') params.category = category;
  const res = await newsApi.get('/api/news', { params });
  return res.data;
}

export async function voteArticle(articleId: string, type: 'positive' | 'negative') {
  await newsApi.post(`/api/news/${articleId}/vote?type=${type}`);
}

export async function fetchComments(articleId: string, page = 0) {
  const res = await newsApi.get(`/api/forum/comments/${articleId}`, { params: { page, size: 20 } });
  return res.data;
}

export async function postComment(articleId: string, content: string, username: string) {
  const res = await newsApi.post('/api/forum/comments', {
    articleId, content, username, userId: crypto.randomUUID(),
  });
  return res.data;
}

// Auth API
export async function kakaoLogin(code: string) {
  const res = await engineApi.post('/api/auth/kakao', { code });
  return res.data;
}
