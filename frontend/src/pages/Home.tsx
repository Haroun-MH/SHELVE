import { useQuery } from '@tanstack/react-query';
import { BookOpen, Sparkles, TrendingUp, Clock } from 'lucide-react';
import { Link } from 'react-router-dom';
import { recommendationApi } from '../api/recommendations';
import { bookApi } from '../api/books';
import { shelfApi } from '../api/shelf';
import { useAuthStore } from '../store/authStore';
import BookCard from '../components/BookCard';
import { Book } from '../types';

export default function Home() {
  const user = useAuthStore((state) => state.user);

  const { data: recommendations, isLoading: loadingRecs } = useQuery({
    queryKey: ['recommendations'],
    queryFn: () => recommendationApi.getRecommendations(10),
  });

  const { data: topRated, isLoading: loadingTop } = useQuery({
    queryKey: ['top-rated'],
    queryFn: () => bookApi.getTopRated(0, 10),
  });

  const { data: recentBooks, isLoading: loadingRecent } = useQuery({
    queryKey: ['recent-books'],
    queryFn: () => bookApi.getRecentlyAdded(0, 10),
  });

  const { data: currentlyReading } = useQuery({
    queryKey: ['currently-reading'],
    queryFn: () => shelfApi.getByStatus('READING'),
  });

  return (
    <div className="space-y-12">
      {/* Hero Section */}
      <section className="bg-gradient-to-r from-primary-600 to-primary-800 rounded-2xl p-8 text-white">
        <div className="max-w-2xl">
          <h1 className="text-3xl font-serif font-bold mb-4">
            Welcome back, {user?.name}!
          </h1>
          <p className="text-primary-100 mb-6">
            Discover your next great read with personalized recommendations
            based on your reading preferences.
          </p>
          <Link to="/discover" className="btn bg-white text-primary-700 hover:bg-primary-50">
            Explore Books
          </Link>
        </div>
      </section>

      {/* Currently Reading */}
      {currentlyReading && currentlyReading.length > 0 && (
        <section>
          <div className="flex items-center gap-2 mb-6">
            <Clock className="w-5 h-5 text-primary-600" />
            <h2 className="text-xl font-serif font-bold text-gray-900">
              Continue Reading
            </h2>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
            {currentlyReading.slice(0, 5).map((item: { book: Book; id: string }) => (
              <BookCard key={item.id} book={item.book} />
            ))}
          </div>
        </section>
      )}

      {/* Personalized Recommendations */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-primary-600" />
            <h2 className="text-xl font-serif font-bold text-gray-900">
              Recommended for You
            </h2>
          </div>
          <Link
            to="/discover?tab=recommended"
            className="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            View All
          </Link>
        </div>
        {loadingRecs ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : recommendations && recommendations.length > 0 ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
            {recommendations.map((book: Book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        ) : (
          <div className="card p-8 text-center">
            <BookOpen className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-600 mb-4">
              Rate more books to get personalized recommendations
            </p>
            <Link to="/discover" className="btn-primary">
              Browse Books
            </Link>
          </div>
        )}
      </section>

      {/* Top Rated */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-primary-600" />
            <h2 className="text-xl font-serif font-bold text-gray-900">
              Top Rated
            </h2>
          </div>
          <Link
            to="/discover?tab=top-rated"
            className="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            View All
          </Link>
        </div>
        {loadingTop ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
            {topRated?.content.map((book: Book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        )}
      </section>

      {/* Recently Added */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-primary-600" />
            <h2 className="text-xl font-serif font-bold text-gray-900">
              Recently Added
            </h2>
          </div>
          <Link
            to="/discover?tab=recent"
            className="text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            View All
          </Link>
        </div>
        {loadingRecent ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
            {recentBooks?.content.map((book: Book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
