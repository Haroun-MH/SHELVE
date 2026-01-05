import api from './client';
import { Book, PagedResponse } from '../types';

export const bookApi = {
  // Alias methods for consistency
  getAll: async (page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get('/books', { params: { page, size } });
    return response.data;
  },

  getBooks: async (page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get('/books', { params: { page, size } });
    return response.data;
  },

  getBook: async (id: string): Promise<Book> => {
    const response = await api.get(`/books/${id}`);
    return response.data;
  },

  getById: async (id: string): Promise<Book> => {
    const response = await api.get(`/books/${id}`);
    return response.data;
  },

  search: async (query: string, page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get('/books/search', { params: { q: query, page, size } });
    return response.data;
  },

  searchBooks: async (query: string, page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get('/books/search', { params: { q: query, page, size } });
    return response.data;
  },

  getByGenre: async (genre: string, page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get(`/books/genre/${genre}`, { params: { page, size } });
    return response.data;
  },

  getBooksByGenre: async (genre: string, page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get(`/books/genre/${genre}`, { params: { page, size } });
    return response.data;
  },

  getGenres: async (): Promise<string[]> => {
    const response = await api.get('/books/genres');
    return response.data;
  },

  getTopRated: async (page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get('/books/top-rated', { params: { page, size } });
    return response.data;
  },

  getRecentlyAdded: async (page = 0, size = 20): Promise<PagedResponse<Book>> => {
    const response = await api.get('/books/recent', { params: { page, size } });
    return response.data;
  },

  getBooksByIds: async (ids: string[]): Promise<Book[]> => {
    const response = await api.get('/books/batch', { params: { ids: ids.join(',') } });
    return response.data;
  },
};
