# On-Demand Home Services Marketplace

Microservices platform per `implementation_prompt.md`: Spring Boot services (auth, wallet, offer, notification, admin), Jakarta EE / WildFly booking service with EJBs and RabbitMQ, MySQL, and Postman integration tests.

## Prerequisites

- JDK 17, Maven 3.9+
- Docker Desktop (for MySQL + RabbitMQ)
- WildFly 30+ with `standalone-full.xml` (or equivalent) for **booking-service** WAR deployment
- `mvn -f shared-dto/pom.xml install` once before building other modules

## Infrastructure

```bash
docker compose up -d
```

- RabbitMQ: `localhost:5672`, management UI `http://localhost:15672` (admin/admin)
- MySQL: `localhost:3306`, root password `root`, databases created by `mysql-init/01-databases.sql`

## Run Spring Boot services (each terminal)

```bash
mvn -f auth-service/pom.xml spring-boot:run
mvn -f wallet-service/pom.xml spring-boot:run
mvn -f offer-service/pom.xml spring-boot:run
mvn -f notification-service/pom.xml spring-boot:run
mvn -f admin-service/pom.xml spring-boot:run
```

## Booking service (WildFly)

Build WAR: `mvn -f booking-service/pom.xml package`

Configure MySQL datasource `java:jboss/datasources/BookingDS` and deploy `booking-service/target/booking-service.war`. See [booking-service/README-WILDFLY.md](booking-service/README-WILDFLY.md).

## Defaults

- JWT secret (all services): `HomeSvcPlatform2024SecretKey`
- Seeded admin: `admin` / `admin123` (auth service)

## Postman

Import [postman/HomeServices.postman_collection.json](postman/HomeServices.postman_collection.json) and [postman/HomeServices.postman_environment.json](postman/HomeServices.postman_environment.json).

## EJB note (booking)

`BookingResultMDB` is implemented as a `@Singleton @Startup` RabbitMQ consumer using `com.rabbitmq.client`, which the implementation prompt lists as an **acceptable alternative** to a JMS `@MessageDriven` bean when a WildFly JMS–RabbitMQ bridge is not used. Stateless saga logic lives in `BookingProcessorBean`; optional `CategoryCacheBean` caches categories from the offer service.
