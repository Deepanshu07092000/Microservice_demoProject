# Spring Boot Microservices: Eureka + API Gateway + User & Order Services

| App | Port | Eureka name | Database |
|---|---|---|---|
| eureka-server | 8761 | - | - |
| api-gateway | 8080 | API-GATEWAY | - |
| user-service | 8081 | USER-SERVICE | `user_service_db` |
| order-service | 8082 | ORDER-SERVICE | `order_service_db` |
| notification-service | 8083 | NOTIFICATION-SERVICE | `notification_service_db` |
| audit-service | 8084 | AUDIT-SERVICE | `audit_service_db` |
| kafka (broker) | 9092 (PC) / 29092 (Docker network) | - | topic `order-events` |

**Versions:** Java 21, Spring Boot **3.4.5**, Spring Cloud **2024.0.1** (the release train that matches Boot 3.4.x).
If you change Boot, change Spring Cloud with it: https://spring.io/projects/spring-cloud#overview

## Architecture

```
                         +--------------------+
                         |   Eureka Server    |  8761  (registry / "phone book")
                         +--------------------+
                          ^   ^            ^   ^
              register    |   | lookup     |   | register
         +----------------+   |            |   +----------------+
         |                    |            |                    |
 Client -> API Gateway (8080) +            +-- Order Service (8082) --REST via USER-SERVICE--> User Service (8081)
 (Postman)   /api/users/**  -> lb://USER-SERVICE                |                                 |
             /api/orders/** -> lb://ORDER-SERVICE               v                                 v
                                                         order_service_db                   user_service_db
```

Request flow: `Postman -> Gateway(8080) -> asks Eureka where USER-SERVICE is -> forwards to 8081 -> response back`.
Eureka is only a registry; the **gateway** forwards the traffic.

## Prerequisites
- JDK 21, Maven 3.9+ (or let IntelliJ import each `pom.xml`), MySQL 8 running on localhost:3306
- Environment variables (set them in IntelliJ Run Configuration for user-service and order-service):

| Variable | Default |
|---|---|
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | *(empty)* - **set this to your MySQL password** |
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` |

Databases are created automatically (`createDatabaseIfNotExist=true`).

## Run order
1. `eureka-server` -> open http://localhost:8761
2. `user-service`
3. `order-service`
4. `api-gateway`

Each: `cd <folder> && mvn spring-boot:run` (or run the `*Application` class in IntelliJ).
Wait ~30 s; the Eureka dashboard should list **API-GATEWAY, USER-SERVICE, ORDER-SERVICE** as UP.

## Import into Postman
Import `postman/Microservices.postman_collection.json`. Run requests in order (create user first).

## API checklist (always through the gateway `http://localhost:8080`)

| Method | URL | Body | Success |
|---|---|---|---|
| POST | /api/users | `{"name":"Asha Rao","email":"asha@example.com","phoneNumber":"9876543210"}` | 201 |
| GET | /api/users | - | 200 |
| GET | /api/users/1 | - | 200 (404 if missing) |
| PUT | /api/users/1 | same as POST | 200 |
| DELETE | /api/users/1 | - | 204 |
| POST | /api/orders | `{"userId":1,"productName":"Laptop","quantity":2,"price":499.99}` | 201 (`totalAmount` = 999.98) |
| GET | /api/orders | - | 200 |
| GET | /api/orders/1 | - | 200 |
| GET | /api/orders/user/1 | - | 200 |
| PUT | /api/orders/1 | `{"userId":1,"productName":"Laptop","quantity":3,"price":499.99,"orderStatus":"CONFIRMED"}` | 200 |
| DELETE | /api/orders/1 | - | 204 |

Header for bodies: `Content-Type: application/json`.
Direct (development only): same paths on `:8081` (users) and `:8082` (orders). Clients should only use `:8080`.

Error examples: validation -> 400 with `validationErrors`; unknown id -> 404; duplicate email -> 409;
order for a non-existent user -> 404 `User not found`; User Service down -> 503.

## Service-to-service call
`OrderServiceImpl.createOrder` -> `UserClient` -> `GET http://USER-SERVICE/api/users/{id}` using a
`@LoadBalanced RestClient.Builder`. This call goes **directly** between services (not via the gateway).
Test: create an order with `userId` 9999 -> 404; stop user-service and create an order -> 503.

## Gateway: explicit routes vs discovery locator
Explicit routes (used here) expose only the paths you choose (`/api/users/**`, `/api/orders/**`).
`spring.cloud.gateway.discovery.locator.enabled=true` would auto-expose every registered service under
`/SERVICE-NAME/**`, which is convenient for demos but exposes more than you intend. The Eureka dashboard
stays on 8761, separate from the business routes.

