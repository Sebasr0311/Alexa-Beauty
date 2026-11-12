import type { Metadata, Viewport } from 'next';
import { Fraunces, Manrope } from 'next/font/google';
import './globals.css';
import { CartProvider } from '@/context/CartContext';

const display = Fraunces({
  subsets: ['latin'],
  style: ['normal', 'italic'],
  variable: '--font-display',
  display: 'swap',
});

const body = Manrope({
  subsets: ['latin'],
  variable: '--font-body',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'Alexa Beauty — Tu belleza, tu esencia',
  description:
    'Descubre cosméticos y maquillaje seleccionados para resaltar tu brillo natural. Asesoría personalizada y pedidos fáciles vía WhatsApp.',
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
  themeColor: '#711A38',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className={`scroll-smooth ${display.variable} ${body.variable}`}>
      <body className="bg-cream text-ink antialiased font-sans">
        <CartProvider>{children}</CartProvider>
      </body>
    </html>
  );
}
