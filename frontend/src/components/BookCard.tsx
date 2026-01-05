import { Link } from 'react-router-dom';
import { Star, BookOpen } from 'lucide-react';
import { Book } from '../types';

interface BookCardProps {
  book: Book;
  reason?: string;
  onAddToShelf?: (bookId: string) => void;
}

export default function BookCard({ book, reason }: BookCardProps) {
  return (
    <Link
      to={`/book/${book.id}`}
      className="card group hover:shadow-md transition-shadow duration-200"
    >
      <div className="aspect-[2/3] bg-gray-100 relative overflow-hidden">
        {book.coverUrl ? (
          <img
            src={book.coverUrl}
            alt={book.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            onError={(e) => {
              (e.target as HTMLImageElement).src = 'https://via.placeholder.com/200x300?text=No+Cover';
            }}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-primary-100 to-primary-200">
            <BookOpen className="w-12 h-12 text-primary-400" />
          </div>
        )}
      </div>
      
      <div className="p-4">
        <h3 className="font-serif font-semibold text-gray-900 line-clamp-2 group-hover:text-primary-600 transition-colors">
          {book.title}
        </h3>
        <p className="text-sm text-gray-600 mt-1">{book.author}</p>
        
        <div className="flex items-center gap-2 mt-2">
          <div className="flex items-center gap-1">
            <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
            <span className="text-sm font-medium text-gray-700">
              {(book.averageRating ?? 0).toFixed(1)}
            </span>
          </div>
          <span className="text-xs text-gray-400">({book.ratingsCount ?? 0})</span>
        </div>
        
        {reason && (
          <p className="text-xs text-primary-600 mt-2 bg-primary-50 px-2 py-1 rounded-full inline-block">
            {reason}
          </p>
        )}
        
        <span className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded-full mt-2 inline-block">
          {book.genre}
        </span>
      </div>
    </Link>
  );
}
