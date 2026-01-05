import numpy as np
import pandas as pd
from scipy.sparse import csr_matrix
from sklearn.neighbors import NearestNeighbors
from sqlalchemy.orm import Session
from typing import List, Dict, Tuple
from uuid import UUID
import logging
import httpx

from app.models import UserRating, BookSimilarity, UserRecommendation
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


class CollaborativeFilteringEngine:
    """
    Implements item-based collaborative filtering for book recommendations.
    Uses cosine similarity to find similar books based on user ratings.
    """
    
    def __init__(self):
        self.model = None
        self.book_mapper = {}
        self.book_inv_mapper = {}
        self.ratings_matrix = None
        
    def build_ratings_matrix(self, db: Session) -> Tuple[csr_matrix, Dict, Dict]:
        """Build user-item ratings matrix from database."""
        ratings = db.query(UserRating).all()
        
        if not ratings:
            logger.warning("No ratings found in database")
            return None, {}, {}
        
        # Create DataFrame
        df = pd.DataFrame([
            {
                'user_id': str(r.user_id),
                'book_id': str(r.book_id),
                'score': r.score
            }
            for r in ratings
        ])
        
        # Create mappings
        user_ids = df['user_id'].unique()
        book_ids = df['book_id'].unique()
        
        user_mapper = {uid: idx for idx, uid in enumerate(user_ids)}
        book_mapper = {bid: idx for idx, bid in enumerate(book_ids)}
        
        user_inv_mapper = {idx: uid for uid, idx in user_mapper.items()}
        book_inv_mapper = {idx: bid for bid, idx in book_mapper.items()}
        
        # Create sparse matrix
        user_indices = df['user_id'].map(user_mapper).values
        book_indices = df['book_id'].map(book_mapper).values
        scores = df['score'].values
        
        ratings_matrix = csr_matrix(
            (scores, (user_indices, book_indices)),
            shape=(len(user_ids), len(book_ids))
        )
        
        self.book_mapper = book_mapper
        self.book_inv_mapper = book_inv_mapper
        self.ratings_matrix = ratings_matrix
        
        return ratings_matrix, book_mapper, book_inv_mapper
    
    def train_model(self, db: Session):
        """Train the collaborative filtering model."""
        logger.info("Training recommendation model...")
        
        ratings_matrix, _, _ = self.build_ratings_matrix(db)
        
        if ratings_matrix is None or ratings_matrix.shape[0] < 2:
            logger.warning("Not enough data to train model")
            return
        
        # Use item-based collaborative filtering
        # Transpose matrix for item-based (books as rows)
        item_matrix = ratings_matrix.T
        
        # Fit KNN model
        self.model = NearestNeighbors(
            metric='cosine',
            algorithm='brute',
            n_neighbors=min(20, item_matrix.shape[0])
        )
        self.model.fit(item_matrix)
        
        logger.info(f"Model trained with {item_matrix.shape[0]} books and {ratings_matrix.shape[0]} users")
        
        # Compute and store book similarities
        self._compute_book_similarities(db, item_matrix)
    
    def _compute_book_similarities(self, db: Session, item_matrix: csr_matrix):
        """Compute and store pairwise book similarities."""
        if self.model is None:
            return
        
        n_books = item_matrix.shape[0]
        
        # Clear existing similarities
        db.query(BookSimilarity).delete()
        
        # For each book, find similar books
        for book_idx in range(n_books):
            if book_idx not in self.book_inv_mapper:
                continue
                
            try:
                distances, indices = self.model.kneighbors(
                    item_matrix[book_idx].reshape(1, -1),
                    n_neighbors=min(10, n_books)
                )
                
                book_id_1 = self.book_inv_mapper[book_idx]
                
                for dist, idx in zip(distances[0], indices[0]):
                    if idx == book_idx:
                        continue
                    if idx not in self.book_inv_mapper:
                        continue
                        
                    book_id_2 = self.book_inv_mapper[idx]
                    similarity = 1 - dist  # Convert distance to similarity
                    
                    if similarity > 0.1:  # Only store meaningful similarities
                        similarity_record = BookSimilarity(
                            book_id_1=UUID(book_id_1),
                            book_id_2=UUID(book_id_2),
                            similarity_score=float(similarity)
                        )
                        db.add(similarity_record)
            except Exception as e:
                logger.error(f"Error computing similarity for book {book_idx}: {e}")
        
        db.commit()
        logger.info("Book similarities computed and stored")
    
    def get_recommendations(
        self, 
        db: Session, 
        user_id: str, 
        n_recommendations: int = 10
    ) -> List[Dict]:
        """
        Generate recommendations for a user using collaborative filtering.
        """
        user_uuid = UUID(user_id)
        
        # Get user's ratings
        user_ratings = db.query(UserRating).filter(
            UserRating.user_id == user_uuid
        ).all()
        
        if not user_ratings:
            logger.info(f"No ratings found for user {user_id}, returning popular books")
            return self._get_popular_books(db, n_recommendations)
        
        # Get books the user has rated/liked
        rated_book_ids = {str(r.book_id) for r in user_ratings}
        liked_book_ids = [str(r.book_id) for r in user_ratings if r.liked or r.score >= 4]
        
        recommendations = {}
        
        # Find similar books to the ones user liked
        for book_id in liked_book_ids:
            similar_books = db.query(BookSimilarity).filter(
                BookSimilarity.book_id_1 == UUID(book_id)
            ).order_by(BookSimilarity.similarity_score.desc()).limit(10).all()
            
            for sim in similar_books:
                sim_book_id = str(sim.book_id_2)
                
                # Skip if user already rated this book
                if sim_book_id in rated_book_ids:
                    continue
                
                # Aggregate scores
                if sim_book_id not in recommendations:
                    recommendations[sim_book_id] = {
                        'score': 0,
                        'reasons': []
                    }
                
                recommendations[sim_book_id]['score'] += sim.similarity_score
                recommendations[sim_book_id]['reasons'].append(book_id)
        
        # Sort by score and take top N
        sorted_recs = sorted(
            recommendations.items(),
            key=lambda x: x[1]['score'],
            reverse=True
        )[:n_recommendations]
        
        # Format recommendations
        result = []
        for book_id, data in sorted_recs:
            reason = f"Similar to books you liked" if data['reasons'] else None
            result.append({
                'bookId': book_id,
                'score': round(data['score'], 3),
                'reason': reason
            })
        
        # If not enough recommendations, fill with popular books
        if len(result) < n_recommendations:
            popular = self._get_popular_books(
                db, 
                n_recommendations - len(result),
                exclude_ids=rated_book_ids | {r['bookId'] for r in result}
            )
            result.extend(popular)
        
        return result
    
    def _get_popular_books(
        self, 
        db: Session, 
        n: int, 
        exclude_ids: set = None
    ) -> List[Dict]:
        """Get most popular books - first from ratings, then from catalog service."""
        from sqlalchemy import func
        
        exclude_ids = exclude_ids or set()
        
        # First try to get from local ratings
        popular_query = db.query(
            UserRating.book_id,
            func.count(UserRating.id).label('count'),
            func.avg(UserRating.score).label('avg_score')
        ).group_by(UserRating.book_id).order_by(
            func.count(UserRating.id).desc(),
            func.avg(UserRating.score).desc()
        ).limit(n + len(exclude_ids))
        
        popular = []
        for row in popular_query:
            book_id = str(row.book_id)
            if book_id not in exclude_ids:
                popular.append({
                    'bookId': book_id,
                    'score': round(float(row.avg_score) / 5.0, 3),  # Normalize to 0-1
                    'reason': 'Popular among readers'
                })
                if len(popular) >= n:
                    break
        
        # If not enough, fetch from book catalog service
        if len(popular) < n:
            try:
                remaining = n - len(popular)
                catalog_books = self._fetch_catalog_books(remaining, exclude_ids | {p['bookId'] for p in popular})
                popular.extend(catalog_books)
            except Exception as e:
                logger.warning(f"Could not fetch from catalog service: {e}")
        
        return popular
    
    def _fetch_catalog_books(self, n: int, exclude_ids: set = None) -> List[Dict]:
        """Fetch popular books from the book catalog service."""
        exclude_ids = exclude_ids or set()
        
        try:
            with httpx.Client(timeout=10.0) as client:
                # Fetch top-rated books from catalog
                response = client.get(
                    f"{settings.book_catalog_service_url}/top-rated",
                    params={"size": n + len(exclude_ids)}
                )
                response.raise_for_status()
                data = response.json()
                
                books = []
                for book in data.get('content', []):
                    book_id = book.get('id')
                    if book_id and book_id not in exclude_ids:
                        books.append({
                            'bookId': book_id,
                            'score': 0.8,  # Default high score for popular books
                            'reason': 'Highly rated by readers'
                        })
                        if len(books) >= n:
                            break
                
                return books
        except Exception as e:
            logger.error(f"Error fetching from catalog: {e}")
            return []


# Global engine instance
recommendation_engine = CollaborativeFilteringEngine()
