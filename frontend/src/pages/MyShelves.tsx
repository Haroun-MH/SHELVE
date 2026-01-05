import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { BookOpen, BookMarked, Check, Plus, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import { shelfApi } from '../api/shelf';
import { ShelfItem, ShelfStatus } from '../types';

const SHELF_TABS: { id: ShelfStatus | 'all'; label: string; icon: React.ReactNode }[] = [
  { id: 'all', label: 'All Books', icon: <BookOpen className="w-4 h-4" /> },
  { id: 'READING', label: 'Currently Reading', icon: <BookMarked className="w-4 h-4" /> },
  { id: 'READ', label: 'Read', icon: <Check className="w-4 h-4" /> },
  { id: 'TO_READ', label: 'To Read', icon: <Plus className="w-4 h-4" /> },
];

export default function MyShelves() {
  const [activeTab, setActiveTab] = useState<ShelfStatus | 'all'>('all');
  const queryClient = useQueryClient();

  const { data: allItems = [], isLoading } = useQuery({
    queryKey: ['my-shelves'],
    queryFn: shelfApi.getMyShelf,
  });

  const removeMutation = useMutation({
    mutationFn: (bookId: string) => shelfApi.removeFromShelf(bookId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-shelves'] });
      toast.success('Book removed from shelf');
    },
    onError: () => {
      toast.error('Failed to remove book');
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ bookId, status }: { bookId: string; status: ShelfStatus }) =>
      shelfApi.updateStatus(bookId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-shelves'] });
      toast.success('Shelf updated');
    },
    onError: () => {
      toast.error('Failed to update shelf');
    },
  });

  const filteredItems = activeTab === 'all' 
    ? allItems 
    : allItems.filter((item: ShelfItem) => item.status === activeTab);

  const getShelfCount = (status: ShelfStatus | 'all'): number => {
    if (status === 'all') return allItems.length;
    return allItems.filter((item: ShelfItem) => item.status === status).length;
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-serif font-bold text-gray-900 mb-2">
          My Shelves
        </h1>
        <p className="text-gray-600">
          Organize and track your reading journey
        </p>
      </div>

      {/* Shelf Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {SHELF_TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`card p-4 text-left transition-all ${
              activeTab === tab.id
                ? 'ring-2 ring-primary-500 bg-primary-50'
                : 'hover:shadow-md'
            }`}
          >
            <div className="flex items-center gap-2 text-gray-600 mb-2">
              {tab.icon}
              <span className="text-sm font-medium">{tab.label}</span>
            </div>
            <p className="text-2xl font-bold text-gray-900">
              {getShelfCount(tab.id)}
            </p>
          </button>
        ))}
      </div>

      {/* Books Grid */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      ) : filteredItems.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredItems.map((item: ShelfItem) => (
            <div key={item.id} className="card p-4">
              <div className="flex gap-4">
                <div className="w-24 flex-shrink-0">
                  {item.book.coverUrl ? (
                    <img
                      src={item.book.coverUrl}
                      alt={item.book.title}
                      className="w-full rounded-lg shadow-sm"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = 'https://via.placeholder.com/96x144?text=No+Cover';
                      }}
                    />
                  ) : (
                    <div className="w-full aspect-[2/3] rounded-lg bg-gradient-to-br from-primary-100 to-primary-200 flex items-center justify-center">
                      <BookOpen className="w-8 h-8 text-primary-400" />
                    </div>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-medium text-gray-900 line-clamp-2 mb-1">
                    {item.book.title}
                  </h3>
                  <p className="text-sm text-gray-500 mb-3">{item.book.author}</p>
                  
                  {/* Status Selector */}
                  <div className="mb-3">
                    <select
                      value={item.status}
                      onChange={(e) =>
                        updateStatusMutation.mutate({
                          bookId: item.bookId,
                          status: e.target.value as ShelfStatus,
                        })
                      }
                      className="input text-sm py-1.5"
                    >
                      <option value="TO_READ">To Read</option>
                      <option value="READING">Currently Reading</option>
                      <option value="READ">Read</option>
                    </select>
                  </div>

                  <div className="flex gap-2">
                    <a
                      href={`/book/${item.book.id}`}
                      className="btn-secondary text-sm py-1.5 px-3"
                    >
                      View Details
                    </a>
                    <button
                      onClick={() => removeMutation.mutate(item.bookId)}
                      className="btn text-sm py-1.5 px-3 text-red-600 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 text-xs text-gray-500">
                Added {new Date(item.addedAt).toLocaleDateString()}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="card p-12 text-center">
          <BookOpen className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600 mb-2">No books in this shelf</p>
          <p className="text-sm text-gray-500 mb-4">
            {activeTab === 'all'
              ? "Start building your library by adding books to your shelves"
              : `You haven't added any books to this shelf yet`}
          </p>
          <a href="/discover" className="btn-primary inline-block">
            Browse Books
          </a>
        </div>
      )}
    </div>
  );
}
