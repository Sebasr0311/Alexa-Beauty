'use client';

import React from 'react';
import { motion, MotionConfig } from 'motion/react';

interface RevealProps {
  children: React.ReactNode;
  className?: string;
  delay?: number;
  /** Vertical offset in px the content travels while fading in. */
  y?: number;
  as?: 'div' | 'section';
}

/**
 * Fades and lifts content into view once. Respects prefers-reduced-motion.
 * Lightweight alternative to React Bits' FadeContent (which depends on GSAP).
 */
export function Reveal({ children, className, delay = 0, y = 18, as = 'div' }: RevealProps) {
  const Component = as === 'section' ? motion.section : motion.div;
  return (
    <MotionConfig reducedMotion="user">
      <Component
        className={className}
        initial={{ opacity: 0, y }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '0px 0px -10% 0px' }}
        transition={{ duration: 0.6, delay, ease: [0.22, 1, 0.36, 1] }}
      >
        {children}
      </Component>
    </MotionConfig>
  );
}
