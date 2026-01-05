import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  BookOpen,
  ArrowLeft,
  Calendar,
  FileText,
  Tag,
  Plus,
  Star,
  MessageSquare,
  User,
  Loader2,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { bookApi } from '../api/books';
import { shelfApi } from '../api/shelf';
import { ratingApi, reviewApi } from '../api/ratings';
import StarRating from '../components/StarRating';
import { ShelfStatus, Review } from '../types';
import { useAuthStore } from '../store/authStore';

export default function BookDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  
  const [userRating, setUserRating] = useState(0);
  const [reviewText, setReviewText] = useState('');
  const [showReviewForm, setShowReviewForm] = useState(false);

  const { data: book, isLoading: loadingBook } = useQuery({
    queryKey: ['book', id],
    queryFn: () => bookApi.getById(id!),
    enabled: !!id,
  });

  const { data: reviews = [], isLoading: loadingReviews } = useQuery({
    queryKey: ['book-reviews', id],
    queryFn: () => reviewApi.getByBook(id!),
    enabled: !!id,
  });

  const { data: bookRating } = useQuery({
    queryKey: ['book-rating', id],
    queryFn: () => ratingApi.getAverageRating(id!),
    enabled: !!id,
  });

  const { data: myRating } = useQuery({
    queryKey: ['my-rating', id],
    queryFn: () => ratingApi.getUserRating(id!),
    enabled: !!id && !!user,
  });

  // Update local rating state when myRating data changes
  useEffect(() => {
    if (myRating) {
      setUserRating(myRating.score);
    }
  }, [myRating]);

  const { data: shelfItem } = useQuery({
    queryKey: ['shelf-item', id],
    queryFn: () => shelfApi.getByBookId(id!),
    enabled: !!id && !!user,
  });

  const addToShelfMutation = useMutation({
    mutationFn: ({ bookId, status }: { bookId: string; status: ShelfStatus }) =>
      shelfApi.addToShelf(bookId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shelf-item', id] });
      toast.success('Added to shelf!');
    },
    onError: () => {
      toast.error('Failed to add to shelf');
    },
  });

  const updateShelfMutation = useMutation({
    mutationFn: ({ bookId, status }: { bookId: string; status: ShelfStatus }) =>
      shelfApi.updateStatus(bookId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shelf-item', id] });
      toast.success('Shelf updated!');
    },
  });

  const rateMutation = useMutation({
    mutationFn: ({ bookId, rating }: { bookId: string; rating: number }) =>
      ratingApi.rateBook(bookId, rating),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['book-rating', id] });
      queryClient.invalidateQueries({ queryKey: ['my-rating', id] });
      toast.success('Rating saved!');
    },
    onError: () => {
      toast.error('Failed to save rating');
    },
  });

  const reviewMutation = useMutation({
    mutationFn: ({ bookId, content }: { bookId: string; content: string }) =>
      reviewApi.createReview(bookId, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['book-reviews', id] });
      setReviewText('');
      setShowReviewForm(false);
      toast.success('Review submitted!');
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || 'Failed to submit review';
      toast.error(message);
    },
  });

  const handleRating = (rating: number) => {
    setUserRating(rating);
    if (id) {
      rateMutation.mutate({ bookId: id, rating });
    }
  };

  const handleSubmitReview = (e: React.FormEvent) => {
    e.preventDefault();
    if (reviewText.trim().length < 10) {
      toast.error('Review must be at least 10 characters');
      return;
    }
    if (id && reviewText.trim()) {
      reviewMutation.mutate({ bookId: id, content: reviewText.trim() });
    }
  };

  if (loadingBook) {
    return (
      <div className="flex justify-center py-20">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!book) {
    return (
      <div className="text-center py-20">
        <BookOpen className="w-16 h-16 text-gray-300 mx-auto mb-4" />
        <h2 className="text-xl font-medium text-gray-900 mb-2">Book not found</h2>
        <button onClick={() => navigate(-1)} className="btn-primary">
          Go Back
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back</span>
      </button>

      {/* Book Details */}
      <div className="card p-6 md:p-8">
        <div className="flex flex-col md:flex-row gap-8">
          {/* Cover */}
          <div className="w-full md:w-64 flex-shrink-0">
            {book.coverUrl ? (
              <img
                src={book.coverUrl}
                alt={book.title}
                className="w-full rounded-lg shadow-lg"
                onError={(e) => {
                  (e.target as HTMLImageElement).src = 'https://via.placeholder.com/256x384?text=No+Cover';
                }}
              />
            ) : (
              <div className="w-full aspect-[2/3] rounded-lg bg-gradient-to-br from-primary-100 to-primary-200 flex items-center justify-center">
                <BookOpen className="w-16 h-16 text-primary-400" />
              </div>
            )}
          </div>

          {/* Info */}
          <div className="flex-1">
            <h1 className="text-3xl font-serif font-bold text-gray-900 mb-2">
              {book.title}
            </h1>
            <p className="text-xl text-gray-600 mb-4">by {book.author}</p>

            {/* Rating */}
            <div className="flex items-center gap-4 mb-6">
              <div className="flex items-center gap-2">
                <Star className="w-5 h-5 text-yellow-400 fill-current" />
                <span className="text-lg font-medium">
                  {typeof bookRating === 'number' ? bookRating.toFixed(1) : (typeof book.averageRating === 'number' ? book.averageRating.toFixed(1) : 'N/A')}
                </span>
              </div>
            </div>

            {/* Meta Info */}
            <div className="grid grid-cols-2 gap-4 mb-6">
              {(book.publishedYear || book.publishedDate) && (
                <div className="flex items-center gap-2 text-gray-600">
                  <Calendar className="w-4 h-4" />
                  <span>Published {book.publishedYear || (book.publishedDate ? new Date(book.publishedDate).getFullYear() : '')}</span>
                </div>
              )}
              {(book.pages || book.pageCount) && (
                <div className="flex items-center gap-2 text-gray-600">
                  <FileText className="w-4 h-4" />
                  <span>{book.pages || book.pageCount} pages</span>
                </div>
              )}
              {book.genre && (
                <div className="flex items-center gap-2 text-gray-600">
                  <Tag className="w-4 h-4" />
                  <span>{book.genre}</span>
                </div>
              )}
              {book.isbn && (
                <div className="flex items-center gap-2 text-gray-600">
                  <BookOpen className="w-4 h-4" />
                  <span>ISBN: {book.isbn}</span>
                </div>
              )}
            </div>

            {/* Description */}
            {book.description && (
              <div className="mb-6">
                <h3 className="font-medium text-gray-900 mb-2">Description</h3>
                <p className="text-gray-600 leading-relaxed">{book.description}</p>
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-wrap gap-4">
              {shelfItem ? (
                <select
                  value={shelfItem.status}
                  onChange={(e) =>
                    updateShelfMutation.mutate({
                      bookId: book.id,
                      status: e.target.value as ShelfStatus,
                    })
                  }
                  className="input"
                >
                  <option value="TO_READ">📚 To Read</option>
                  <option value="READING">📖 Currently Reading</option>
                  <option value="READ">✅ Read</option>
                </select>
              ) : (
                <div className="flex gap-2">
                  <button
                    onClick={() => addToShelfMutation.mutate({ bookId: book.id, status: 'TO_READ' })}
                    className="btn-primary flex items-center gap-2"
                    disabled={addToShelfMutation.isPending}
                  >
                    {addToShelfMutation.isPending ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <Plus className="w-4 h-4" />
                    )}
                    Add to Shelf
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* User Rating Section */}
      <div className="card p-6">
        <h3 className="font-medium text-gray-900 mb-4">Your Rating</h3>
        <div className="flex items-center gap-4">
          <StarRating
            rating={userRating}
            onRate={handleRating}
            size="lg"
          />
          {userRating > 0 && (
            <span className="text-gray-600">
              You rated this {userRating} star{userRating !== 1 ? 's' : ''}
            </span>
          )}
        </div>
      </div>

      {/* Reviews Section */}
      <div className="card p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="font-medium text-gray-900 flex items-center gap-2">
            <MessageSquare className="w-5 h-5" />
            Reviews ({reviews.length})
          </h3>
          {!showReviewForm && (
            <button
              onClick={() => setShowReviewForm(true)}
              className="btn-secondary"
            >
              Write a Review
            </button>
          )}
        </div>

        {/* Review Form */}
        {showReviewForm && (
          <form onSubmit={handleSubmitReview} className="mb-6 p-4 bg-gray-50 rounded-lg">
            <textarea
              value={reviewText}
              onChange={(e) => setReviewText(e.target.value)}
              placeholder="Share your thoughts about this book (minimum 10 characters)..."
              className="input min-h-[120px] mb-2"
              required
              minLength={10}
            />
            <div className="flex justify-between items-center mb-4">
              <span className={`text-sm ${reviewText.trim().length < 10 ? 'text-red-500' : 'text-gray-500'}`}>
                {reviewText.trim().length}/10 minimum characters
              </span>
            </div>
            <div className="flex gap-2 justify-end">
              <button
                type="button"
                onClick={() => {
                  setShowReviewForm(false);
                  setReviewText('');
                }}
                className="btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn-primary flex items-center gap-2"
                disabled={reviewMutation.isPending || reviewText.trim().length < 10}
              >
                {reviewMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : null}
                Submit Review
              </button>
            </div>
          </form>
        )}

        {/* Reviews List */}
        {loadingReviews ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : reviews.length > 0 ? (
          <div className="space-y-4">
            {reviews.map((review: Review) => (
              <div key={review.id} className="border-b border-gray-100 pb-4 last:border-0">
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
                    <User className="w-4 h-4 text-primary-600" />
                  </div>
                  <span className="font-medium text-gray-900">
                    {review.username || 'Anonymous'}
                  </span>
                  <span className="text-sm text-gray-500">
                    {new Date(review.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <p className="text-gray-600 pl-10">{review.content}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-center text-gray-500 py-8">
            No reviews yet. Be the first to share your thoughts!
          </p>
        )}
      </div>
    </div>
  );
}
