# Delayed Job & Retry Queue Using Kafka

Kafka does NOT support delayed messages natively (unlike SQS, RabbitMQ with TTL, etc.).

Yet:

Almost every real system needs delayed retries

Payment retries, webhook retries, email retries, job scheduling, etc.

👉 This project teaches how to build delay & retry semantics on top of Kafka
