/**
 * Production-ready API Base URL helper for TacitIQ frontend.
 * Prepends VITE_API_BASE_URL when deployed to Vercel or external host,
 * or falls back to relative paths when proxied locally by Vite / Nginx.
 */
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

export function getApiUrl(path: string): string {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  return API_BASE_URL ? `${API_BASE_URL}${cleanPath}` : cleanPath;
}
