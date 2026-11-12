# ==============================
# Configuration
# ==============================

BACKEND_DIR := backend

SERVICES := \
	api-gateway \
	discovery-server \
	media-service \
	product-service \
	user-service \
	order-service

MVN_CMD := ./mvnw

# ==============================
# Phony Targets
# ==============================

.PHONY: help dev logs build clean install rebuild docker-up docker-up-dbs infrastructure-up docker-down docker-build env ssl

# Default target
help:
	@echo "Available targets:"
	@echo "  make dev              -> start infra (Kafka, Mongo, Eureka) & run 5 local services"
	@echo "  make logs s=<service> -> tail live logs (e.g., make logs s=api-gateway)"
	@echo "  make env              -> generate RSA keys for JWT signing in .env"
	@echo "  make ssl              -> generate SSL certificates for secure communication"
	@echo "  make install          -> clean install all backend services"
	@echo "  make clean            -> clean all backend services"
	@echo "  make rebuild          -> clean install all services"
	@echo "  make infrastructure-up-> start kafka, mongo dbs, and discovery-server"
	@echo "  make docker-build     -> build docker images"
	@echo "  make docker-up        -> start docker-compose"
	@echo "  make docker-up-dbs    -> start docker-compose databases only"
	@echo "  make docker-down      -> stop docker-compose"

# ==============================
# Local Development Targets
# ==============================

dev: infrastructure-up
	@bash ./scripts/run.sh

logs:
	@if [ -z "$(s)" ]; then \
		echo "Usage: make logs s=<service-name>"; \
		echo "Available logs:"; \
		ls -1 /tmp/buy01-logs 2>/dev/null | sed 's/\.log//' || echo "  No active logs found (is 'make dev' running?)"; \
	else \
		tail -f /tmp/buy01-logs/$(s).log; \
	fi

# ==============================
# Maven Targets
# ==============================

install: ssl
	@for service in $(SERVICES); do \
		echo "Building $$service..."; \
		cd $(BACKEND_DIR)/$$service && $(MVN_CMD) clean install -DskipTests && cd - > /dev/null; \
	done

clean:
	@for service in $(SERVICES); do \
		echo "Cleaning $$service..."; \
		cd $(BACKEND_DIR)/$$service && $(MVN_CMD) clean && cd - > /dev/null; \
	done

rebuild: clean install

# ==============================
# Docker Targets
# ==============================

docker-build: ssl
	docker compose build

docker-up:
	docker compose up -d

docker-up-dbs:
	docker compose up -d mongo-user mongo-product mongo-media mongo-order

infrastructure-up:
	docker compose up -d kafka mongo-user mongo-product mongo-media discovery-server mongo-order

docker-down:
	docker compose down

# ==============================
# Key Generation
# ==============================
ssl:
	@echo "Checking SSL certificates..."
	@bash ./scripts/setup-ssl.sh

env:
	@echo "Generating RSA keys..."
	@./scripts/generate-keys.sh
