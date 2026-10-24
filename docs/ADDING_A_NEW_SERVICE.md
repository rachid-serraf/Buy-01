# Adding a New Service to the Jenkins Pipeline

This guide explains how to add a new microservice to the Buy-01 project so it is automatically picked up by the data-driven Jenkins pipeline.

## Overview

The pipeline uses a **service registry** (defined in the `Detect Changes` stage) as a single source of truth. Most stages (Tests, SonarQube, Build, Smoke Test, Deploy detection, and Ordering) iterate over this registry. 

However, there are still a few **explicit places** that need manual updates due to shell cases, environment variable exports, and cleanup lists.

## Step-by-Step Guide

### 1) Create the service in the repo

1. Create the service folder under `backend/your-service/` (or `frontend/your-service/` if it's a frontend app).
2. Add a `Dockerfile` (or `dockerfile` if lowercase; the build logic handles `discovery-server` specially, otherwise it looks for `Dockerfile`).
3. Ensure the service can be built with Maven (`./mvnw clean test`) or Node (`npm ci && npm run test:ci`) depending on its type.

### 2) Add to docker-compose.yml

Add the service definition with a `container_name` that **exactly matches** the `id` you will use in the registry.

Example (Maven service):

```yaml
  orders-service:
    container_name: orders-service
    build:
      context: ./backend/orders-service
      dockerfile: Dockerfile
    image: ${ORDERS_SERVICE_IMAGE:-orders-service-image:latest}
    restart: unless-stopped
    depends_on:
      discovery-server:
        condition: service_started
      mongo-orders:
        condition: service_healthy
    environment:
      - TZ=${TZ}
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
      - EUREKA_URL=http://discovery-server:8761/eureka
    networks:
      - ecommerce-network
    # healthcheck: optional but recommended for the Health Check stage
```

If you want the pipeline's Health Check stage to validate it, add a `healthcheck` to the compose service.

### 3) Add to the Jenkins pipeline

#### A. Add image environment variable
In the `environment {}` block at the top of `Jenkinsfile`, add:

```groovy
ORDERS_SERVICE_IMAGE = "orders-service-image:${BUILD_NUMBER}"
```

#### B. Add to the service registry
In the `Detect Changes` stage, find the `services` array and append your entry:

```groovy
def services = [
    [id: 'discovery-server', dir: 'backend/discovery-server', type: 'maven', imageRepo: 'discovery-server-image', sonarKey: 'discovery-server', sonarName: 'Discovery Server'],
    [id: 'api-gateway',      dir: 'backend/api-gateway',      type: 'maven', imageRepo: 'api-gateway-image',      sonarKey: 'api-gateway',      sonarName: 'API Gateway'],
    [id: 'user-service',     dir: 'backend/user-service',     type: 'maven', imageRepo: 'user-service-image',     sonarKey: 'user-service',     sonarName: 'User Service'],
    [id: 'product-service',  dir: 'backend/product-service',  type: 'maven', imageRepo: 'product-service-image',  sonarKey: 'product-service',  sonarName: 'Product Service'],
    [id: 'media-service',    dir: 'backend/media-service',    type: 'maven', imageRepo: 'media-service-image',    sonarKey: 'media-service',    sonarName: 'Media Service'],
    [id: 'orders-service',   dir: 'backend/orders-service',   type: 'maven', imageRepo: 'orders-service-image',   sonarKey: 'orders-service',   sonarName: 'Orders Service'],
    [id: 'frontend',         dir: 'frontend',                 type: 'node',  imageRepo: 'frontend-image',         sonarKey: 'frontend',         sonarName: 'Frontend']
]
```

Field descriptions:
| Field | Required | Notes |
| --- | --- | --- |
| `id` | Yes | Must match `container_name` in `docker-compose.yml`. Used to generate change flags and for ordering. |
| `dir` | Yes | Path from repo root where the source and Dockerfile are located. Changes under this directory trigger builds/tests. |
| `type` | Yes | `maven` (runs `./mvnw clean test`, uses Java Sonar args, runs Java smoke test) or `node` (runs `npm ci && npm run test:ci`, uses Node Sonar args, skipped in smoke tests). |
| `imageRepo` | Yes | The repository name used for tagging `:latest` and for cleanup. |
| `sonarKey` | Yes | Project key used in SonarQube analysis. |
| `sonarName` | Yes | Display name used in SonarQQube analysis. |

#### C. Update Deploy (exports + rollback cases)
In the `Deploy` stage's shell script (`steps { withCredentials { script { ... } sh ''' ... ''' } }`), add your image export:

```bash
export DISCOVERY_SERVER_IMAGE="${DISCOVERY_SERVER_IMAGE}"
export API_GATEWAY_IMAGE="${API_GATEWAY_IMAGE}"
export PRODUCT_SERVICE_IMAGE="${PRODUCT_SERVICE_IMAGE}"
export USER_SERVICE_IMAGE="${USER_SERVICE_IMAGE}"
export MEDIA_SERVICE_IMAGE="${MEDIA_SERVICE_IMAGE}"
export ORDERS_SERVICE_IMAGE="${ORDERS_SERVICE_IMAGE}"
export FRONTEND_IMAGE="${FRONTEND_IMAGE}"
```

In the `Deploy` post-failure rollback, add a case:

```bash
case $SVC in
    discovery-server)
        DISCOVERY_SERVER_IMAGE="$PREV_IMAGE"
        ;;
    api-gateway)
        API_GATEWAY_IMAGE="$PREV_IMAGE"
        ;;
    product-service)
        PRODUCT_SERVICE_IMAGE="$PREV_IMAGE"
        ;;
    user-service)
        USER_SERVICE_IMAGE="$PREV_IMAGE"
        ;;
    media-service)
        MEDIA_SERVICE_IMAGE="$PREV_IMAGE"
        ;;
    orders-service)
        ORDERS_SERVICE_IMAGE="$PREV_IMAGE"
        ;;
    frontend)
        FRONTEND_IMAGE="$PREV DET_IMAGE        ;;
esac"
```

#### D. Update Health Check (rollback cases)
In the `Health Check` post-failure rollback, add the same case for your service.

#### E. Update Cleanup Old Images
In the `Cleanup Old Images` stage's shell script, add your `imageRepo` to both loops:

```bash
for repo in discovery-server-image api-gateway-image product-service-image user-service-image media-service-image orders-service-image frontend-image; do
    ...
done
```

Also update the `grep -E` filter at the end to include it:

```bash
echo "Remaining images:"
docker images | grep -E 'discovery-server-image|api-gateway-image|product-service-image|user-service-image|media-service-image|orders-service-image|frontend-image' || true
```

#### F. Update pipeline post (always)
In the `post { always { sh ''' ... ''' } }` section, add your service to the image listing loop and grep:

```bash
for repo in discovery-server-image api-gateway-image product-service-image user-service-image media-service-image orders-service-image frontend-image; do
    docker image ls "$repo" \
        --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}" || true
done
```

### 4) Configure startup order (if needed)

If your new service depends on certain infra/services, update the `order` map in the `Deploy` stage:

```groovy
def order = ['discovery-server': 0, 'api-gateway': 1, 'user-service': 2, 'product-service': 2, 'media-service': 2, 'orders-service': 2, 'frontend': 3]
toDeploy = toDeploy.sort { a, b ->
    (order[a] ?: 99) <=> (order[b] ?: 99)
}
```

- Discovery should be first (0)
- Gateway second (1)
- Core/backend services (2)
- Frontend last (3)
- Unknown services fall back to `99` (will come after everything else)

### 5) Verify

1. Commit and push your changes (including the new service folder, `docker-compose.yml`, and updated `Jenkinsfile`).
2. Create a PR or push to a branch. The pipeline will:
   - Detect changes to `backend/orders-service/` automatically
   - Run tests and Sonar if changed or if infrastructure changed
   - Build the image on `main` branch
   - Deploy only if changed and on `main` without PR
   - Health check it (if it has a healthcheck)
   - Clean up old image tags
3. If it's a new service with no previous image, rollback won't restore anything useful, but the logic still works.

## Quick Checklist

- [ ] Service folder + `Dockerfile` created
- [-image] `container_name` matches registry `id`
- [ ] Added to `docker-compose.yml`
- [ ] Added `*_IMAGE` env var in pipeline environment
- [ ] Added entry to `services` registry in `Detect Changes`
- [ ] Added exports/cases to Deploy (shell + rollback)
- [ ] Added rollback cases to Health Check
- [ ] Added to Cleanup Old Images lists
- [ ] Added to `post { always {} }` image lists
- [ ] Updated startup order map if needed
- [ ] Commit/push and test pipeline
