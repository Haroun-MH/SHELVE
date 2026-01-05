import pika
import json
import threading
import logging
from uuid import UUID

from app.config import get_settings
from app.database import SessionLocal
from app.models import UserRating
from app.engine import recommendation_engine

logger = logging.getLogger(__name__)
settings = get_settings()


class RatingEventConsumer:
    """Consumes rating and shelf events from RabbitMQ and updates the recommendation model."""
    
    def __init__(self):
        self.connection = None
        self.channel = None
        self.should_stop = False
        
    def connect(self):
        """Establish connection to RabbitMQ."""
        try:
            credentials = pika.PlainCredentials(
                settings.rabbitmq_user,
                settings.rabbitmq_password
            )
            parameters = pika.ConnectionParameters(
                host=settings.rabbitmq_host,
                port=settings.rabbitmq_port,
                credentials=credentials
            )
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()
            
            # Declare rating exchange and queue
            self.channel.exchange_declare(
                exchange='rating.exchange',
                exchange_type='topic',
                durable=True
            )
            self.channel.queue_declare(queue='rating.queue', durable=True)
            self.channel.queue_bind(
                queue='rating.queue',
                exchange='rating.exchange',
                routing_key='rating.created'
            )
            
            # Declare shelf exchange and queue for recommendation updates
            self.channel.exchange_declare(
                exchange='shelf.exchange',
                exchange_type='topic',
                durable=True
            )
            self.channel.queue_declare(queue='shelf.recommendation.queue', durable=True)
            self.channel.queue_bind(
                queue='shelf.recommendation.queue',
                exchange='shelf.exchange',
                routing_key='shelf.#'
            )
            
            logger.info("Connected to RabbitMQ")
        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {e}")
            raise
    
    def process_message(self, ch, method, properties, body):
        """Process incoming rating event."""
        try:
            event = json.loads(body)
            logger.info(f"Received rating event: {event}")
            
            db = SessionLocal()
            try:
                # Check if rating already exists
                existing = db.query(UserRating).filter(
                    UserRating.user_id == UUID(event['userId']),
                    UserRating.book_id == UUID(event['bookId'])
                ).first()
                
                if existing:
                    # Update existing rating
                    existing.score = event['score']
                    existing.liked = event['liked']
                else:
                    # Create new rating
                    rating = UserRating(
                        user_id=UUID(event['userId']),
                        book_id=UUID(event['bookId']),
                        score=event['score'],
                        liked=event['liked']
                    )
                    db.add(rating)
                
                db.commit()
                logger.info(f"Rating saved for user {event['userId']}, book {event['bookId']}")
                
                # Retrain model periodically (in production, use a more sophisticated approach)
                # For now, we'll train on every 10th rating
                rating_count = db.query(UserRating).count()
                if rating_count % 10 == 0:
                    logger.info("Triggering model retrain...")
                    recommendation_engine.train_model(db)
                    
            finally:
                db.close()
            
            ch.basic_ack(delivery_tag=method.delivery_tag)
            
        except Exception as e:
            logger.error(f"Error processing message: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
    
    def process_shelf_message(self, ch, method, properties, body):
        """Process incoming shelf event - trigger recommendation update when books added to READ shelf."""
        try:
            event = json.loads(body)
            logger.info(f"Received shelf event: {event}")
            
            # Trigger model retrain when book is added/moved to READ shelf
            shelf_type = event.get('shelfType', '')
            if shelf_type == 'READ':
                logger.info(f"Book added to READ shelf, triggering recommendation update for user {event.get('userId')}")
                db = SessionLocal()
                try:
                    # Also create a rating entry if it doesn't exist (implicit like)
                    user_id = UUID(event['userId'])
                    book_id = UUID(event['bookId'])
                    
                    existing = db.query(UserRating).filter(
                        UserRating.user_id == user_id,
                        UserRating.book_id == book_id
                    ).first()
                    
                    if not existing:
                        # Create implicit rating for READ books
                        rating = UserRating(
                            user_id=user_id,
                            book_id=book_id,
                            score=4,  # Implicit positive rating for finished books
                            liked=True
                        )
                        db.add(rating)
                        db.commit()
                        logger.info(f"Created implicit rating for READ book {book_id}")
                    
                    # Retrain model
                    recommendation_engine.train_model(db)
                finally:
                    db.close()
            
            ch.basic_ack(delivery_tag=method.delivery_tag)
            
        except Exception as e:
            logger.error(f"Error processing shelf message: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
    
    def start_consuming(self):
        """Start consuming messages."""
        self.connect()
        
        self.channel.basic_qos(prefetch_count=1)
        
        # Consume rating events
        self.channel.basic_consume(
            queue='rating.queue',
            on_message_callback=self.process_message
        )
        
        # Consume shelf events
        self.channel.basic_consume(
            queue='shelf.recommendation.queue',
            on_message_callback=self.process_shelf_message
        )
        
        logger.info("Starting to consume rating and shelf events...")
        
        while not self.should_stop:
            try:
                self.connection.process_data_events(time_limit=1)
            except Exception as e:
                logger.error(f"Error in consumer loop: {e}")
                if not self.should_stop:
                    self.connect()
    
    def stop(self):
        """Stop consuming."""
        self.should_stop = True
        if self.connection and self.connection.is_open:
            self.connection.close()


def start_consumer_thread():
    """Start the consumer in a background thread."""
    consumer = RatingEventConsumer()
    
    def run():
        try:
            consumer.start_consuming()
        except Exception as e:
            logger.error(f"Consumer thread error: {e}")
    
    thread = threading.Thread(target=run, daemon=True)
    thread.start()
    return consumer
