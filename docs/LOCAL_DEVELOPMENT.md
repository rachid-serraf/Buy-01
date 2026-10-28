# Running Buy-01 Locally

This guide runs the databases and Kafka in Docker, while the Spring Boot services
(and optionally the frontend) run directly on your machine.

> **Important:** When a service runs on your host, every hostname in `.env` must be
> `localhost`. Container names such as `discovery-server`, `kafka` or `mongo-user`
> only resolve inside the Docker network.

---

## Prerequisites

- Docker and Docker Compose
- Java 21+
- Node.js 20+ (only for the frontend)
- `make` and `openssl`

---

## 1. Create the `.env` file and JWT keys

From the repository root:

```bash
make env
```

This copies `.env.example` to `.env` (if it does not exist yet) and generates the
RSA key pair used to sign JWTs.

Check the file and make sure the values point to `localhost` (see
[step 2](#2-point-env-to-localhost)).

---

## 2. Point `.env` to localhost

`.env.example` is written for Docker Compose and may contain container names.
When running the application locally, these values must be changed:

| Variable            | If it contains a container name | Use instead                     |
| ------------------- | ------------------------------- | ------------------------------- |
| `EUREKA_URL`        | `http://discovery-server:8761/eureka` | `http://localhost:8761/eureka` |
| `KAFKA_BROKER`      | `kafka:9092`                    | `localhost:9092`                |
| `USER_MONGO_HOST`   | `mongo-user`                    | `localhost`                     |
| `PRODUCT_MONGO_HOST`| `mongo-product`                 | `localhost`                     |
| `MEDIA_MONGO_HOST`  | `mongo-media`                   | `localhost`                     |

The ports stay the same (`27017`, `27018`, `27019`, `9092`, `8761`).

---

## 3. Export the environment

The environment must be available to every service you start. Load it into the
**current shell**:

```bash
set -a; source .env; set +a
```

Environment variables are **per shell**. You must run this command again in
**every new terminal** you open for a service.

---
## 4. Start the infrastructure

```bash
make infrastructure-up
```

This starts `mongo-user`, `mongo-product` `mongo-media` `kafka` and `discovery-server`.

---

## 5. Run the backend services

Each service runs in its own terminal, and **each new terminal needs the export
from step 3**. Open the terminal in the repository root, export, then start the
service:

```bash
# Run this first in EVERY new terminal
set -a; source .env; set +a

# 1. Discovery server (skip if started with make infrastructure-up)
cd backend/discovery-server && ./mvnw spring-boot:run
```

Repeat the same pattern for the remaining services (separate terminal each):

```bash
# Terminal 2: user service
set -a; source .env; set +a
cd backend/user-service && ./mvnw spring-boot:run

# Terminal 3: product service
set -a; source .env; set +a
cd backend/product-service && ./mvnw spring-boot:run

# Terminal 4: media service
set -a; source .env; set +a
cd backend/media-service && ./mvnw spring-boot:run

# Terminal 5: gateway (last)
set -a; source .env; set +a
cd backend/api-gateway && ./mvnw spring-boot:run
```

> If you skip the export in a new terminal, the services fall back to their
> defaults or fail with missing values (`JWT_PUBLIC_KEY`, etc.).

| Service          | URL                              |
| ---------------- | -------------------------------- |
| Discovery Server | http://localhost:8761            |
| API Gateway      | http://localhost:8080            |
| User Service     | http://localhost:8081/actuator/health |
| Product Service  | http://localhost:8082/actuator/health |
| Media Service    | http://localhost:8083/actuator/health |

---

## 6. Run the frontend

```bash
cd frontend
npm install
npm start
```

The frontend runs at http://localhost:4200.

---

## Running everything in Docker instead

To run the whole stack (including all services and the frontend) in containers:

```bash
make docker-up
```

In that mode the container hostnames from `.env.example` are correct, because the
services talk to each other inside the `buy01-ecommerce-network` network.

---

## Troubleshooting

### `UnknownHostException`, `Connection refused`, or Mongo/Kafka timeouts

**Symptom:** a service fails to start with an error such as:

```
UnknownHostException: discovery-server
MongoTimeoutException: Timed out while waiting for a server
No resolvable bootstrap urls
```

**Cause:** `.env` was generated from `.env.example` and still uses Docker container
names. Those names only exist inside the Docker network. A service running on your
host must use `localhost`.

**Fix:** update `.env` and re-export it:

```bash
sed -i 's|^EUREKA_URL=.*|EUREKA_URL=http://localhost:8761/eureka|' .env
sed -i 's|^KAFKA_BROKER=.*|KAFKA_BROKER=localhost:9092|' .env
sed -i 's|^USER_MONGO_HOST=.*|USER_MONGO_HOST=localhost|' .env
sed -i 's|^PRODUCT_MONGO_HOST=.*|PRODUCT_MONGO_HOST=localhost|' .env
sed -i 's|^MEDIA_MONGO_HOST=.*|MEDIA_MONGO_HOST=localhost|' .env

set -a; source .env; set +a
```

Then restart the services.

> Do **not** change the hostnames inside the `environment:` blocks of
> `docker-compose.yml`. Those are intentional: containers use container names to
> reach each other. Only `.env` needs `localhost` for local runs.

### Port already in use

Another container or service is already bound to the port. Stop it or change the
matching variable in `.env`:

```bash
docker ps
make docker-down
```
