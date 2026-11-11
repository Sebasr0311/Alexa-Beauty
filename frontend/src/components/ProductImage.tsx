'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { cn } from '@/lib/utils';

const OPTIMIZABLE_HOSTS = ['.supabase.co', '.supabase.in'];

/**
 * Only hosts whitelisted in next.config `images.remotePatterns` go through the
 * Next.js optimizer (resized + AVIF/WebP). Anything else is rendered as-is so an
 * unknown host never breaks the page.
 */
export function isOptimizableImage(src: string): boolean {
  try {
    const url = new URL(src);
    return url.protocol === 'https:' && OPTIMIZABLE_HOSTS.some((host) => url.hostname.endsWith(host));
  } catch {
    return false;
  }
}

interface ProductImageProps {
  src?: string | null;
  alt: string;
  sizes: string;
  priority?: boolean;
  className?: string;
  imgClassName?: string;
  fallback?: React.ReactNode;
  quality?: number;
}

/**
 * Responsive product image: shimmer placeholder while loading, fade-in on decode,
 * graceful fallback on error. Parent must be `position: relative` with a size.
 */
export function ProductImage({
  src,
  alt,
  sizes,
  priority = false,
  className,
  imgClassName,
  fallback = null,
  quality = 75,
}: ProductImageProps) {
  const [loaded, setLoaded] = useState(false);
  const [failedSrc, setFailedSrc] = useState<string | null>(null);

  if (!src || failedSrc === src) return <>{fallback}</>;

  return (
    <div className={cn('absolute inset-0', className)}>
      {!loaded && <div className="absolute inset-0 shimmer" aria-hidden="true" />}
      <Image
        key={src}
        src={src}
        alt={alt}
        fill
        sizes={sizes}
        quality={quality}
        priority={priority}
        fetchPriority={priority ? 'high' : 'auto'}
        unoptimized={!isOptimizableImage(src)}
        onLoad={() => setLoaded(true)}
        onError={() => setFailedSrc(src)}
        className={cn(
          'transition-[opacity,transform] duration-500 ease-out',
          loaded ? 'opacity-100' : 'opacity-0',
          imgClassName,
        )}
      />
    </div>
  );
}
