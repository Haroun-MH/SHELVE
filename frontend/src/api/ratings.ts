import api from './client';
import { Rating, Review } from '../types';

export const ratingApi = {
  rateBook: async (bookId: string, rating: number): Promise<Rating> => {
    const response = await api.post('/ratings', { bookId, score: rating });
    return response.data;
  },

  getUserRating: async (bookId: string): Promise<Rating | null> => {
    try {
      const response = await api.get(`/ratings/book/${bookId}`);
      return response.data;
    } catch {
      return null;
    }
  },

  getAverageRating: async (bookId: string): Promise<number> => {
    const response = await api.get(`/ratings/book/${bookId}/average`);
    return response.data;
  },

  submitInitialLikedBooks: async (bookIds: string[]): Promise<void> => {
    // Use batch endpoint to rate all initial books with 5 stars
    await api.post('/ratings/initial', { bookIds });
  },

  getUserRatings: async (): Promise<Rating[]> => {
    const response = await api.get('/ratings/user');
    return response.data;
  },
};

export const reviewApi = {
  getByBook: async (bookId: string): Promise<Review[]> => {
    const response = await api.get(`/reviews/book/${bookId}`);
    // Backend returns PagedResponse, extract content
    return response.data.content || response.data;
  },

  createReview: async (bookId: string, content: string): Promise<Review> => {
    const response = await api.post('/reviews', { bookId, content });
    return response.data;
  },

  updateReview: async (reviewId: string, content: string): Promise<Review> => {
    const response = await api.put(`/reviews/${reviewId}`, { content });
    return response.data;
  },

  deleteReview: async (reviewId: string): Promise<void> => {
    await api.delete(`/reviews/${reviewId}`);
  },
};
