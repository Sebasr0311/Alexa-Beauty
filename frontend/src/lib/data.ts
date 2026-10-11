export interface Product {
  id: number;
  brand: string;
  name: string;
  category: 'Rostro' | 'Labios' | 'Ojos' | 'Accesorios';
  price: number;
  stock: number;
  icon: string;
  visual: 'dark' | 'blush' | 'gold';
  badge: string;
  description?: string;
}

export const CATEGORIES = ['Todos', 'Rostro', 'Labios', 'Ojos', 'Accesorios'] as const;
export type Category = (typeof CATEGORIES)[number];

export const PRODUCTS: Product[] = [
  {
    id: 1,
    brand: 'BELLA COSMETICS',
    name: 'Labial mate duradero Nude 01',
    category: 'Labios',
    price: 18000,
    stock: 14,
    icon: '💄',
    visual: 'dark',
    badge: 'Más vendido',
    description: 'Fórmula hidratante con pigmentación de larga duración y acabado aterciopelado.',
  },
  {
    id: 2,
    brand: 'ROSA GLOW',
    name: 'Rubor compacto Rosa Suave',
    category: 'Rostro',
    price: 24000,
    stock: 8,
    icon: '🌸',
    visual: 'blush',
    badge: 'Favorito',
    description: 'Textura sedosa ultrafina que realza los pómulos con un rubor natural y saludable.',
  },
  {
    id: 3,
    brand: 'BELLA COSMETICS',
    name: 'Base líquida acabado natural',
    category: 'Rostro',
    price: 35000,
    stock: 6,
    icon: '🧴',
    visual: 'gold',
    badge: 'Nuevo',
    description: 'Cobertura media modulable con ácido hialurónico para un efecto segunda piel.',
  },
  {
    id: 4,
    brand: 'LUNA BEAUTY',
    name: 'Paleta de sombras Rosé',
    category: 'Ojos',
    price: 42000,
    stock: 5,
    icon: '🎨',
    visual: 'dark',
    badge: 'Colección',
    description: '9 tonos en acabados mate, satinado y glitter de alta adherencia.',
  },
  {
    id: 5,
    brand: 'LUNA BEAUTY',
    name: 'Gloss hidratante Cherry',
    category: 'Labios',
    price: 16000,
    stock: 13,
    icon: '💗',
    visual: 'blush',
    badge: 'Nuevo',
    description: 'Brillo espejo enriquecido con aceite de jojoba y aroma a frutos rojos.',
  },
  {
    id: 6,
    brand: 'ROSA GLOW',
    name: 'Brocha suave para difuminar',
    category: 'Accesorios',
    price: 12000,
    stock: 0,
    icon: '🖌️',
    visual: 'gold',
    badge: 'Agotado',
    description: 'Fibras sintéticas ultrasuaves para transiciones perfectas sin cortes.',
  },
  {
    id: 7,
    brand: 'BELLA COSMETICS',
    name: 'Polvo compacto beige medio',
    category: 'Rostro',
    price: 26000,
    stock: 4,
    icon: '✨',
    visual: 'blush',
    badge: 'Últimas unidades',
    description: 'Control de brillo prolongado sin resecar ni alterar el tono de tu piel.',
  },
  {
    id: 8,
    brand: 'LUNA BEAUTY',
    name: 'Máscara de pestañas volumen',
    category: 'Ojos',
    price: 29000,
    stock: 9,
    icon: '🪄',
    visual: 'dark',
    badge: 'Favorito',
    description: 'Cepillo de silicona curvado que eleva y separa cada pestaña al instante.',
  },
];