## Troubleshooting
- **404 via gateway:** path doesn't match a route predicate, or the service isn't implementing that path. Try the direct port first.
- **503 from gateway ("Unable to find instance"):** the service isn't registered yet (check dashboard), or the name in `lb://` is wrong. Wait ~30 s after startup.
- **Connection refused:** target app not running or wrong port; check `netstat`/`lsof -i :8081`.
- **Not registering with Eureka:** check `eureka.client.service-url.defaultZone` and that Eureka started first.
- **Port already in use:** stop the old process or change `server.port` in that service's `application.yml`.
- **MySQL access denied:** set `DB_USERNAME` / `DB_PASSWORD`.
- **Lombok errors in IntelliJ:** enable *Settings > Build > Compiler > Annotation Processors > Enable annotation processing*.
- **Which service handled a request:** every log line is tagged `[USER-SERVICE:8081]`, `[ORDER-SERVICE:8082]`, `[API-GATEWAY]`.

## Kafka: event-driven notifications and auditing

```
                                   topic: order-events  (3 partitions, key = orderId)
 Order Service  --publishes-->  +--------------------------------------------+
 (producer)                     |  ORDER_CREATED / ORDER_UPDATED / ORDER_DELETED |
                                +--------------------------------------------+
                                     |                              |
                       group: notification-group          group: audit-group
                                     v                              v
                         Notification Service (8083)       Audit Service (8084)
                         builds a message, saves it        stores every event, never edits it
                         notification_service_db           audit_service_db
```

**Why Kafka here?** Order Service only announces "an order was created" and does not know who cares.
Notification Service and Audit Service react on their own. If one of them is down, orders still work and the
service catches up from its saved position (offset) when it comes back. Adding a third consumer later needs
no change in Order Service.

**Producer side (order-service)**
- `OrderServiceImpl` announces an `OrderEvent` after create / update / delete.
- `OrderEventPublisher` sends it to Kafka only AFTER the database transaction commits, so a rolled-back order is never published.
- If Kafka is down, the order is still saved; the failed publish is only logged (gives up after ~5 s). A production system would use the Outbox pattern to guarantee delivery.

**Consumer side (notification-service, audit-service)**
- Each has its own consumer group, so each receives every event.
- Each has its own copy of the event class (only the JSON shape is shared).
- Duplicate protection: `eventId` is unique in both databases, so a redelivered message is ignored.
- Failed messages are retried 3 times (1 s apart), then logged and skipped.

### Run with Docker
```
docker compose up --build -d
```
Starts MySQL, Kafka, Eureka and all six apps. Kafka must be healthy before Order/Notification/Audit start (handled by `depends_on`).
Needs roughly 5-6 GB of free RAM for Docker; stop other heavy programs first.

The MySQL volume may already exist from before. `init.sql` only runs on a brand-new volume, but the new services create
their own database through `createDatabaseIfNotExist=true`, so you do NOT have to delete the volume.

### Run from IntelliJ instead
Start only the infrastructure in Docker, then run the apps from IntelliJ (they use `localhost:9092` by default):
```
docker compose up -d kafka mysql
```
Start Kafka before the apps. Without it, Order Service waits ~30 s at startup for the topic check.

### Test the Kafka flow (through the gateway on 8080)
1. `POST /api/users` - create a user (note its `id`).
2. `POST /api/orders` with that `userId` -> 201.
3. `GET /api/notifications` -> a `SENT` notification: "Your order #1 for 2 x Laptop has been placed. Total: 999.98."
4. `GET /api/audits` -> a row with `ORDER_CREATED`, the full JSON `payload` and the Kafka `kafkaPartition` / `kafkaOffset`.
5. `PUT /api/orders/1` and `DELETE /api/orders/1`, then `GET /api/audits/order/1` -> the whole history of that order.

### Watch Kafka itself
```
# read the raw messages on the topic
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:29092 --topic order-events --from-beginning

# how far behind each consumer group is (LAG 0 = fully caught up)
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:29092 --describe --group notification-group
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:29092 --describe --group audit-group

# list topics
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:29092 --list
```

### Try the failure cases
- `docker stop notification-service`, create 3 orders (they still return 201), then `docker start notification-service`.
  Within seconds `GET /api/notifications` shows the 3 missed notifications: Kafka kept them.
- `docker stop kafka`, create an order: it still returns 201 (about 5 s slower); order-service logs `Could not publish ... to Kafka`. That event is lost - the reason the Outbox pattern exists.

### Kafka troubleshooting
- **Consumers log `Connection to node -1 could not be established`:** Kafka is not reachable. In Docker the address must be `kafka:29092`; from IntelliJ it must be `localhost:9092`.
- **`docker compose ps` shows kafka `unhealthy`:** check `docker compose logs kafka`. A stale `kafka-data` volume can conflict; `docker compose down -v` resets everything (this also deletes MySQL data).
- **A service shows `Exited` with out-of-memory:** raise Docker's memory (Docker Desktop / `.wslconfig`) or stop other containers.
- **No notification appears:** check `docker compose logs notification-service` for `Received from Kafka`. If missing, check that order-service logged `Published ORDER_CREATED`.
