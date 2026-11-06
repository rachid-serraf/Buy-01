# Run Buy-01 locally — step by step

A beginner's guide to running the whole project on your machine, from zero.

Every command in this file was run and checked on Linux (Ubuntu, zsh) on
2026-11-03. The "You should see" parts are real outputs from that run.

---

## 1. What is this project?

**Buy-01** is a small online marketplace. **Sellers** create an account, add
products with photos, and manage them. **Clients** create an account and browse
and search those products.

It is built as **microservices**: several small Spring Boot apps (users,
products, media), a gateway in front of them, an Angular website, MongoDB
databases and Kafka for events.

---

## 2. Architecture

```
                         Your browser
                              │
          https://localhost:4200  (Angular dev server, `npm start`)
                              │
                              ▼  HTTPS
              ┌──────────────────────────────────┐
              │   API Gateway  :8080  (HTTPS)    │  checks the JWT token,
              │   Spring Cloud Gateway           │  rate limit, CORS
              └───┬──────────────┬───────────┬───┘
       /users/**  │  /products/**│           │ /media/**
                  ▼              ▼           ▼
          ┌────────────┐ ┌──────────────┐ ┌─────────────┐
          │user-service│ │product-svc   │ │media-service│
          │   :8081    │ │   :8082      │ │   :8083     │
          └──┬─────┬───┘ └──────┬───────┘ └──────┬──────┘
             │     └── REST (Feign) ─────────────►│
             ▼                  ▼                 ▼
        mongo-user        mongo-product      mongo-media
          :27017             :27019            :27018
             │                  │                 │
             └──────────►  Kafka :9092  ◄─────────┘
             events: user-deleted, images-linked, product-deleted

   Discovery Server (Eureka) :8761 — every service registers here, and the
   gateway uses it to find them.
```

