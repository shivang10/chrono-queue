# Docker Setup for ChronoQueue

## Prerequisites
- Docker Engine 20.10+
- Docker Compose 1.29+

## Services Overview
The complete ChronoQueue system consists of:
- **Zookeeper**: Kafka coordination service
- **Kafka**: Message broker
- **Job Producer**: Produces jobs to Kafka topics (Port 8080)
- **Job Worker**: Consumes and processes jobs (Port 8081)
- **Retry Dispatcher**: Handles job retries (Port 8082)

## Quick Start

### Build and Start All Services
```bash
docker-compose up --build
```

### Start Services in Background
```bash
docker-compose up -d --build
```

### Stop All Services
```bash
docker-compose down
```

### Stop and Remove Volumes
```bash
docker-compose down -v
```

## Service Endpoints
- Job Producer: http://localhost:8080
- Job Worker: http://localhost:8081
- Retry Dispatcher: http://localhost:8082
- Kafka: localhost:9092

## View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f job-producer
docker-compose logs -f job-worker
docker-compose logs -f retry-dispatcher
```

## Rebuild Single Service
```bash
docker-compose up -d --build job-producer
```

## Troubleshooting

### Kafka Connection Issues
If services can't connect to Kafka, ensure Kafka is healthy:
```bash
docker-compose ps
```

### Reset Everything
```bash
docker-compose down -v
docker-compose up --build
```

## Environment Variables
You can set environment variables in a `.env` file:
```
SPRING_PROFILES_ACTIVE=production
```
