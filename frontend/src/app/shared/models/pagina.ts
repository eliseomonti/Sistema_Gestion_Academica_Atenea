/**
 * Igual que el `PagedModel` de Spring: la forma de todos los listados paginados del backend.
 * `number` es la página actual, empezando en 0.
 */
export interface Pagina<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
