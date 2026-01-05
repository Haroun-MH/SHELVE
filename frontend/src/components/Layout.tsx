import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { BookOpen, Compass, Library, User, LogOut } from 'lucide-react';
import { useAuthStore } from '../store/authStore';

export default function Layout() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <NavLink to="/" className="flex items-center gap-2">
              <BookOpen className="w-8 h-8 text-primary-600" />
              <span className="text-xl font-serif font-bold text-gray-900">Shelve</span>
            </NavLink>
            
            <nav className="hidden md:flex items-center gap-8">
              <NavLink 
                to="/" 
                className={({ isActive }) => 
                  `flex items-center gap-2 text-sm font-medium transition-colors ${
                    isActive ? 'text-primary-600' : 'text-gray-600 hover:text-gray-900'
                  }`
                }
              >
                <BookOpen className="w-4 h-4" />
                Home
              </NavLink>
              <NavLink 
                to="/discover" 
                className={({ isActive }) => 
                  `flex items-center gap-2 text-sm font-medium transition-colors ${
                    isActive ? 'text-primary-600' : 'text-gray-600 hover:text-gray-900'
                  }`
                }
              >
                <Compass className="w-4 h-4" />
                Discover
              </NavLink>
              <NavLink 
                to="/shelves" 
                className={({ isActive }) => 
                  `flex items-center gap-2 text-sm font-medium transition-colors ${
                    isActive ? 'text-primary-600' : 'text-gray-600 hover:text-gray-900'
                  }`
                }
              >
                <Library className="w-4 h-4" />
                My Shelves
              </NavLink>
            </nav>
            
            <div className="flex items-center gap-4">
              <NavLink 
                to="/profile"
                className="flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900"
              >
                <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center">
                  <User className="w-4 h-4 text-primary-600" />
                </div>
                <span className="hidden sm:inline">{user?.name}</span>
              </NavLink>
              <button
                onClick={handleLogout}
                className="p-2 text-gray-400 hover:text-gray-600 transition-colors"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Mobile Navigation */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 z-50">
        <div className="flex justify-around py-2">
          <NavLink 
            to="/" 
            className={({ isActive }) => 
              `flex flex-col items-center gap-1 px-4 py-2 ${
                isActive ? 'text-primary-600' : 'text-gray-500'
              }`
            }
          >
            <BookOpen className="w-5 h-5" />
            <span className="text-xs">Home</span>
          </NavLink>
          <NavLink 
            to="/discover" 
            className={({ isActive }) => 
              `flex flex-col items-center gap-1 px-4 py-2 ${
                isActive ? 'text-primary-600' : 'text-gray-500'
              }`
            }
          >
            <Compass className="w-5 h-5" />
            <span className="text-xs">Discover</span>
          </NavLink>
          <NavLink 
            to="/shelves" 
            className={({ isActive }) => 
              `flex flex-col items-center gap-1 px-4 py-2 ${
                isActive ? 'text-primary-600' : 'text-gray-500'
              }`
            }
          >
            <Library className="w-5 h-5" />
            <span className="text-xs">Shelves</span>
          </NavLink>
          <NavLink 
            to="/profile" 
            className={({ isActive }) => 
              `flex flex-col items-center gap-1 px-4 py-2 ${
                isActive ? 'text-primary-600' : 'text-gray-500'
              }`
            }
          >
            <User className="w-5 h-5" />
            <span className="text-xs">Profile</span>
          </NavLink>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 pb-24 md:pb-8">
        <Outlet />
      </main>
    </div>
  );
}
