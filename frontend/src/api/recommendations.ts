import api from './client';
import { RecommendationResponse, Book } from '../types';
import { bookApi } from './books';

export const recommendationApi = {
  getRawRecommendations: async (limit = 10): Promise<RecommendationResponse> => {
    const response = await api.get('/recommendations', { params: { limit } });
    return response.data;
  },

  // Returns Book[] directly for use in components
  getRecommendations: async (limit = 10): Promise<Book[]> => {
    try {
      const recResponse = await recommendationApi.getRawRecommendations(limit);
      
      if (!recResponse.recommendations || recResponse.recommendations.length === 0) {
        return [];
      }

      const bookIds = recResponse.recommendations.map(r => r.bookId);
      const books = await bookApi.getBooksByIds(bookIds);
      
      // Return books in the same order as recommendations
      return recResponse.recommendations
        .map(rec => books.find(b => b.id === rec.bookId))
        .filter((book): book is Book => book !== undefined);
    } catch (error) {
      console.error('Failed to fetch recommendations:', error);
      return [];
    }
  },

  getRecommendedBooks: async (limit = 10): Promise<(Book & { reason?: string })[]> => {
    try {
      const recResponse = await recommendationApi.getRawRecommendations(limit);
      
      if (!recResponse.recommendations || recResponse.recommendations.length === 0) {
        return [];
      }

      const bookIds = recResponse.recommendations.map(r => r.bookId);
      const books = await bookApi.getBooksByIds(bookIds);
      
      // Merge book data with recommendation data
      const results: (Book & { reason?: string })[] = [];
      for (const rec of recResponse.recommendations) {
        const book = books.find(b => b.id === rec.bookId);
        if (book) {
          results.push({ ...book, reason: rec.reason });
        }
      }
      return results;
    } catch (error) {
      console.error('Failed to fetch recommendations:', error);
      return [];
    }
  },
};
