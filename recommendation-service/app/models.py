from sqlalchemy import Column, String, Integer, Float, DateTime, Boolean
from sqlalchemy.dialects.postgresql import UUID
from datetime import datetime
import uuid

from app.database import Base


class UserRating(Base):
    __tablename__ = "user_ratings"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    book_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    score = Column(Integer, nullable=False)
    liked = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class BookSimilarity(Base):
    __tablename__ = "book_similarities"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    book_id_1 = Column(UUID(as_uuid=True), nullable=False, index=True)
    book_id_2 = Column(UUID(as_uuid=True), nullable=False, index=True)
    similarity_score = Column(Float, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class UserRecommendation(Base):
    __tablename__ = "user_recommendations"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    book_id = Column(UUID(as_uuid=True), nullable=False)
    score = Column(Float, nullable=False)
    reason = Column(String(255))
    created_at = Column(DateTime, default=datetime.utcnow)
