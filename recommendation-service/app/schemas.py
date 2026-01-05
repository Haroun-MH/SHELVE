from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
from uuid import UUID


class RatingEvent(BaseModel):
    userId: str
    bookId: str
    score: int
    liked: bool
    eventType: str
    timestamp: datetime


class BookRecommendation(BaseModel):
    bookId: str
    score: float
    reason: Optional[str] = None


class RecommendationResponse(BaseModel):
    userId: str
    recommendations: List[BookRecommendation]
    generatedAt: datetime


class HealthResponse(BaseModel):
    status: str
    service: str
    timestamp: datetime
