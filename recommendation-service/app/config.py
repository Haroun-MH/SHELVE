from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # Database
    database_url: str = "postgresql://shelve:shelve123@localhost:5436/shelve_recommendations"
    
    # RabbitMQ
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "guest"
    rabbitmq_password: str = "guest"
    
    # Service
    service_port: int = 8085
    debug: bool = False
    
    # Book Catalog Service URL (matches env var BOOK_CATALOG_SERVICE_URL)
    book_catalog_service_url: str = "http://localhost:8082/api/books"
    
    # Recommendation Engine
    min_ratings_for_recommendation: int = 3
    max_recommendations: int = 20
    
    class Config:
        env_file = ".env"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
