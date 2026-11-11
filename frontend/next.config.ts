import type { NextConfig } from 'next';
const defaultProdBackend = 'https://alexa-beauty-96wl.onrender.com/api/v1';
const configuredBackend = process.env.API_BACKEND_URL || (process.env.NODE_ENV === 'production' ? defaultProdBackend : 'http://localhost:8080/api/v1');
const cleanBackend = configuredBackend.trim().replace(/^["']+|["']+$/g, '').replace(/\/+$/, '');
const backend = cleanBackend.endsWith('/api/v1') ? cleanBackend : `${cleanBackend}/api/v1`;
if (process.env.NODE_ENV === 'production' && !backend.startsWith('https://')) {
  throw new Error('Production requires an HTTPS API_BACKEND_URL');
}
const nextConfig: NextConfig = {
  reactStrictMode: true,
  distDir: process.env.NEXT_BUILD_DIR || '.next',
  poweredByHeader: false,
  compress: true,
  images: {
    formats: ['image/avif', 'image/webp'],
    deviceSizes: [360, 480, 640, 768, 1024, 1280, 1600],
    imageSizes: [48, 64, 96, 128, 192, 256, 320],
    minimumCacheTTL: 60 * 60 * 24 * 30,
    remotePatterns: [
      { protocol: 'https', hostname: '*.supabase.co', pathname: '/storage/v1/object/public/**' },
      { protocol: 'https', hostname: '*.supabase.in', pathname: '/storage/v1/object/public/**' },
    ],
  },
  experimental: {
    optimizePackageImports: ['lucide-react', 'motion'],
  },
  async rewrites() { return [{ source: '/api/backend/:path*', destination: `${backend}/:path*` }, { source: '/uploads/:path*', destination: `${backend.replace(/\/api\/v1\/?$/, '')}/uploads/:path*` }]; },
  async headers() { return [{ source: '/:path*', headers: [
    { key: 'X-Content-Type-Options', value: 'nosniff' },
    { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
    { key: 'X-Frame-Options', value: 'DENY' },
    { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
  ] }]; },
};
export default nextConfig;
