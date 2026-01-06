# Shelve - Book Recommendation Platform

A comprehensive microservices-based book recommendation platform built with Spring Boot, React, and Python. Shelve allows users to discover books, manage personal bookshelves, rate and review books, and receive personalized recommendations powered by collaborative filtering.

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue)
![Backend](https://img.shields.io/badge/Backend-Spring%20Boot%203.2-green)
![Frontend](https://img.shields.io/badge/Frontend-React%2018-61dafb)
![ML](https://img.shields.io/badge/ML-Python%20FastAPI-yellow)

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Services](#services)
  - [Config Server](#config-server)
  - [Discovery Server](#discovery-server)
  - [API Gateway](#api-gateway)
  - [Auth Service](#auth-service)
  - [Book Catalog Service](#book-catalog-service)
  - [Shelf Service](#shelf-service)
  - [Review Rating Service](#review-rating-service)
  - [Recommendation Service](#recommendation-service)
  - [Frontend](#frontend)
- [Database Schema](#database-schema)
- [Message Queues](#message-queues)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Frontend (React)                                │
│                              localhost:3000                                  │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API Gateway (Spring)                               │
│                              localhost:8080                                  │
│              • JWT Authentication • Rate Limiting • CORS                     │
└───────┬──────────┬──────────┬──────────┬──────────┬────────────────────────┘
        │          │          │          │          │
        ▼          ▼          ▼          ▼          ▼
┌───────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌─────────────┐
│   Auth    │ │  Book  │ │ Shelf  │ │ Review │ │Recommendation│
│  Service  │ │Catalog │ │Service │ │ Rating │ │   Service   │
│   :8081   │ │ :8082  │ │ :8083  │ │ :8084  │ │    :8085    │
└─────┬─────┘ └───┬────┘ └───┬────┘ └───┬────┘ └──────┬──────┘
      │           │          │          │             │
      ▼           ▼          ▼          ▼             ▼
┌───────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌─────────────┐
│PostgreSQL │ │Postgres│ │Postgres│ │Postgres│ │  PostgreSQL │
│  :5432    │ │ :5433  │ │ :5434  │ │ :5435  │ │    :5436    │
└───────────┘ └────────┘ └────────┘ └────────┘ └─────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                     RabbitMQ Message Broker :5672                            │
│           • Rating Events • Shelf Events • Async Communication               │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│  Config Server :8888          │        Discovery Server (Eureka) :8761       │
│  Centralized Configuration    │        Service Registry & Discovery          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

### Backend
- **Java 17** - Primary backend language
- **Spring Boot 3.2.1** - Microservices framework
- **Spring Cloud 2023.0.0** - Cloud-native patterns
- **Spring Security + JWT** - Authentication & authorization
- **Spring Data JPA** - Database ORM
- **PostgreSQL** - Relational database
- **RabbitMQ** - Message broker

### Machine Learning
- **Python 3.11** - ML service language
- **FastAPI** - High-performance Python web framework
- **scikit-learn** - Collaborative filtering algorithms
- **NumPy/Pandas** - Data processing

### Frontend
- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool
- **TailwindCSS** - Styling
- **React Query** - Data fetching & caching
- **Zustand** - State management
- **React Router** - Navigation

### Infrastructure
- **Docker & Docker Compose** - Containerization
- **Netflix Eureka** - Service discovery
- **Spring Cloud Config** - Centralized configuration
- **Spring Cloud Gateway** - API gateway

---

## Services

### Config Server
**Port: 8888**

Centralized configuration management for all microservices.

#### Directory Structure
```
config-server/
├── src/main/java/com/shelve/config/
│   └── ConfigServerApplication.java     # Main application entry point
├── src/main/resources/
│   ├── application.yml                  # Server configuration
│   └── config/                          # Service-specific configs
│       ├── application.yml              # Shared configuration
│       └── {service-name}.yml           # Per-service overrides
├── pom.xml                              # Maven dependencies
└── Dockerfile                           # Container configuration
```

#### Key Features
- Centralized configuration storage
- Environment-specific profiles (dev, docker, prod)
- Dynamic configuration refresh
- Encrypted sensitive properties

---

### Discovery Server
**Port: 8761**

Netflix Eureka service registry for dynamic service discovery.

#### Directory Structure
```
discovery-server/
├── src/main/java/com/shelve/discovery/
│   └── DiscoveryServerApplication.java  # Main application with @EnableEurekaServer
├── src/main/resources/
│   └── application.yml                  # Eureka server configuration
├── pom.xml                              # Maven dependencies
└── Dockerfile                           # Container configuration
```

#### Key Features
- Service registration and deregistration
- Health monitoring
- Load balancing support
- Self-preservation mode
- Dashboard UI at `/eureka`

---

### API Gateway
**Port: 8080**

Spring Cloud Gateway providing unified API entry point.

#### Directory Structure
```
api-gateway/
├── src/main/java/com/shelve/gateway/
│   ├── ApiGatewayApplication.java       # Main application entry
│   ├── config/
│   │   └── GatewayConfig.java           # Route definitions & configuration
│   ├── filter/
│   │   └── AuthenticationFilter.java    # JWT validation filter
│   └── util/
│       └── JwtUtil.java                 # JWT token utilities
├── src/main/resources/
│   └── application.yml                  # Gateway configuration
├── pom.xml                              # Maven dependencies
└── Dockerfile                           # Container configuration
```

#### Files Explained

| File | Purpose |
|------|---------|
| `ApiGatewayApplication.java` | Spring Boot main class, enables gateway functionality |
| `GatewayConfig.java` | Defines all routes: auth→8081, books→8082, shelves→8083, reviews→8084, recommendations→8085 |
| `AuthenticationFilter.java` | Intercepts requests, validates JWT tokens, injects X-User-Id header |
| `JwtUtil.java` | JWT parsing, validation, and claims extraction |

#### Route Configuration
```java
/api/auth/**        → auth-service (public)
/api/users/**       → auth-service (authenticated)
/api/books/**       → book-catalog-service (public)
/api/shelves/**     → shelf-service (authenticated)
/api/reviews/**     → review-rating-service (authenticated)
/api/ratings/**     → review-rating-service (authenticated)
/api/recommendations/** → recommendation-service (authenticated)
```

---

### Auth Service
**Port: 8081 | Database: 5432**

User authentication, authorization, and profile management.

#### Directory Structure
```
auth-service/
├── src/main/java/com/shelve/auth/
│   ├── AuthServiceApplication.java      # Main application entry
│   ├── config/
│   │   └── SecurityConfig.java          # Spring Security configuration
│   ├── controller/
│   │   ├── AuthController.java          # Login/Register endpoints
│   │   └── UserController.java          # Profile management endpoints
│   ├── dto/
│   │   ├── AuthResponse.java            # Login/Register response
│   │   ├── ChangePasswordRequest.java   # Password change request
│   │   ├── LoginRequest.java            # Login credentials
│   │   ├── RegisterRequest.java         # Registration data
│   │   ├── UpdateProfileRequest.java    # Profile update data
│   │   ├── UserInfoResponse.java        # Basic user info (for other services)
│   │   └── UserProfileResponse.java     # Full profile response
│   ├── entity/
│   │   └── User.java                    # User JPA entity
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # Centralized error handling
│   │   ├── UserAlreadyExistsException.java
│   │   └── UserNotFoundException.java
│   ├── repository/
│   │   └── UserRepository.java          # User data access layer
│   └── service/
│       ├── AuthService.java             # Authentication business logic
│       └── JwtService.java              # JWT token generation/validation
├── src/main/resources/
│   ├── application.yml                  # Service configuration
│   └── application-docker.yml           # Docker-specific config
├── pom.xml
└── Dockerfile
```

#### Files Explained

| File | Purpose |
|------|---------|
| `AuthController.java` | Handles POST `/api/auth/register`, `/api/auth/login` |
| `UserController.java` | Handles GET/PUT `/api/users/profile`, `/api/users/{id}/info`, batch info |
| `AuthService.java` | Registration, login validation, password hashing, JWT generation |
| `JwtService.java` | Creates JWT tokens with user claims, validates tokens, extracts userId |
| `User.java` | Entity with id, email, password, name, avatarUrl, bio, onboardingComplete |
| `SecurityConfig.java` | Configures BCrypt password encoder, CORS settings |

#### API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Create new account |
| POST | `/api/auth/login` | Authenticate user |
| GET | `/api/users/profile` | Get current user profile |
| PUT | `/api/users/profile` | Update profile |
| PUT | `/api/users/password` | Change password |
| POST | `/api/users/onboarding/complete` | Mark onboarding done |
| GET | `/api/users/{id}/info` | Get user info (internal) |
| POST | `/api/users/batch/info` | Batch get user info |

---

### Book Catalog Service
**Port: 8082 | Database: 5433**

Book management with external API integration (Google Books, Open Library).

#### Directory Structure
```
book-catalog-service/
├── src/main/java/com/shelve/bookcatalog/
│   ├── BookCatalogServiceApplication.java  # Main application entry
│   ├── config/
│   │   └── DataInitializer.java            # Seeds initial book data
│   ├── controller/
│   │   ├── BookController.java             # Book CRUD endpoints
│   │   └── BookImportController.java       # External API import endpoints
│   ├── dto/
│   │   ├── BookResponse.java               # Book data transfer object
│   │   ├── CreateBookRequest.java          # Book creation request
│   │   └── PagedResponse.java              # Paginated response wrapper
│   ├── entity/
│   │   └── Book.java                       # Book JPA entity
│   ├── exception/
│   │   ├── BookNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   ├── repository/
│   │   └── BookRepository.java             # Book data access with custom queries
│   └── service/
│       ├── BookService.java                # Book business logic
│       └── ExternalBookService.java        # Google Books & Open Library integration
├── src/main/resources/
│   ├── application.yml
│   └── application-docker.yml
├── pom.xml
└── Dockerfile
```

#### Files Explained

| File | Purpose |
|------|---------|
| `BookController.java` | REST endpoints for book CRUD, search, filter by genre |
| `BookImportController.java` | Endpoints to search/import from Google Books & Open Library |
| `BookService.java` | Search logic that combines local DB with external APIs |
| `ExternalBookService.java` | Calls Google Books API and Open Library API, parses responses |
| `Book.java` | Entity: id, title, author, isbn, description, coverUrl, genre, publishedDate, pageCount, publisher, language, averageRating, ratingsCount |
| `DataInitializer.java` | Runs on startup to populate database with sample books |
| `BookRepository.java` | JPA repository with custom search queries, genre filtering |

#### API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/books` | List all books (paginated) |
| GET | `/api/books/{id}` | Get book by ID |
| GET | `/api/books/batch` | Get multiple books by IDs |
| GET | `/api/books/search?q=` | Search books (local + external) |
| GET | `/api/books/genre/{genre}` | Filter by genre |
| GET | `/api/books/genres` | List all genres |
| GET | `/api/books/top-rated` | Top rated books |
| GET | `/api/books/recent` | Recently added books |
| POST | `/api/books` | Create new book |
| GET | `/api/books/import/search` | Search external APIs |

#### External API Integration
- **Google Books API**: Searches by query, fetches title, author, ISBN, cover, description
- **Open Library API**: Alternative source, different cover images, additional metadata
- Books from external APIs are automatically saved to local database

---

### Shelf Service
**Port: 8083 | Database: 5434**

Personal bookshelf management with RabbitMQ event publishing.

#### Directory Structure
```
shelf-service/
├── src/main/java/com/shelve/shelf/
│   ├── ShelfServiceApplication.java     # Main application entry
│   ├── client/
│   │   └── BookCatalogClient.java       # Calls book-catalog-service
│   ├── config/
│   │   └── RabbitMQConfig.java          # RabbitMQ exchange/queue setup
│   ├── controller/
│   │   └── ShelfController.java         # Shelf management endpoints
│   ├── dto/
│   │   ├── AddToShelfRequest.java       # Add book request
│   │   ├── PagedResponse.java           # Paginated response
│   │   ├── ShelfEvent.java              # RabbitMQ event payload
│   │   ├── ShelfItemResponse.java       # Shelf item with book details
│   │   └── UpdateShelfRequest.java      # Move book between shelves
│   ├── entity/
│   │   └── ShelfItem.java               # Shelf item JPA entity
│   ├── event/
│   │   └── ShelfEventPublisher.java     # Publishes events to RabbitMQ
│   ├── exception/
│   │   ├── BookAlreadyOnShelfException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ShelfItemNotFoundException.java
│   ├── repository/
│   │   └── ShelfItemRepository.java     # Shelf data access
│   └── service/
│       └── ShelfService.java            # Shelf business logic
├── src/main/resources/
│   ├── application.yml
│   └── application-docker.yml
├── pom.xml
└── Dockerfile
```

#### Files Explained

| File | Purpose |
|------|---------|
| `ShelfController.java` | REST endpoints for shelf operations |
| `ShelfService.java` | Add/remove/move books, validates book exists via catalog service |
| `ShelfEventPublisher.java` | Publishes shelf events to RabbitMQ when books added to READ shelf |
| `ShelfEvent.java` | DTO with userId, bookId, shelfType, eventType, timestamp |
| `RabbitMQConfig.java` | Defines shelf.exchange, shelf.queue, bindings |
| `BookCatalogClient.java` | WebClient to verify books exist in catalog |
| `ShelfItem.java` | Entity: id, userId, bookId, shelfType (READING/READ/TO_READ), addedAt |

#### Shelf Types
- **TO_READ** - Want to read list
- **READING** - Currently reading
- **READ** - Finished books

#### API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/shelves` | Get all user's shelf items |
| GET | `/api/shelves/{type}` | Get items by shelf type |
| GET | `/api/shelves/book/{bookId}` | Check if book is on any shelf |
| POST | `/api/shelves` | Add book to shelf |
| PUT | `/api/shelves/{bookId}` | Move book between shelves |
| DELETE | `/api/shelves/{bookId}` | Remove from shelf |

#### Event Publishing
When a book is added to or moved to the READ shelf, an event is published:
```json
{
  "userId": "uuid",
  "bookId": "uuid",
  "shelfType": "READ",
  "eventType": "ADDED",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

### Review Rating Service
**Port: 8084 | Database: 5435**

Book reviews and ratings with event publishing.

#### Directory Structure
```
review-rating-service/
├── src/main/java/com/shelve/review/
│   ├── ReviewRatingServiceApplication.java  # Main application entry
│   ├── client/
│   │   ├── AuthServiceClient.java           # Fetches usernames from auth service
│   │   └── UserInfo.java                    # User info DTO
│   ├── config/
│   │   └── RabbitMQConfig.java              # RabbitMQ configuration
│   ├── controller/
│   │   ├── RatingController.java            # Rating endpoints
│   │   └── ReviewController.java            # Review endpoints
│   ├── dto/
│   │   ├── CreateRatingRequest.java         # Rating creation request
│   │   ├── CreateReviewRequest.java         # Review creation request
│   │   ├── PagedResponse.java               # Paginated response
│   │   ├── RatingEvent.java                 # RabbitMQ rating event
│   │   ├── RatingResponse.java              # Rating response DTO
│   │   └── ReviewResponse.java              # Review response with username
│   ├── entity/
│   │   ├── Rating.java                      # Rating JPA entity
│   │   └── Review.java                      # Review JPA entity
│   ├── event/
│   │   └── RatingEventPublisher.java        # Publishes rating events
│   ├── exception/
│   │   ├── DuplicateRatingException.java
│   │   ├── DuplicateReviewException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── RatingNotFoundException.java
│   │   └── ReviewNotFoundException.java
│   ├── repository/
│   │   ├── RatingRepository.java            # Rating data access
│   │   └── ReviewRepository.java            # Review data access
│   └── service/
│       ├── RatingService.java               # Rating business logic
│       └── ReviewService.java               # Review logic with username fetch
├── src/main/resources/
│   ├── application.yml
│   └── application-docker.yml
├── pom.xml
└── Dockerfile
```

#### Files Explained

| File | Purpose |
|------|---------|
| `RatingController.java` | REST endpoints for rating CRUD operations |
| `ReviewController.java` | REST endpoints for review CRUD operations |
| `RatingService.java` | Rate books, update ratings, calculate averages |
| `ReviewService.java` | Create/update/delete reviews, fetches usernames from auth service |
| `AuthServiceClient.java` | WebClient that calls auth-service for user info |
| `RatingEventPublisher.java` | Publishes rating events to RabbitMQ |
| `Rating.java` | Entity: id, userId, bookId, score (1-5), liked, timestamps |
| `Review.java` | Entity: id, userId, bookId, title, content, timestamps |
| `ReviewResponse.java` | Includes username field fetched from auth service |

#### API Endpoints - Ratings
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ratings` | Rate a book |
| PUT | `/api/ratings/{bookId}` | Update rating |
| GET | `/api/ratings/book/{bookId}` | Get all ratings for book |
| GET | `/api/ratings/book/{bookId}/average` | Get average rating |
| GET | `/api/ratings/user` | Get user's ratings |
| GET | `/api/ratings/my/{bookId}` | Get user's rating for book |
| DELETE | `/api/ratings/{bookId}` | Delete rating |

#### API Endpoints - Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reviews` | Create review |
| PUT | `/api/reviews/{id}` | Update review |
| GET | `/api/reviews/{id}` | Get review by ID |
| GET | `/api/reviews/book/{bookId}` | Get reviews for book |
| GET | `/api/reviews/user` | Get user's reviews |
| DELETE | `/api/reviews/{id}` | Delete review |

---

### Recommendation Service
**Port: 8085 | Database: 5436**

Python-based recommendation engine using collaborative filtering.

#### Directory Structure
```
recommendation-service/
├── app/
│   ├── __init__.py                  # Package initialization
│   ├── config.py                    # Configuration settings
│   ├── consumer.py                  # RabbitMQ event consumer
│   ├── database.py                  # SQLAlchemy database setup
│   ├── engine.py                    # Recommendation algorithms
│   ├── main.py                      # FastAPI application
│   ├── models.py                    # SQLAlchemy models
│   └── schemas.py                   # Pydantic schemas
├── requirements.txt                 # Python dependencies
└── Dockerfile                       # Container configuration
```

#### Files Explained

| File | Purpose |
|------|---------|
| `main.py` | FastAPI app with `/api/recommendations` endpoint, health check, startup tasks |
| `engine.py` | `RecommendationEngine` class with collaborative filtering using cosine similarity |
| `consumer.py` | `RatingEventConsumer` listens to both rating.queue and shelf.queue |
| `config.py` | Settings: database URL, RabbitMQ config, book catalog URL, thresholds |
| `database.py` | SQLAlchemy engine, session management, Base class |
| `models.py` | `UserRating` model (user_id, book_id, score, liked) |
| `schemas.py` | Pydantic models: `RecommendationResponse`, `BookRecommendation` |

#### Algorithm Details

The recommendation engine uses **Item-Based Collaborative Filtering**:

1. **User-Book Matrix**: Creates a sparse matrix of user ratings
2. **Cosine Similarity**: Computes similarity between books based on user ratings
3. **Prediction**: For a user, finds similar books to ones they've rated highly
4. **Fallback**: If insufficient data, returns popular books from book-catalog-service

```python
# Simplified algorithm
for each rated_book in user_ratings:
    similar_books = get_similar_books(rated_book)
    for similar_book in similar_books:
        score = similarity * user_rating
        recommendations.add(similar_book, score)
return top_n(recommendations)
```

#### Event Consumption
- **Rating Events**: Updates internal rating store, triggers model retrain
- **Shelf Events**: When book added to READ shelf, creates implicit rating (score=4)

#### API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/recommendations` | Get personalized recommendations |
| POST | `/api/recommendations/retrain` | Manually trigger model retrain |
| GET | `/health` | Health check endpoint |

---

### Frontend
**Port: 3000**

Modern React SPA with TypeScript and TailwindCSS.

#### Directory Structure
```
frontend/
├── src/
│   ├── api/
│   │   ├── books.ts                 # Book API calls
│   │   ├── client.ts                # Axios client setup
│   │   ├── ratings.ts               # Rating API calls
│   │   ├── recommendations.ts       # Recommendation API calls
│   │   ├── shelf.ts                 # Shelf API calls
│   │   └── user.ts                  # User/Auth API calls
│   ├── components/
│   │   ├── BookCard.tsx             # Book card display component
│   │   ├── Layout.tsx               # Main layout with navigation
│   │   └── StarRating.tsx           # Interactive star rating component
│   ├── pages/
│   │   ├── BookDetail.tsx           # Single book view with reviews
│   │   ├── Discover.tsx             # Book discovery/search page
│   │   ├── Home.tsx                 # Dashboard with recommendations
│   │   ├── Login.tsx                # Login form
│   │   ├── MyShelves.tsx            # Personal bookshelf management
│   │   ├── Onboarding.tsx           # New user onboarding flow
│   │   ├── Profile.tsx              # User profile settings
│   │   └── Register.tsx             # Registration form
│   ├── store/
│   │   └── authStore.ts             # Zustand auth state management
│   ├── types/
│   │   └── index.ts                 # TypeScript interfaces
│   ├── App.tsx                      # Main app with routing
│   ├── main.tsx                     # Application entry point
│   └── index.css                    # TailwindCSS imports
├── public/
│   └── ...                          # Static assets
├── index.html                       # HTML template
├── package.json                     # Dependencies
├── tailwind.config.js               # TailwindCSS configuration
├── tsconfig.json                    # TypeScript configuration
├── vite.config.ts                   # Vite build configuration
├── nginx.conf                       # Nginx config for Docker
└── Dockerfile                       # Multi-stage build
```

#### Files Explained

| File | Purpose |
|------|---------|
| `App.tsx` | React Router setup, protected routes, auth guards |
| `client.ts` | Axios instance with JWT interceptor, base URL config |
| `authStore.ts` | Zustand store: user, token, login/logout, onboarding state |
| `Home.tsx` | Dashboard showing recommendations, currently reading |
| `Discover.tsx` | Book search with tabs (All, Recommended, Top Rated, New) |
| `BookDetail.tsx` | Full book info, add to shelf, rate, review |
| `MyShelves.tsx` | Three-column shelf view (Reading, Want to Read, Read) |
| `Onboarding.tsx` | Select favorite books to seed recommendations |
| `BookCard.tsx` | Reusable book display with cover, rating, genre |
| `StarRating.tsx` | Interactive 1-5 star rating input |
| `Layout.tsx` | Navigation header, responsive sidebar |

#### State Management
```typescript
// authStore.ts
interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  needsOnboarding: boolean;
  login: (response: AuthResponse) => void;
  logout: () => void;
  completeOnboarding: () => void;
}
```

#### Key Features
- **JWT Authentication**: Stored in localStorage, attached to all API calls
- **React Query**: Caches API responses, automatic refetching
- **Responsive Design**: Mobile-first TailwindCSS styling
- **Toast Notifications**: react-hot-toast for feedback
- **Protected Routes**: Redirect to login if unauthenticated

---

## Database Schema

### Auth Database (port 5432)
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  avatar_url VARCHAR(500),
  bio TEXT,
  first_login BOOLEAN DEFAULT TRUE,
  onboarding_complete BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

### Book Database (port 5433)
```sql
CREATE TABLE books (
  id UUID PRIMARY KEY,
  title VARCHAR(500) NOT NULL,
  author VARCHAR(500) NOT NULL,
  isbn VARCHAR(20),
  description TEXT,
  cover_url VARCHAR(1000),
  genre VARCHAR(200),
  published_date DATE,
  page_count INTEGER,
  publisher VARCHAR(500),
  language VARCHAR(10),
  average_rating DOUBLE DEFAULT 0,
  ratings_count INTEGER DEFAULT 0,
  created_at TIMESTAMP
);
```

### Shelf Database (port 5434)
```sql
CREATE TABLE shelf_items (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  book_id UUID NOT NULL,
  shelf_type VARCHAR(20) NOT NULL,  -- READING, READ, TO_READ
  added_at TIMESTAMP,
  UNIQUE(user_id, book_id)
);
```

### Review Database (port 5435)
```sql
CREATE TABLE ratings (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  book_id UUID NOT NULL,
  score INTEGER NOT NULL,  -- 1-5
  liked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  UNIQUE(user_id, book_id)
);

CREATE TABLE reviews (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  book_id UUID NOT NULL,
  title VARCHAR(255),
  content TEXT NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  UNIQUE(user_id, book_id)
);
```

### Recommendation Database (port 5436)
```sql
CREATE TABLE user_ratings (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  book_id UUID NOT NULL,
  score INTEGER NOT NULL,
  liked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP,
  UNIQUE(user_id, book_id)
);

CREATE TABLE book_similarities (
  id UUID PRIMARY KEY,
  book_id_1 UUID NOT NULL,
  book_id_2 UUID NOT NULL,
  similarity FLOAT NOT NULL,
  UNIQUE(book_id_1, book_id_2)
);
```

---

## Message Queues

### RabbitMQ Configuration

#### Exchanges
| Exchange | Type | Purpose |
|----------|------|---------|
| `rating.exchange` | Topic | Rating events |
| `shelf.exchange` | Topic | Shelf change events |

#### Queues
| Queue | Binding | Consumer |
|-------|---------|----------|
| `rating.queue` | `rating.created` | recommendation-service |
| `shelf.queue` | `shelf.#` | shelf-service (producer) |
| `shelf.recommendation.queue` | `shelf.#` | recommendation-service |

#### Event Schemas

**Rating Event** (rating.created)
```json
{
  "userId": "uuid",
  "bookId": "uuid",
  "score": 5,
  "liked": true,
  "timestamp": "2024-01-01T00:00:00Z"
}
```

**Shelf Event** (shelf.read.added)
```json
{
  "userId": "uuid",
  "bookId": "uuid",
  "shelfType": "READ",
  "eventType": "ADDED",
  "previousShelfType": "READING",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

---

## Getting Started

### Prerequisites
- Docker Desktop
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/shelve.git
   cd shelve
   ```

2. **Start all services**
   ```bash
   docker-compose up -d --build
   ```

3. **Wait for services to be healthy** (~2-3 minutes)
   ```bash
   docker-compose ps
   ```

4. **Access the application**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - RabbitMQ Management: http://localhost:15672 (guest/guest)

### Development

**Rebuild a specific service:**
```bash
docker-compose up -d --build auth-service
```

**View logs:**
```bash
docker-compose logs -f auth-service
```

**Stop all services:**
```bash
docker-compose down
```

**Reset databases:**
```bash
docker-compose down -v
docker-compose up -d --build
```

---

## API Documentation

### Authentication Header
All protected endpoints require:
```
Authorization: Bearer <jwt-token>
```

### Response Format
**Success:**
```json
{
  "data": { ... },
  "message": "Success"
}
```

**Paginated:**
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

**Error:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}
```

---

## Project Structure

```
shelve/
├── api-gateway/                    # Spring Cloud Gateway
├── auth-service/                   # User authentication
├── book-catalog-service/           # Book management
├── config-server/                  # Centralized config
├── discovery-server/               # Eureka registry
├── frontend/                       # React SPA
├── recommendation-service/         # Python ML service
├── review-rating-service/          # Reviews & ratings
├── shelf-service/                  # Bookshelf management
├── docker-compose.yml              # Container orchestration
├── pom.xml                         # Parent Maven config
└── README.md                       # This file
```

---

## Environment Variables

### Docker Compose Defaults
```yaml
# Databases
POSTGRES_USER: shelve
POSTGRES_PASSWORD: shelve123

# RabbitMQ
RABBITMQ_DEFAULT_USER: shelve
RABBITMQ_DEFAULT_PASS: shelve123

# JWT
JWT_SECRET: shelve-secret-key-for-jwt-token-generation-minimum-256-bits-required
```

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Acknowledgments

- Spring Boot & Spring Cloud teams
- FastAPI framework
- React & Vite teams
- TailwindCSS
- Google Books API
- Open Library API
---

## Microservices Architecture Compliance Checklist

This section documents how Shelve complies with each requirement of a proper microservices architecture.

### ✅ 1. Each Microservice Has Its Own Database

| Service | Database | Port | Technology |
|---------|----------|------|------------|
| auth-service | auth_db | 5432 | PostgreSQL |
| book-catalog-service | books_db | 5433 | PostgreSQL |
| shelf-service | shelf_db | 5434 | PostgreSQL |
| review-rating-service | review_db | 5435 | PostgreSQL |
| recommendation-service | recommendation_db | 5436 | PostgreSQL |

**Implementation**: Each service connects to its dedicated PostgreSQL instance, ensuring complete data isolation. No service directly accesses another service's database.

---

### ✅ 2. Services Expose REST APIs

All microservices expose RESTful HTTP APIs:

| Service | Base Path | Example Endpoints |
|---------|-----------|-------------------|
| auth-service | `/api/auth`, `/api/users` | `POST /api/auth/login`, `GET /api/users/profile` |
| book-catalog-service | `/api/books` | `GET /api/books`, `GET /api/books/{id}`, `GET /api/books/search` |
| shelf-service | `/api/shelves` | `POST /api/shelves`, `GET /api/shelves/{type}` |
| review-rating-service | `/api/reviews`, `/api/ratings` | `POST /api/reviews`, `GET /api/ratings/book/{id}` |
| recommendation-service | `/api/recommendations` | `GET /api/recommendations` |

**Implementation**: Spring Boot `@RestController` for Java services, FastAPI router for Python service.

---

### ✅ 3. Service Discovery with Eureka Server

**Location**: `discovery-server/` (Port 8761)

**Implementation**:
- Netflix Eureka Server configured with `@EnableEurekaServer`
- All services register as Eureka clients
- Dashboard accessible at `http://localhost:8761`

**Service Registration** (example from auth-service `application.yml`):
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://discovery-server:8761/eureka/
  instance:
    preferIpAddress: true
```

---

### ✅ 4. API Gateway

**Location**: `api-gateway/` (Port 8080)

**Implementation**:
- Spring Cloud Gateway routes all external traffic
- Centralized JWT authentication via `AuthenticationFilter.java`
- Rate limiting and CORS configuration
- Route definitions in `GatewayConfig.java`

**Routing Table**:
```
/api/auth/**          → auth-service:8081
/api/users/**         → auth-service:8081
/api/books/**         → book-catalog-service:8082
/api/shelves/**       → shelf-service:8083
/api/reviews/**       → review-rating-service:8084
/api/ratings/**       → review-rating-service:8084
/api/recommendations/** → recommendation-service:8085
```

---

### ✅ 5. Synchronous REST Communication Between Services

| Consumer Service | Provider Service | Client Implementation | Endpoints Called |
|------------------|------------------|----------------------|------------------|
| review-rating-service | auth-service | `AuthServiceClient.java` (WebClient) | `GET /api/users/{id}/info`, `POST /api/users/batch/info` |
| shelf-service | book-catalog-service | `BookClient.java` (OpenFeign) | `GET /api/books/batch` |
| recommendation-service | book-catalog-service | HTTP requests (Python) | `GET /api/books/batch`, `GET /api/books/top-rated` |

**Implementation Details**:
- **review-rating-service**: Uses Spring WebClient with Resilience4j circuit breaker to fetch usernames when returning reviews
- **shelf-service**: Uses OpenFeign declarative client with circuit breaker to enrich shelf items with book details
- **recommendation-service**: Uses Python `httpx` client to fetch book details for recommendations

---

### ✅ 6. Asynchronous Event/Message Communication

**Message Broker**: RabbitMQ (Port 5672, Management UI: 15672)

**Exchanges & Queues**:

| Exchange | Type | Queue | Producer | Consumer |
|----------|------|-------|----------|----------|
| `rating.exchange` | Topic | `rating.queue` | review-rating-service | recommendation-service |
| `shelf.exchange` | Topic | `shelf.recommendation.queue` | shelf-service | recommendation-service |

**Events Published**:

1. **Rating Created Event** (`review-rating-service → recommendation-service`):
   - File: `RatingEventPublisher.java`
   - Trigger: User rates a book
   - Payload: `{ userId, bookId, score, liked, timestamp }`
   - Purpose: Update recommendation model with new rating data

2. **Shelf Event** (`shelf-service → recommendation-service`):
   - File: `ShelfEventPublisher.java`
   - Trigger: Book added to READ shelf
   - Payload: `{ userId, bookId, shelfType, eventType, timestamp }`
   - Purpose: Create implicit rating when user finishes a book

**Event Consumption**:
- File: `recommendation-service/app/consumer.py`
- Both rating and shelf events trigger recommendation model updates

---

### ✅ 7. Error Handling & Resilience (Circuit Breaker Pattern)

**Implementation**: Resilience4j Circuit Breaker

#### review-rating-service

**File**: `AuthServiceClient.java`
```java
@CircuitBreaker(name = "authService", fallbackMethod = "getUserInfoFallback")
@Retry(name = "authService")
public UserInfo getUserInfo(String userId) { ... }

// Fallback returns default UserInfo when auth-service is unavailable
private UserInfo getUserInfoFallback(String userId, Throwable t) {
    return new UserInfo(userId, "Unknown User", null);
}
```

**Configuration** (`application.yml`):
```yaml
resilience4j:
  circuitbreaker:
    instances:
      authService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      authService:
        maxAttempts: 3
        waitDuration: 500ms
```

#### shelf-service

**File**: `BookClient.java`
```java
@FeignClient(name = "book-catalog-service", fallback = BookClientFallback.class)
public interface BookClient {
    @CircuitBreaker(name = "bookCatalogService")
    List<BookResponse> getBooksByIds(@RequestParam List<String> ids);
}
```

**Fallback**: `BookClientFallback.java` returns empty list when book-catalog-service is unavailable

#### Additional Error Handling

- **Global Exception Handlers**: Each service has `GlobalExceptionHandler.java` with `@ControllerAdvice`
- **Custom Exceptions**: Service-specific exceptions (e.g., `BookNotFoundException`, `UserAlreadyExistsException`)
- **Standardized Error Responses**: Consistent JSON error format across all services

---

### ✅ 8. Containerization with Docker

**Files**:
- `docker-compose.yml` - Orchestrates all 14 containers
- `Dockerfile` in each service directory

**Containers Defined**:

| Container | Image | Ports |
|-----------|-------|-------|
| postgres-auth | postgres:15 | 5432 |
| postgres-books | postgres:15 | 5433 |
| postgres-shelf | postgres:15 | 5434 |
| postgres-review | postgres:15 | 5435 |
| postgres-recommendation | postgres:15 | 5436 |
| rabbitmq | rabbitmq:3-management | 5672, 15672 |
| config-server | Custom (Spring Boot) | 8888 |
| discovery-server | Custom (Spring Boot) | 8761 |
| api-gateway | Custom (Spring Boot) | 8080 |
| auth-service | Custom (Spring Boot) | 8081 |
| book-catalog-service | Custom (Spring Boot) | 8082 |
| shelf-service | Custom (Spring Boot) | 8083 |
| review-rating-service | Custom (Spring Boot) | 8084 |
| recommendation-service | Custom (Python/FastAPI) | 8085 |
| frontend | Custom (React/Nginx) | 3000 |

**Docker Features Used**:
- Multi-stage builds (frontend)
- Health checks for service dependencies
- Named volumes for data persistence
- Custom network for inter-service communication
- Environment variable injection

---

### Summary

| # | Requirement | Status | Implementation |
|---|-------------|--------|----------------|
| 1 | Each MS has its own database | ✅ | 5 PostgreSQL instances (ports 5432-5436) |
| 2 | REST APIs | ✅ | Spring @RestController, FastAPI router |
| 3 | Service Discovery (Eureka) | ✅ | discovery-server with @EnableEurekaServer |
| 4 | API Gateway | ✅ | Spring Cloud Gateway with JWT auth |
| 5 | REST inter-service communication | ✅ | WebClient, OpenFeign, httpx |
| 6 | Event/Message communication | ✅ | RabbitMQ with rating & shelf events |
| 7 | Circuit Breaker & Error Handling | ✅ | Resilience4j with fallbacks |
| 8 | Docker containerization | ✅ | docker-compose with 15 containers |

**All 8 microservices architecture requirements are fully implemented.**

---

## API Endpoints Reference

All endpoints are accessed through the **API Gateway** at `http://localhost:8080`. The gateway handles JWT authentication and routes requests to the appropriate microservice.

### Authentication Headers

For protected endpoints, include the JWT token in the `Authorization` header:
```
Authorization: Bearer <your-jwt-token>
```

---

### 🔐 Auth Service (`/api/auth`, `/api/users`)

#### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |

#### Protected Endpoints (Require JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/profile` | Get current user's profile |
| PUT | `/api/users/profile` | Update user profile |
| PUT | `/api/users/password` | Change password |
| POST | `/api/users/onboarding/complete` | Mark onboarding as complete |
| GET | `/api/users/{userId}/info` | Get user info by ID (internal) |
| POST | `/api/users/batch/info` | Get multiple users info (internal) |

#### Postman Examples

**Register User**
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "password": "securePassword123"
}
```

**Response (201 Created):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@example.com",
    "name": "John Doe",
    "onboardingComplete": false
}
```

**Login**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "email": "john.doe@example.com",
    "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@example.com",
    "name": "John Doe",
    "onboardingComplete": true
}
```

**Get User Profile**
```http
GET http://localhost:8080/api/users/profile
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "John Doe",
    "email": "john.doe@example.com",
    "avatarUrl": null,
    "bio": null,
    "onboardingComplete": true,
    "createdAt": "2024-01-15T10:30:00"
}
```

---

### 📚 Book Catalog Service (`/api/books`)

#### Endpoints (All Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/books` | Get all books (paginated) |
| GET | `/api/books/{id}` | Get book by ID |
| GET | `/api/books/batch?ids=id1,id2` | Get multiple books by IDs |
| GET | `/api/books/search?q=query` | Search books by title/author |
| GET | `/api/books/genre/{genre}` | Get books by genre |
| GET | `/api/books/genres` | Get all available genres |
| GET | `/api/books/top-rated` | Get top-rated books |
| GET | `/api/books/recent` | Get recently added books |
| POST | `/api/books` | Add a new book |

#### Query Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `page` | 0 | Page number (0-indexed) |
| `size` | 20 | Items per page |
| `sortBy` | title | Sort field |
| `sortDir` | asc | Sort direction (asc/desc) |

#### Postman Examples

**Get All Books (Paginated)**
```http
GET http://localhost:8080/api/books?page=0&size=10&sortBy=title&sortDir=asc
```

**Response (200 OK):**
```json
{
    "content": [
        {
            "id": "OL12345W",
            "title": "The Great Gatsby",
            "author": "F. Scott Fitzgerald",
            "description": "A story of the Jazz Age...",
            "coverUrl": "https://covers.openlibrary.org/b/id/12345-L.jpg",
            "genre": "Fiction",
            "publishedYear": 1925,
            "isbn": "9780743273565",
            "averageRating": 4.2,
            "totalRatings": 150
        }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10
}
```

**Search Books**
```http
GET http://localhost:8080/api/books/search?q=harry potter&page=0&size=5
```

**Get Book by ID**
```http
GET http://localhost:8080/api/books/OL12345W
```

**Response (200 OK):**
```json
{
    "id": "OL12345W",
    "title": "Harry Potter and the Philosopher's Stone",
    "author": "J.K. Rowling",
    "description": "The first book in the Harry Potter series...",
    "coverUrl": "https://covers.openlibrary.org/b/id/67890-L.jpg",
    "genre": "Fantasy",
    "publishedYear": 1997,
    "isbn": "9780747532699",
    "averageRating": 4.8,
    "totalRatings": 5000
}
```

**Get Books by Genre**
```http
GET http://localhost:8080/api/books/genre/Fantasy?page=0&size=10
```

**Create New Book**
```http
POST http://localhost:8080/api/books
Content-Type: application/json

{
    "title": "New Book Title",
    "author": "Author Name",
    "description": "A fascinating story about...",
    "coverUrl": "https://example.com/cover.jpg",
    "genre": "Fiction",
    "publishedYear": 2024,
    "isbn": "9781234567890"
}
```

---

### 📖 Shelf Service (`/api/shelves`)

#### Endpoints (All Protected - Require JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/shelves` | Get all user's shelves with counts |
| GET | `/api/shelves/{shelfType}` | Get books on a specific shelf |
| POST | `/api/shelves/{shelfType}/books/{bookId}` | Add book to shelf |
| PUT | `/api/shelves/books/{bookId}` | Move book between shelves |
| DELETE | `/api/shelves/books/{bookId}` | Remove book from all shelves |
| GET | `/api/shelves/books/{bookId}/status` | Check which shelf a book is on |

#### Shelf Types

| Type | Description |
|------|-------------|
| `WANT_TO_READ` | Books user wants to read |
| `READING` | Books currently being read |
| `READ` | Books already finished |

#### Postman Examples

**Get All Shelves**
```http
GET http://localhost:8080/api/shelves
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "wantToRead": {
        "count": 15,
        "items": []
    },
    "reading": {
        "count": 3,
        "items": []
    },
    "read": {
        "count": 42,
        "items": []
    }
}
```

**Get Books on "Reading" Shelf**
```http
GET http://localhost:8080/api/shelves/READING
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
[
    {
        "id": "abc123",
        "bookId": "OL12345W",
        "shelfType": "READING",
        "addedAt": "2024-01-20T14:30:00",
        "book": {
            "id": "OL12345W",
            "title": "The Great Gatsby",
            "author": "F. Scott Fitzgerald",
            "coverUrl": "https://covers.openlibrary.org/b/id/12345-L.jpg"
        }
    }
]
```

**Add Book to Shelf**
```http
POST http://localhost:8080/api/shelves/WANT_TO_READ/books/OL12345W
Authorization: Bearer <jwt-token>
```

**Response (201 Created):**
```json
{
    "id": "def456",
    "bookId": "OL12345W",
    "shelfType": "WANT_TO_READ",
    "addedAt": "2024-01-21T09:15:00",
    "book": {
        "id": "OL12345W",
        "title": "1984",
        "author": "George Orwell",
        "coverUrl": "https://covers.openlibrary.org/b/id/54321-L.jpg"
    }
}
```

**Move Book Between Shelves**
```http
PUT http://localhost:8080/api/shelves/books/OL12345W
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
    "targetShelf": "READING"
}
```

**Remove Book from Shelf**
```http
DELETE http://localhost:8080/api/shelves/books/OL12345W
Authorization: Bearer <jwt-token>
```

**Response (204 No Content)**

---

### ⭐ Review & Rating Service (`/api/reviews`, `/api/ratings`)

#### Review Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reviews` | Create a review (Protected) |
| PUT | `/api/reviews/{reviewId}` | Update a review (Protected) |
| GET | `/api/reviews/{reviewId}` | Get review by ID |
| GET | `/api/reviews/book/{bookId}` | Get all reviews for a book |
| GET | `/api/reviews/user` | Get current user's reviews (Protected) |
| DELETE | `/api/reviews/{reviewId}` | Delete a review (Protected) |

#### Rating Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ratings` | Rate a book (Protected) |
| POST | `/api/ratings/initial` | Submit initial liked books (Protected) |
| PUT | `/api/ratings/book/{bookId}?score=4` | Update rating (Protected) |
| GET | `/api/ratings/book/{bookId}` | Get user's rating for a book (Protected) |
| GET | `/api/ratings/user` | Get all user's ratings (Protected) |
| GET | `/api/ratings/book/{bookId}/all` | Get all ratings for a book |
| GET | `/api/ratings/book/{bookId}/average` | Get average rating for a book |

#### Postman Examples

**Create Review**
```http
POST http://localhost:8080/api/reviews
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
    "bookId": "OL12345W",
    "title": "A Masterpiece of Literature",
    "content": "This book truly captures the essence of the American Dream. Fitzgerald's prose is elegant and the characters are unforgettable. A must-read for anyone interested in classic literature."
}
```

**Response (201 Created):**
```json
{
    "id": "rev-123456",
    "bookId": "OL12345W",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "John Doe",
    "title": "A Masterpiece of Literature",
    "content": "This book truly captures the essence of the American Dream...",
    "createdAt": "2024-01-21T10:30:00",
    "updatedAt": "2024-01-21T10:30:00"
}
```

**Get Book Reviews**
```http
GET http://localhost:8080/api/reviews/book/OL12345W?page=0&size=10
```

**Response (200 OK):**
```json
{
    "content": [
        {
            "id": "rev-123456",
            "bookId": "OL12345W",
            "userId": "550e8400-e29b-41d4-a716-446655440000",
            "username": "John Doe",
            "title": "A Masterpiece of Literature",
            "content": "This book truly captures the essence...",
            "createdAt": "2024-01-21T10:30:00",
            "updatedAt": "2024-01-21T10:30:00"
        }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
}
```

**Rate a Book**
```http
POST http://localhost:8080/api/ratings
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
    "bookId": "OL12345W",
    "score": 5
}
```

**Response (201 Created):**
```json
{
    "id": "rat-789012",
    "bookId": "OL12345W",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "score": 5,
    "createdAt": "2024-01-21T10:35:00"
}
```

**Submit Initial Liked Books (Onboarding)**
```http
POST http://localhost:8080/api/ratings/initial
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
    "bookIds": ["OL12345W", "OL67890W", "OL11111W"]
}
```

**Response (201 Created):**
```json
[
    {
        "id": "rat-001",
        "bookId": "OL12345W",
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "score": 5,
        "createdAt": "2024-01-21T10:40:00"
    },
    {
        "id": "rat-002",
        "bookId": "OL67890W",
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "score": 5,
        "createdAt": "2024-01-21T10:40:00"
    }
]
```

**Get Average Rating for a Book**
```http
GET http://localhost:8080/api/ratings/book/OL12345W/average
```

**Response (200 OK):**
```json
4.5
```

---

### 🤖 Recommendation Service (`/api/recommendations`)

#### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/recommendations` | Get personalized recommendations (Protected) |
| POST | `/api/recommendations/retrain` | Manually retrain ML model (Internal) |
| GET | `/api/recommendations/health` | Health check |

#### Query Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `limit` | 10 | Number of recommendations (max 50) |

#### Postman Examples

**Get Recommendations**
```http
GET http://localhost:8080/api/recommendations?limit=10
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "recommendations": [
        {
            "bookId": "OL98765W",
            "score": 0.95,
            "reason": "Based on your interest in Fantasy"
        },
        {
            "bookId": "OL54321W",
            "score": 0.89,
            "reason": "Users with similar taste also enjoyed this"
        }
    ],
    "generatedAt": "2024-01-21T11:00:00"
}
```

**Health Check**
```http
GET http://localhost:8080/api/recommendations/health
```

**Response (200 OK):**
```json
{
    "status": "UP",
    "service": "recommendation-service",
    "timestamp": "2024-01-21T11:05:00"
}
```

---

## Complete Testing Workflow in Postman

Here's a step-by-step workflow to test the entire application:

### Step 1: Register a New User
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
    "name": "Test User",
    "email": "test@example.com",
    "password": "password123"
}
```
📝 Save the `token` from the response.

### Step 2: Search for Books
```http
GET http://localhost:8080/api/books/search?q=lord of the rings
```
📝 Note some book IDs from the results.

### Step 3: Like Initial Books (Onboarding)
```http
POST http://localhost:8080/api/ratings/initial
Authorization: Bearer <token>
Content-Type: application/json

{
    "bookIds": ["<book-id-1>", "<book-id-2>", "<book-id-3>"]
}
```

### Step 4: Complete Onboarding
```http
POST http://localhost:8080/api/users/onboarding/complete
Authorization: Bearer <token>
```

### Step 5: Add Book to "Want to Read" Shelf
```http
POST http://localhost:8080/api/shelves/WANT_TO_READ/books/<book-id>
Authorization: Bearer <token>
```

### Step 6: Move Book to "Reading" Shelf
```http
PUT http://localhost:8080/api/shelves/books/<book-id>
Authorization: Bearer <token>
Content-Type: application/json

{
    "targetShelf": "READING"
}
```

### Step 7: Rate the Book
```http
POST http://localhost:8080/api/ratings
Authorization: Bearer <token>
Content-Type: application/json

{
    "bookId": "<book-id>",
    "score": 5
}
```

### Step 8: Write a Review
```http
POST http://localhost:8080/api/reviews
Authorization: Bearer <token>
Content-Type: application/json

{
    "bookId": "<book-id>",
    "title": "Absolutely Amazing!",
    "content": "This is one of the best books I have ever read. The world-building is incredible and the characters are so well developed."
}
```

### Step 9: Move Book to "Read" Shelf
```http
PUT http://localhost:8080/api/shelves/books/<book-id>
Authorization: Bearer <token>
Content-Type: application/json

{
    "targetShelf": "READ"
}
```

### Step 10: Get Recommendations
```http
GET http://localhost:8080/api/recommendations?limit=10
Authorization: Bearer <token>
```

---

## Postman Collection Import

You can import this collection directly into Postman by creating a new collection with these environment variables:

| Variable | Value |
|----------|-------|
| `BASE_URL` | `http://localhost:8080` |
| `TOKEN` | (set after login) |

Then use `{{BASE_URL}}` and `{{TOKEN}}` in your requests:
```
{{BASE_URL}}/api/auth/login
Authorization: Bearer {{TOKEN}}
```

---

## Error Response Format

All services return errors in a consistent format:

```json
{
    "timestamp": "2024-01-21T12:00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "path": "/api/auth/register"
}
```

### Common HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request succeeded |
| 201 | Created - Resource created |
| 204 | No Content - Deleted successfully |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing/invalid JWT |
| 403 | Forbidden - Access denied |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Resource already exists |
| 500 | Internal Server Error |