export interface IBook {
  id: number;
  title?: string | null;
  author?: string | null;
  publishedYear?: number | null;
  price?: number | null;
  available?: boolean | null;
}

export type NewBook = Omit<IBook, 'id'> & { id: null };