| Service | Port (on your PC) | Database | Role |
|---|---|---|---|
| frontend (dev server) | 4200 (HTTPS) | – | The website you use (Angular 20) |
| frontend (Docker/Nginx) | 8085 | – | Production build of the website — see [problem 8.6](#86-the-site-on-port-8085-shows-failed-to-load-products) |
| api-gateway | 8080 (HTTPS) | – | Single entry point; routes `/users`, `/products`, `/media` |
| discovery-server | 8761 | – | Eureka: list of running services |
| user-service | 8081 | mongo-user `:27017`, db `user_db` | Register, login (JWT), profile, avatar |
| product-service | 8082 | mongo-product `:27019`, db `mongo-product` | Product CRUD, list, search |
| media-service | 8083 | mongo-media `:27018`, db `mongo-media` | Upload and serve product images |
| kafka | 9092 | – | Events between services |

---

## 3. Prerequisites

Install these, then run each check command.

| Tool | Version used | Needed for | Check command |
|---|---|---|---|
| Docker Engine | 29.9 | running everything | `docker --version` |
| Docker Compose v2 | v5.6 | `docker compose …` | `docker compose version` |
| Node.js | 22.23 (20.19+ works) | the website | `node -v` |
| npm | 10.9 | the website | `npm -v` |
| OpenSSL | 3.5 | generating keys | `openssl version` |
| make | 4.4 | shortcuts in `Makefile` | `make --version` |
| Java JDK | 21 | *optional* — only to build the backend outside Docker | `java -version` |
| Maven | 3.9 | *optional* — the project also ships `./mvnw` | `mvn -v` |

Check that Docker works **without** `sudo`:

```bash
docker run --rm hello-world
```

You should see: `Hello from Docker!`

---

## 4. Step-by-step run guide

Open a terminal **in the project root** (the folder that contains
`docker-compose.yml`).

### Step 1 — Create the `.env` file and the JWT keys

```bash
make env
```

**What it does:** copies `.env.example` to `.env` and writes a fresh RSA key pair
into it (`JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY`). The services need these keys to
create and check login tokens.

**You should see:**

```
.env file not found. Copying from .env.example...
✓ RSA key pair generated and added to .env file successfully!
  - Private key (base64): 1624 characters
  - Public key (base64): 392 characters
```

### Step 2 — Give the Docker project a name

```bash
echo 'COMPOSE_PROJECT_NAME=buy01' >> .env
```

**What it does:** names the Docker project `buy01`. Without it Docker uses the
folder name (`buy-01`), which clashes with any other copy of this project
started from a folder with the same name (a Jenkins copy, for example). See
[problem 8.4](#84-another-copy-of-the-project-is-already-running).

**Check the configuration:**

```bash
docker compose config --quiet && echo "compose config OK"
```

**You should see:** `compose config OK`

### Step 3 — Build the Docker images

```bash
docker compose build
```

**What it does:** compiles the 5 Java services and the Angular site **inside
Docker** (you do not need Java or Maven for this) and creates 6 images.

**You should see:** the command ends without `ERROR`. Then:

```bash
docker images | grep -E 'buy01-|service-image|frontend-image'
```

shows `buy01-api-gateway`, `buy01-discovery-server`, `user-service-image`,
`product-service-image`, `media-service-image`, `frontend-image`.

> ⏱ The **first** build downloads all Maven and npm dependencies. It can take
> 10–50 minutes on a slow connection. Later builds reuse the cache (~3 min).
> If it stops with `Read timed out`, just run it again — see
> [problem 8.7](#87-build-fails-with-read-timed-out-or-a-fonts-error).

<details>
<summary>Optional: build the backend with Maven on your machine instead</summary>

Only needed if you want to run the services outside Docker
(see `docs/LOCAL_DEVELOPMENT.md`). Needs Java 21.

```bash
SSL_PASSWORD=123456 make install
```

You should see `BUILD SUCCESS` five times, and five jars in
`backend/*/target/*.jar`. Always pass `SSL_PASSWORD=123456`: `make` does not read
`.env`, and without it the gateway certificate is created with an empty password.

</details>

### Step 4 — Start everything

```bash
docker compose up -d
```

**What it does:** starts 10 containers in the background, in the right order:
databases and Kafka → Eureka → the 3 services → gateway → Nginx frontend.

**You should see:** a list of `Container … Started` lines. Wait **about 30
seconds**, then:

```bash
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
```

(Plain `docker compose ps` works too; it just prints more columns.)

**You should see** all 10 containers `Up`, and 8 of them `(healthy)`
(`api-gateway` and `kafka` have no health check, they only show `Up`):

```
NAME               STATUS
api-gateway        Up 35 seconds
discovery-server   Up 46 seconds (healthy)
frontend           Up 35 seconds (healthy)
kafka              Up 46 seconds
media-service      Up 36 seconds (healthy)
mongo-media        Up 47 seconds (healthy)
mongo-product      Up 46 seconds (healthy)
mongo-user         Up 46 seconds (healthy)
product-service    Up 35 seconds (healthy)
user-service       Up 35 seconds (healthy)
```

If some still say `(health: starting)`, wait 15 seconds and run it again.

### Step 5 — Check that the backend is healthy

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl -k https://localhost:8080/actuator/health
```

**What it does:** asks each service "are you OK?". `-k` tells curl to accept the
gateway's self-signed HTTPS certificate.

**You should see:** each answer starts with `{"status":"UP"`.

Then open **http://localhost:8761** in your browser (Eureka dashboard).
**You should see** under *Instances currently registered*: `GATEWAY`,
`MEDIA-SERVICE`, `PRODUCT-SERVICE`, `USER-SERVICE`. (They appear about 15–30
seconds after start.)

Optional — check that Kafka created its topics:

```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

**You should see:** `images-linked`, `product-deleted`, `user-deleted`.

### Step 6 — Install and start the website

```bash
cd frontend
npm install
npm start
```

**What it does:** downloads the website's libraries (~30 s), then starts the
Angular dev server with HTTPS. Leave this terminal open.

**You should see:**

```
  ➜  Local:   https://localhost:4200/
```

### Step 7 — Trust the two local certificates (once per browser)

Both the website (4200) and the gateway (8080) use **self-signed** certificates,
so your browser blocks them until you allow them:

1. Open **https://localhost:8080/actuator/health** → click *Advanced* →
   *Proceed to localhost*. You should see `{"status":"UP",...}`.
2. Open **https://localhost:4200** → *Advanced* → *Proceed to localhost*.

**You should see:** the Buy-01 shop page. If you skip point 1, the page says
**"Failed to load products"** (the browser silently refuses to call the gateway).

### Step 8 — Test the main features with curl (optional)

Open a **new terminal** in the project root and paste this block. It registers a
seller and a client, logs in, uploads an image, creates a product and reads it
back.

```bash
# 1. Register a seller (with an avatar) and a client
curl -k -X POST https://localhost:8080/users/auth/register \
  -F name="Sara Seller" -F email=sara@example.com -F password=secret123 \
  -F role=SELLER -F avatar=@frontend/public/assets/placeholder.png
echo
curl -k -X POST https://localhost:8080/users/auth/register \
  -F name="Carl Client" -F email=carl@example.com -F password=secret123 -F role=CLIENT
echo

# 2. Log in and keep the token in a variable
TOKEN=$(curl -sk -X POST https://localhost:8080/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sara@example.com","password":"secret123"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
echo "TOKEN length: ${#TOKEN}"

# 3. Who am I?
curl -sk https://localhost:8080/users/me -H "Authorization: Bearer $TOKEN"; echo

# 4. Upload an image and keep its id
IMAGE_ID=$(curl -sk -X POST https://localhost:8080/media -H "Authorization: Bearer $TOKEN" \
  -F files=@frontend/public/assets/placeholder.png \
  | sed -E 's/.*"imagesId":"([^"]+)".*/\1/')
echo "IMAGE_ID=$IMAGE_ID"

# 5. Create a product that uses this image
curl -sk -X POST https://localhost:8080/products -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Desk Lamp\",\"description\":\"A warm LED desk lamp with three brightness levels.\",\"category\":\"HOME\",\"price\":39.90,\"quantity\":10,\"imagesIds\":[\"$IMAGE_ID\"]}" \
  -w ' [%{http_code}]\n'

# 6. List products (public, no token needed) and download the image
curl -sk https://localhost:8080/products; echo
curl -sk -o /dev/null -w 'image: %{http_code} %{content_type}\n' https://localhost:8080/media/$IMAGE_ID
```

**You should see (shortened):**

| Request | Answer |
|---|---|
| register seller | `{"id":"…","name":"Sara Seller",…,"role":"SELLER"}` |
| register client | `{"id":"…","name":"Carl Client",…,"role":"CLIENT"}` |
| login | `TOKEN length: 528` |
| /users/me | `{"id":"…","name":"Sara Seller","email":"sara@example.com",…}` |
| upload | `IMAGE_ID=6ae9…` (24 characters) |
| create product | `{"id":"…","name":"Desk Lamp",…,"price":39.90,…} [201]` |
| list products | `[{"id":"…","name":"Desk Lamp","price":39.90,"image":"6ae9…","category":"HOME",…}]` |
| image | `image: 200 image/png` |

Security checks you can also try (same terminal):

```bash
# no token -> 401
curl -sk -o /dev/null -w '%{http_code}\n' -X POST https://localhost:8080/products -H "Content-Type: application/json" -d '{}'
# wrong password -> 401 "Invalid email or password"
curl -sk -X POST https://localhost:8080/users/auth/login -H "Content-Type: application/json" -d '{"email":"sara@example.com","password":"wrongpass1"}'; echo
```

---

## 5. How to use the app

Open **https://localhost:4200** (after [step 7](#step-7--trust-the-two-local-certificates-once-per-browser)).

1. **Create an account** — click **Sign up**. Choose **Sell** (seller) or
   **Shop** (client), enter a name (3–50 characters), an email and a password
   (6–30 characters). A profile photo is optional (JPG/PNG/GIF/WebP, max 2 MB).
   Click **Create account** — you are sent to the login page.
2. **Log in** — enter the same email and password, click **Sign in**. Your avatar
   appears at the top right.
3. **Add a product** (sellers only) — click **New product** in the top bar. Fill
   in name (3+ characters), description (20+ characters), category, price and
   quantity.
4. **Upload an image** — in the same form, drop an image on the upload area or
   click it to choose a file (max 2 MB). Use **Add New** for extra images. Click
   **Create Product** — you land on the product page.
5. **See your products** — click **Dashboard** (top bar) to see only your
   listings. Click a product to open it; on your own product the **⋮** menu has
   **edit** and **delete**.
6. **Profile** — avatar menu → **Profile**: change your name or photo, or delete
   your account.
7. **Log out** — avatar menu → **Log out**.

> Use the **New product** button to open the form. Refreshing the browser *on*
> the form page sends you back home — see [problem 8.9](#89-refreshing-productsnew-sends-me-back-to-the-home-page).

---

## 6. Look inside the databases

Each service has its own MongoDB container. Open a Mongo shell with
`docker exec -it <container> mongosh …`:

```bash
# users
docker exec -it mongo-user mongosh -u user_service -p user_password --authenticationDatabase admin user_db
# products
docker exec -it mongo-product mongosh -u rserraf -p 123456 --authenticationDatabase admin mongo-product
# media (image records)
docker exec -it mongo-media mongosh -u rserraf -p 123456 --authenticationDatabase admin mongo-media
```

Useful commands inside `mongosh`:

```js
show collections                                      // users / products / medias
db.users.find({}, {name:1, email:1, role:1, _id:0})   // in user_db
db.products.find({}, {name:1, price:1, category:1})   // in mongo-product
db.medias.find({}, {image_path:1, product_id:1, file_status:1})  // in mongo-media
db.products.countDocuments()
exit
```

One-line versions (no interactive shell):

```bash
docker exec mongo-user mongosh -u user_service -p user_password --authenticationDatabase admin user_db --quiet --eval 'db.users.find({}, {name:1, email:1, role:1, _id:0}).toArray()'
docker exec mongo-product mongosh -u rserraf -p 123456 --authenticationDatabase admin mongo-product --quiet --eval 'db.products.find({}, {name:1, price:1, category:1, _id:0}).toArray()'
```

You should see your users (`Sara Seller`, …) and products (`Desk Lamp`, …). In
`medias`, `file_status: 'LINKED'` means Kafka delivered the *images-linked*
event from product-service to media-service.

Logs of a service (add `-f` to follow live, Ctrl+C to quit):

```bash
docker compose logs --tail 50 user-service
```

---

## 7. Stop everything and reset

| I want to… | Command |
|---|---|
| Stop the website | press **Ctrl+C** in the `npm start` terminal |
| Stop the containers, **keep** the data | `docker compose stop` (start again: `docker compose start`) |
| Remove the containers, **keep** the data | `docker compose down` |
| Remove containers **and delete all data** (users, products) | `docker compose down -v` |
| Also delete the built images (next start rebuilds) | `docker compose down -v --rmi local` |

`down -v` deletes the MongoDB and Kafka volumes, so the next `up` starts with
empty databases.

Uploaded **avatars** are saved on your disk in `./uploads/avatars`. Docker
created that folder as `root`, so to empty it use a throw-away container
instead of `sudo`:

```bash
docker run --rm -v "$PWD/uploads:/u" alpine sh -c 'rm -rf /u/avatars/* /u/media/*'
```

---

## 8. Common problems and fixes

### 8.1 Port 4200 is busy

```
Port 4200 is already in use.
Would you like to use a different port? (Y/n)
```

Something else (another `npm start`, or a container) already uses 4200.
Find it with `docker ps` / `ss -ltnp | grep 4200` and stop it. **Do not** answer
`Y`: the gateway only accepts the website from port **4200** (CORS), so on any
other port every API call fails with **403 Forbidden**.

### 8.2 "Port is already allocated" when starting containers

```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

Another program or container already uses that port (8080, 8761, 27017, …).
Run `docker ps` to find it and stop it, or change the port in `.env`
(`GATEWAY_PORT`, `DISCOVERY_PORT`, `MEDIA_MONGO_LOCAL_PORT`, …).
Note: the website (`npm start`) always calls `https://localhost:8080`, so keep
the gateway on 8080 when you use it.

### 8.3 The page says "Failed to load products"

The website cannot reach the gateway. Check, in this order:

1. Is the backend running? `docker compose ps` — `api-gateway` must be `Up`.
   Start it with `docker compose up -d`. After a restart it needs ~10–30 s.
2. Did you trust the gateway certificate? Open
   https://localhost:8080/actuator/health once and accept it ([step 7](#step-7--trust-the-two-local-certificates-once-per-browser)).
3. Is the website on port 4200? (see 8.1)

Then click **Try again**.

### 8.4 Another copy of the project is already running

If a second copy of Buy-01 (for example deployed by Jenkins) is running, it
holds ports 8080, 8761 and 4200 and you get the errors from 8.1/8.2. Stop it
without deleting it, then start it again later:

```bash
docker ps --format '{{.Names}}'          # find its containers
docker stop <names…>                    # free the ports
docker start <names…>                   # later, to bring it back
```

[Step 2](#step-2--give-the-docker-project-a-name) (`COMPOSE_PROJECT_NAME=buy01`)
keeps your copy separate from it, so `docker compose down` never touches the
other copy.

### 8.5 `NET::ERR_CERT_AUTHORITY_INVALID` / "Your connection is not private"

Normal for local HTTPS with self-signed certificates. Follow
[step 7](#step-7--trust-the-two-local-certificates-once-per-browser). With curl,
add `-k`.

### 8.6 The site on port 8085 shows "Failed to load products"

**Known bug — not fixed yet.** The Docker frontend (Nginx, port 8085) sends API
calls to `/api/products`, but the gateway only knows `/products`, so it answers
**404**. Use the dev server on **https://localhost:4200** ([step 6](#step-6--install-and-start-the-website)).
Proposed fix (one line in `frontend/nginx.conf`, inside `location /api/`):
`rewrite ^/api/(.*)$ /$1 break;`

### 8.7 Build fails with `Read timed out` or a fonts error

```
Could not transfer artifact … from/to central (https://repo.maven.apache.org/maven2): Read timed out
```

or, for the frontend, an error in `inline-fonts.js` while downloading Google
Fonts. Both are **network** problems during the download, not code problems.
Run `docker compose build` again: finished steps are cached, so it continues
where it stopped.

### 8.8 Search and filters always show "No matching products"

**Known bug — not fixed yet.** `GET /products/search` returns 0 results for
every query, even `?sort=price_asc`. Cause in
`product-service/…/MongoProductAdapter.search()`:

1. it queries with the `Product` class (collection `product`) instead of
   `ProductDocument` (collection `products`);
2. the category filter lower-cases the value but categories are stored upper
   case (`HOME`);
3. `price` is stored as a **string** (`'39.90'`), so min/max price compare text.

The normal product list (`GET /products`) works.

### 8.9 Refreshing `/products/new` sends me back to the home page

**Known bug — not fixed yet.** After a full page reload the app loads your user
in the background, but the seller-only route check runs first, sees no role yet,
and redirects to the home page. Open the form with the **New product** button
instead of refreshing it.

### 8.10 Product images disappear after `docker compose down`

**Known bug — not fixed yet.** media-service saves images in `/app/uploads/`
inside its container, but `docker-compose.yml` mounts the disk folder at
`/app/uploads/media`. When the container is recreated (`down`, `up --build`,
`--force-recreate`) the files are lost and images answer
`404 "Image file not found on disk"`. Avatars are not affected.
Fix: mount `./uploads/media:/app/uploads` in `docker-compose.yml`.

### 8.11 `permission denied … /var/run/docker.sock`

*(Not reproduced on the test machine — standard Docker fix.)* Your user is not
allowed to use Docker. Add it to the `docker` group, then log out and back in:

```bash
sudo usermod -aG docker $USER
```

Check with `docker run --rm hello-world`.

### 8.12 Wrong Java version

*(Not reproduced on the test machine.)* Only matters for the optional Maven
build: the services need **Java 21** (`discovery-server` needs 17+). If
`java -version` shows an older version, install a JDK 21 and select it (on
Ubuntu: `sudo update-alternatives --config java`). The Docker build does not use
your local Java at all.

### 8.13 Many `variable is not set` warnings, then `container_name '' does not match`

```
level=warning msg="The \"USER_MONGO_USERNAME\" variable is not set. Defaulting to a blank string."
...
services.mongo-media.container_name '' does not match pattern '[a-zA-Z0-9][a-zA-Z0-9_.-]+'
```

There is no `.env` file, or you are not in the project root. Run
`docker compose` commands from the folder that contains `docker-compose.yml`,
and create `.env` with [step 1](#step-1--create-the-env-file-and-the-jwt-keys).

### 8.14 Login says "Password must be between 6 and 30 characters"

The backend validates the password **before** checking it. A too-short password
gives `400 Bad Request` with that message, not "invalid password".

---

## 9. Quick start (≈1 minute once images are built)

From the project root:

```bash
make env
echo 'COMPOSE_PROJECT_NAME=buy01' >> .env
docker compose up -d --build
cd frontend && npm install && npm start
```

Then open https://localhost:8080/actuator/health (accept the certificate), and
**https://localhost:4200** (accept the certificate). Stop with **Ctrl+C** and
`docker compose down`.

> Only run `make env` the **first** time — running it again creates new keys,
> and existing login tokens stop working.
