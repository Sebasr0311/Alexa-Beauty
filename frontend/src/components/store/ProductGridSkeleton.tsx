export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div
      role="status"
      aria-label="Cargando productos"
      className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-5"
    >
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="bg-white border border-border rounded-2xl overflow-hidden">
          <div className="aspect-square shimmer" />
          <div className="p-3 sm:p-4 space-y-2">
            <div className="h-2.5 w-1/3 rounded-full shimmer" />
            <div className="h-3.5 w-5/6 rounded-full shimmer" />
            <div className="h-3.5 w-2/3 rounded-full shimmer" />
            <div className="flex items-center justify-between pt-3">
              <div className="h-5 w-20 rounded-full shimmer" />
              <div className="w-11 h-11 rounded-full shimmer" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
