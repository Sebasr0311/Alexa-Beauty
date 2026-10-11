import type { Metadata, Viewport } from 'next';
import './globals.css';
import { CartProvider } from '@/context/CartContext';

export const metadata: Metadata = {
  title: 'Alexa Beauty — Tu belleza, tu esencia',
  description:
    'Descubre cosméticos y maquillaje seleccionados para resaltar tu brillo natural. Asesoría personalizada y pedidos fáciles vía WhatsApp.',
  icons: {
    icon: '/branding/logo.png',
  },
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
    <html lang="es" className="scroll-smooth">
      <body className="bg-cream text-ink antialiased selection:bg-blush selection:text-wine font-sans">
        <CartProvider>{children}</CartProvider>
      </body>
    </html>
  );
}
