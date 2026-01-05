export interface User {
  id: string;
  email: string;
  name: string;
  username?: string; // Alias for backward compatibility
  onboardingComplete: boolean;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  name: string;
  firstLogin: boolean;
  onboardingComplete: boolean;
}

export interface Book {
  id: string;
  title: string;
  author: string;
  isbn?: string;
  description?: string;
  coverUrl?: string;
  genre: string;
  publishedYear?: number;
  publishedDate?: string;
  pages?: number;
  pageCount?: number;
  publisher?: string;
  language?: string;
  averageRating?: number;
  ratingsCount?: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type ShelfStatus = 'READING' | 'READ' | 'TO_READ';
export type ShelfType = ShelfStatus;

export interface ShelfItem {
  id: string;
  bookId: string;
  book: Book;
  shelfType: ShelfStatus;
  status: ShelfStatus; // Alias for shelfType for frontend compatibility
  startedAt?: string;
  finishedAt?: string;
  addedAt: string;
  updatedAt?: string;
}

export interface ShelvesResponse {
  reading: ShelfItem[];
  read: ShelfItem[];
  toRead: ShelfItem[];
}

export interface Rating {
  id: string;
  userId: string;
  bookId: string;
  score: number;
  liked?: boolean;
  createdAt: string;
}

export interface Review {
  id: string;
  userId: string;
  username?: string;
  bookId: string;
  content: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Recommendation {
  bookId: string;
  score: number;
  reason?: string;
}

export interface RecommendationResponse {
  userId: string;
  recommendations: Recommendation[];
  generatedAt: string;
}
