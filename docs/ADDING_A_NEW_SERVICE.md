# Adding a New Service to the Pipeline

The `Jenkinsfile` is driven by one service registry, `servicesList()`. Tests, Sonar, image builds, smoke tests, deploy, rollback and cleanup are all generated from it. Adding a service takes **three changes**, and you never edit the pipeline stages.

| # | File | What to do |
|---|------|------------|
| 1 | Service folder | Add the code, a Dockerfile, and a `mvnw` (Maven) or `test:ci` script (Node) |
| 2 | `docker-compose.yml` | Add the service entry, following the naming rules below |
| 3 | `Jenkinsfile` | Add **one line** to `servicesList()` |

---

## 1. Prepare the service folder

**Maven (Spring Boot) service**

```
backend/order-service/
├── Dockerfile          (or "dockerfile", both are detected)
├── mvnw
├── pom.xml
└── src/
```

- `./mvnw clean test` must work from inside the folder.
- The Dockerfile must produce an image that contains `java` (the smoke test runs `java -version`).
- For coverage in Sonar, configure the JaCoCo plugin so that it writes `target/site/jacoco/jacoco.xml`.

**Node (frontend) service**

- `package.json` must have a `test:ci` script (run after `npm ci`).
- Smoke tests are skipped for `type: 'node'`.

---

## 2. Add it to `docker-compose.yml`

Three naming rules must hold. The pipeline derives names from the service `id`:

| Rule | Example for `order-service` |
|------|-----------------------------|
| Compose service name = `id` | `order-service:` |
| `container_name` = `id` | `container_name: order-service` |
| Image uses the variable `<ID_UPPER_SNAKE>_IMAGE` | `image: ${ORDER_SERVICE_IMAGE}` |

(`-` becomes `_` and the name is uppercased, so `order-service` becomes `ORDER_SERVICE_IMAGE`.)

```yaml
  order-service:
    image: ${ORDER_SERVICE_IMAGE}
    container_name: order-service
    env_file: .env
    depends_on:
      discovery-server:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8085/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
```

The `healthcheck` is optional. If it exists, the **Health Check** stage waits for `healthy` and rolls back on failure. If not, the stage skips the service. A healthcheck is recommended, because without one a broken deploy is not detected.

---

## 3. Register it in the `Jenkinsfile`

Add one line to `servicesList()`:

```groovy
def servicesList() {
    return [
        [id: 'discovery-server', dir: 'backend/discovery-server', type: 'maven', sonarName: 'Discovery Server'],
        [id: 'api-gateway',      dir: 'backend/api-gateway',      type: 'maven', sonarName: 'API Gateway'],
        [id: 'user-service',     dir: 'backend/user-service',     type: 'maven', sonarName: 'User Service'],
        [id: 'product-service',  dir: 'backend/product-service',  type: 'maven', sonarName: 'Product Service'],
        [id: 'media-service',    dir: 'backend/media-service',    type: 'maven', sonarName: 'Media Service'],
        [id: 'order-service',    dir: 'backend/order-service',    type: 'maven', sonarName: 'Order Service'],   // <- new
        [id: 'frontend',         dir: 'frontend',                 type: 'node',  sonarName: 'Frontend'],
    ]
}
```

| Field | Meaning |
|-------|---------|
| `id` | Unique name. Used for the container, Sonar project key, image repo (`<id>-image`) and image variable |
| `dir` | Path to the service folder. A change under `dir/` triggers this service |
| `type` | `maven` or `node`. Picks the test command, Sonar settings and smoke test |
| `sonarName` | Display name in SonarQube |

**List order = deploy order.** Services are deployed in the order they appear, so keep dependencies first (discovery-server, then api-gateway, then business services, then frontend). Do not use `sort`: the Jenkins sandbox blocks it.

---

## What you get automatically

| Stage | Behaviour for the new service |
|-------|-------------------------------|
| Detect Changes | Triggered when a file under `dir/` changes |
| Unit Tests | Runs in parallel with the other changed services |
| SonarQube | Project `id` is created on the first scan; the quality gate is enforced |
| Build Images | Builds `<id>-image:<BUILD_NUMBER>` and `:latest` (on `main`) |
| Smoke Test | `java -version` in the image (maven only) |
| Deploy | Stop, remove, then `docker compose up` with the new image; the previous image is saved |
| Health Check | Waits for `healthy` (if a healthcheck is defined) |
| Rollback | Restores the previous image if deploy or health check fails |
| Cleanup | Keeps the last 3 image tags for `<id>-image` |

If `docker-compose.yml`, `Jenkinsfile` or `.env` changes, **all** services are rebuilt and redeployed.

---

## Checklist

- [ ] Folder has a Dockerfile and `./mvnw clean test` (or `npm run test:ci`) works
- [ ] Compose service name and `container_name` equal the `id`
- [ ] Compose `image:` is `${<ID_UPPER_SNAKE>_IMAGE}`
- [ ] Healthcheck added in compose (recommended)
- [ ] One line added to `servicesList()` in the right order
- [ ] Opened a PR first: it runs tests and Sonar, but skips build and deploy
- [ ] Merge to `main` and watch the first deploy and health check

---

## Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| Service never builds or tests | `dir` is wrong, or the change was outside `dir/` |
| `No Dockerfile found for <id>` | Dockerfile is not directly inside `dir/` (accepted names: `Dockerfile`, `dockerfile`) |
| Compose says image variable is not set | Image variable name does not match `<ID_UPPER_SNAKE>_IMAGE` |
| `docker inspect <id>` fails in deploy or health check | `container_name` is not equal to `id` |
| Health check times out | Healthcheck command is wrong, or the app needs more than 30 retries x 10 s (`HEALTH_RETRIES`, `HEALTH_INTERVAL`) |
| Sonar quality gate fails | Fix the issues in SonarQube, or check the JaCoCo report path for coverage |
| `RejectedAccessException` in Jenkins | A non-whitelisted Groovy method was used in the Jenkinsfile. Prefer a plain `for` loop over `sort`, `groovy.json`, etc. |
| Service starts before its dependency | Add `depends_on` with `condition: service_healthy` in compose, and keep the order in `servicesList()` correct |