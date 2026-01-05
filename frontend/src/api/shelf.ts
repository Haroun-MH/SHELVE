import api from './client';
import { ShelfItem, ShelfStatus, ShelvesResponse } from '../types';

// Helper to normalize shelf items (add status alias for shelfType)
const normalizeShelfItem = (item: ShelfItem): ShelfItem => ({
  ...item,
  status: item.shelfType || item.status,
});

export const shelfApi = {
  getMyShelf: async (): Promise<ShelfItem[]> => {
    const response = await api.get<ShelvesResponse>('/shelves');
    const data = response.data;
    const allItems: ShelfItem[] = [];
    if (data.reading) allItems.push(...data.reading.map(normalizeShelfItem));
    if (data.read) allItems.push(...data.read.map(normalizeShelfItem));
    if (data.toRead) allItems.push(...data.toRead.map(normalizeShelfItem));
    return allItems;
  },

  getByStatus: async (status: ShelfStatus): Promise<ShelfItem[]> => {
    const response = await api.get<ShelfItem[]>(`/shelves/${status}`);
    return response.data.map(normalizeShelfItem);
  },

  getByBookId: async (bookId: string): Promise<ShelfItem | null> => {
    try {
      const response = await api.get<ShelfItem>(`/shelves/books/${bookId}/status`);
      return normalizeShelfItem(response.data);
    } catch {
      return null;
    }
  },

  addToShelf: async (bookId: string, status: ShelfStatus): Promise<ShelfItem> => {
    const response = await api.post<ShelfItem>(`/shelves/${status}/books/${bookId}`);
    return normalizeShelfItem(response.data);
  },

  updateStatus: async (bookId: string, status: ShelfStatus): Promise<ShelfItem> => {
    const response = await api.put<ShelfItem>(`/shelves/books/${bookId}`, { targetShelf: status });
    return normalizeShelfItem(response.data);
  },

  removeFromShelf: async (bookId: string): Promise<void> => {
    await api.delete(`/shelves/books/${bookId}`);
  },
};
