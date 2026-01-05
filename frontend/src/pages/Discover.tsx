import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, Filter, SlidersHorizontal } from 'lucide-react';
import { bookApi } from '../api/books';
import { recommendationApi } from '../api/recommendations';
import BookCard from '../components/BookCard';
import { Book } from '../types';

const GENRES = [
  'All',
  'Fiction',
  'Non-Fiction',
  'Science Fiction',
  'Fantasy',
  'Mystery',
  'Thriller',
  'Romance',
  'Historical',
  'Biography',
  'Self-Help',
  'Science',
  'Philosophy',
];

type TabType = 'all' | 'recommended' | 'top-rated' | 'recent';

export default function Discover() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedGenre, setSelectedGenre] = useState('All');
  const [activeTab, setActiveTab] = useState<TabType>('all');
  const [page, setPage] = useState(0);

  const { data: searchResults, isLoading: loadingSearch } = useQuery({
    queryKey: ['books-search', searchQuery, selectedGenre, page],
    queryFn: () => {
      if (searchQuery) {
        return bookApi.search(searchQuery, page, 20);
      }
      if (selectedGenre !== 'All') {
        return bookApi.getByGenre(selectedGenre, page, 20);
      }
      return bookApi.getAll(page, 20);
    },
    enabled: activeTab === 'all',
  });

  const { data: recommendations, isLoading: loadingRecs } = useQuery({
    queryKey: ['recommendations-discover'],
    queryFn: () => recommendationApi.getRecommendations(20),
    enabled: activeTab === 'recommended',
  });

  const { data: topRated, isLoading: loadingTop } = useQuery({
    queryKey: ['top-rated-discover', page],
    queryFn: () => bookApi.getTopRated(page, 20),
    enabled: activeTab === 'top-rated',
  });

  const { data: recent, isLoading: loadingRecent } = useQuery({
    queryKey: ['recent-discover', page],
    queryFn: () => bookApi.getRecentlyAdded(page, 20),
    enabled: activeTab === 'recent',
  });

  const isLoading = loadingSearch || loadingRecs || loadingTop || loadingRecent;

  const getBooks = (): Book[] => {
    switch (activeTab) {
      case 'recommended':
        return recommendations || [];
      case 'top-rated':
        return topRated?.content || [];
      case 'recent':
        return recent?.content || [];
      default:
        return searchResults?.content || [];
    }
  };

  const getTotalPages = (): number => {
    switch (activeTab) {
      case 'recommended':
        return 1;
      case 'top-rated':
        return topRated?.totalPages || 1;
      case 'recent':
        return recent?.totalPages || 1;
      default:
        return searchResults?.totalPages || 1;
    }
  };

  const books = getBooks();
  const totalPages = getTotalPages();

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-serif font-bold text-gray-900 mb-2">
          Discover Books
        </h1>
        <p className="text-gray-600">
          Explore our collection and find your next favorite read
        </p>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-8">
          {[
            { id: 'all', label: 'All Books' },
            { id: 'recommended', label: 'For You' },
            { id: 'top-rated', label: 'Top Rated' },
            { id: 'recent', label: 'New Arrivals' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => {
                setActiveTab(tab.id as TabType);
                setPage(0);
              }}
              className={`pb-3 text-sm font-medium transition-colors relative ${
                activeTab === tab.id
                  ? 'text-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
              {activeTab === tab.id && (
                <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary-600"></span>
              )}
            </button>
          ))}
        </nav>
      </div>

      {/* Search and Filter - only for "All Books" tab */}
      {activeTab === 'all' && (
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search by title, author, or ISBN..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="input pl-10"
            />
          </div>
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <select
              value={selectedGenre}
              onChange={(e) => {
                setSelectedGenre(e.target.value);
                setPage(0);
              }}
              className="input pl-10 pr-8 appearance-none cursor-pointer"
            >
              {GENRES.map((genre) => (
                <option key={genre} value={genre}>
                  {genre}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      {/* Results */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      ) : books.length > 0 ? (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
            {books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn-secondary px-4 py-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <span className="flex items-center px-4 text-sm text-gray-600">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="btn-secondary px-4 py-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </>
      ) : (
        <div className="card p-12 text-center">
          <SlidersHorizontal className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600 mb-2">No books found</p>
          <p className="text-sm text-gray-500">
            {activeTab === 'recommended'
              ? 'Rate more books to get personalized recommendations'
              : 'Try adjusting your search or filters'}
          </p>
        </div>
      )}
    </div>
  );
}
