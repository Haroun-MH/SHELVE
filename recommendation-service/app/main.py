from fastapi import FastAPI, Depends, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from datetime import datetime
from contextlib import asynccontextmanager
import logging

from app.config import get_settings
from app.database import get_db, engine, Base
from app.schemas import RecommendationResponse, BookRecommendation, HealthResponse
from app.engine import recommendation_engine
from app.consumer import start_consumer_thread

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events."""
    # Startup
    logger.info("Starting Recommendation Service...")
    
    # Create database tables
    Base.metadata.create_all(bind=engine)
    
    # Initial model training
    db = next(get_db())
    try:
        recommendation_engine.train_model(db)
    except Exception as e:
        logger.warning(f"Initial model training failed (this is normal if no data): {e}")
    finally:
        db.close()
    
    # Start RabbitMQ consumer
    try:
        consumer = start_consumer_thread()
        logger.info("RabbitMQ consumer started")
    except Exception as e:
        logger.warning(f"Failed to start RabbitMQ consumer: {e}")
    
    yield
    
    # Shutdown
    logger.info("Shutting down Recommendation Service...")


app = FastAPI(
    title="Shelve Recommendation Service",
    description="Book recommendation engine using collaborative filtering",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/recommendations", response_model=RecommendationResponse)
async def get_recommendations(
    x_user_id: str = Header(..., alias="X-User-Id"),
    limit: int = 10,
    db: Session = Depends(get_db)
):
    """Get personalized book recommendations for a user."""
    try:
        recommendations = recommendation_engine.get_recommendations(
            db=db,
            user_id=x_user_id,
            n_recommendations=min(limit, settings.max_recommendations)
        )
        
        return RecommendationResponse(
            userId=x_user_id,
            recommendations=[
                BookRecommendation(**rec) for rec in recommendations
            ],
            generatedAt=datetime.utcnow()
        )
    except Exception as e:
        logger.error(f"Error generating recommendations: {e}")
        raise HTTPException(status_code=500, detail="Failed to generate recommendations")


@app.post("/api/recommendations/retrain")
async def retrain_model(db: Session = Depends(get_db)):
    """Manually trigger model retraining."""
    try:
        recommendation_engine.train_model(db)
        return {"status": "success", "message": "Model retrained successfully"}
    except Exception as e:
        logger.error(f"Error retraining model: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrain model")


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    return HealthResponse(
        status="UP",
        service="recommendation-service",
        timestamp=datetime.utcnow()
    )


@app.get("/api/recommendations/health", response_model=HealthResponse)
async def api_health_check():
    """API health check endpoint."""
    return HealthResponse(
        status="UP",
        service="recommendation-service",
        timestamp=datetime.utcnow()
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.service_port,
        reload=settings.debug
    )
