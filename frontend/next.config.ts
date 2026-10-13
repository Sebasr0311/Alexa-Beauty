import type { NextConfig } from 'next';
const backend = process.env.API_BACKEND_URL || 'http://localhost:8080/api/v1';
if (process.env.NODE_ENV === 'production' && (!process.env.API_BACKEND_URL || !backend.startsWith('https://'))) {
  throw new Error('Production requires an HTTPS API_BACKEND_URL');
}
const nextConfig: NextConfig = {
  reactStrictMode: true,
  distDir: process.env.NEXT_BUILD_DIR || '.next',
  async rewrites() { return [{ source: '/api/backend/:path*', destination: `${backend}/:path*` }]; },
  async headers() { return [{ source: '/:path*', headers: [
    { key: 'X-Content-Type-Options', value: 'nosniff' },
    { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
    { key: 'X-Frame-Options', value: 'DENY' },
    { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
  ] }]; },
};
export default nextConfig;
