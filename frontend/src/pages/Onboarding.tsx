import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { BookOpen, Check, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { bookApi } from '../api/books';
import { ratingApi } from '../api/ratings';
import { shelfApi } from '../api/shelf';
import { userApi } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import { Book } from '../types';

export default function Onboarding() {
  const [selectedBooks, setSelectedBooks] = useState<Set<string>>(new Set());
  const navigate = useNavigate();
  const completeOnboarding = useAuthStore((state) => state.completeOnboarding);

  const { data: booksData, isLoading } = useQuery({
    queryKey: ['onboarding-books'],
    queryFn: () => bookApi.getTopRated(0, 20),
  });

  const submitMutation = useMutation({
    mutationFn: async (bookIds: string[]) => {
      // Rate all books with 5 stars using batch endpoint
      await ratingApi.submitInitialLikedBooks(bookIds);
      // Add all books to "READ" shelf (ignore errors for already added books)
      await Promise.all(
        bookIds.map(bookId => shelfApi.addToShelf(bookId, 'READ').catch(() => {}))
      );
      // Complete onboarding
      await userApi.completeOnboarding();
    },
    onSuccess: () => {
      completeOnboarding();
      toast.success("Great choices! Let's find you some recommendations.");
      navigate('/');
    },
    onError: (error: Error & { response?: { data?: { message?: string } } }) => {
      const message = error.response?.data?.message || 'Something went wrong. Please try again.';
      toast.error(message);
    },
  });

  const toggleBook = (bookId: string) => {
    setSelectedBooks((prev) => {
      const next = new Set(prev);
      if (next.has(bookId)) {
        next.delete(bookId);
      } else {
        next.add(bookId);
      }
      return next;
    });
  };

  const handleContinue = () => {
    if (selectedBooks.size < 3) {
      toast.error('Please select at least 3 books');
      return;
    }
    submitMutation.mutate(Array.from(selectedBooks));
  };

  const handleSkip = async () => {
    await userApi.completeOnboarding();
    completeOnboarding();
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-white">
      <div className="max-w-6xl mx-auto px-4 py-12">
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-2 mb-4">
            <BookOpen className="w-10 h-10 text-primary-600" />
            <span className="text-3xl font-serif font-bold text-gray-900">Shelve</span>
          </div>
          <h1 className="text-3xl font-serif font-bold text-gray-900 mb-4">
            Welcome! Let's personalize your experience
          </h1>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Select books you've read and enjoyed. This helps us recommend books you'll love.
            <br />
            <span className="text-sm text-gray-500">Select at least 3 books</span>
          </p>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4 mb-8">
              {booksData?.content.map((book: Book) => (
                <button
                  key={book.id}
                  onClick={() => toggleBook(book.id)}
                  className={`card relative overflow-hidden transition-all duration-200 ${
                    selectedBooks.has(book.id)
                      ? 'ring-2 ring-primary-500 shadow-lg'
                      : 'hover:shadow-md'
                  }`}
                >
                  {selectedBooks.has(book.id) && (
                    <div className="absolute top-2 right-2 z-10 bg-primary-500 text-white rounded-full p-1">
                      <Check className="w-4 h-4" />
                    </div>
                  )}
                  <div className="aspect-[2/3] bg-gray-100">
                    {book.coverUrl ? (
                      <img
                        src={book.coverUrl}
                        alt={book.title}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = 'https://via.placeholder.com/200x300?text=No+Cover';
                        }}
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-primary-100 to-primary-200">
                        <BookOpen className="w-8 h-8 text-primary-400" />
                      </div>
                    )}
                  </div>
                  <div className="p-3">
                    <h3 className="font-medium text-sm text-gray-900 line-clamp-2">
                      {book.title}
                    </h3>
                    <p className="text-xs text-gray-500 mt-1 line-clamp-1">{book.author}</p>
                  </div>
                </button>
              ))}
            </div>

            <div className="flex justify-center gap-4">
              <button
                onClick={handleSkip}
                className="btn-secondary px-8 py-3"
              >
                Skip for now
              </button>
              <button
                onClick={handleContinue}
                disabled={selectedBooks.size < 3 || submitMutation.isPending}
                className="btn-primary px-8 py-3 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitMutation.isPending ? (
                  'Setting up...'
                ) : (
                  <>
                    Continue ({selectedBooks.size} selected)
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
