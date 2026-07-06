# On-Demand Home Services Marketplace

Distributed microservices platform for a home-services booking workflow. The repository combines Spring Boot REST services, a Jakarta EE booking service on WildFly, RabbitMQ-based messaging, MySQL persistence, JWT security, and Postman collections for API testing.

## Project Overview

This project models a service marketplace where users authenticate, browse offers, place bookings, and exchange asynchronous updates through messaging. It is built as a modular backend system so each service can be developed, deployed, and tested independently.

## Features

- Spring Boot REST APIs for auth, wallet, offer, notification, and admin workflows
- Jakarta EE booking service deployed to WildFly
- EJB-based backend logic in the booking service
- RabbitMQ integration for asynchronous booking result handling
- JWT-based authentication across services
- MySQL-backed persistence for service data
- Postman collection and environment for API verification

## Architecture

- Auth service: handles user authentication and JWT-based access control.
- Wallet service: manages wallet-related business operations.
- Offer service: exposes offer data and related API endpoints.
- Notification service: handles notification workflows for the platform.
- Admin service: provides administrative REST endpoints and backend operations.
- Booking service: Jakarta EE application on WildFly that coordinates booking flow, EJB business logic, MySQL access, and RabbitMQ consumption.
- Shared DTO module: contains shared models and security types used across services.

## Tech Stack

- Java 17
- Maven
- Spring Boot
- Jakarta EE
- EJB
- RabbitMQ
- JWT
- REST APIs
- MySQL
- Docker and Docker Compose
- WildFly 30+

## Project Structure

```text
.
├── admin-service/
├── auth-service/
├── booking-service/
├── mysql-init/
├── notification-service/
├── offer-service/
├── postman/
├── shared-dto/
├── wallet-service/
└── docker-compose.yml
```

## Installation & Setup

### Prerequisites

- JDK 17
- Maven 3.9+
- Docker Desktop
- WildFly 30+ for `booking-service`

### 1. Install the shared DTO module

```bash
mvn -f shared-dto/pom.xml install
```

### 2. Start infrastructure services

```bash
docker compose up -d
```

This starts:

- RabbitMQ on `localhost:5672`
- RabbitMQ management UI at `http://localhost:15672` with `admin/admin`
- MySQL on `localhost:3306` with root password `root`
- Database initialization from `mysql-init/01-databases.sql`

### 3. Prepare WildFly for the booking service

The booking service is packaged as a WAR and deployed to WildFly. Before running it:

- Install the MySQL JDBC driver as a WildFly module:

```xml
<module xmlns="urn:jboss:module:1.9" name="com.mysql">
	<resources>
		<resource-root path="mysql-connector-j-8.3.0.jar"/>
	</resources>
	<dependencies>
		<module name="jakarta.api"/>
		<module name="jakarta.transaction.api"/>
	</dependencies>
</module>
```

- Create the `BookingDS` datasource with JNDI name `java:jboss/datasources/BookingDS`:

```bash
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql,driver-class-name=com.mysql.cj.jdbc.Driver)
/subsystem=datasources/data-source=BookingDS:add(jndi-name=java:jboss/datasources/BookingDS,driver-name=mysql,connection-url=jdbc:mysql://localhost:3306/booking_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8,user-name=root,password=root,min-pool-size=5,max-pool-size=20,enabled=true,jta=true)
```

- Build and deploy the WAR:

```bash
mvn -f booking-service/pom.xml package
cp booking-service/target/booking-service.war $WILDFLY_HOME/standalone/deployments/
```

Detailed steps are in [booking-service/README-WILDFLY.md](booking-service/README-WILDFLY.md).

### 4. Defaults

- JWT secret for all services: `HomeSvcPlatform2024SecretKey`
- Seeded admin credentials: `admin` / `admin123`

## Running the Project

Run each Spring Boot service in its own terminal:

```bash
mvn -f auth-service/pom.xml spring-boot:run
mvn -f wallet-service/pom.xml spring-boot:run
mvn -f offer-service/pom.xml spring-boot:run
mvn -f notification-service/pom.xml spring-boot:run
mvn -f admin-service/pom.xml spring-boot:run
```

Build the booking service WAR:

```bash
mvn -f booking-service/pom.xml package
```

Then deploy the WAR to WildFly after the datasource is configured:

```bash
booking-service/target/booking-service.war
```

## Postman Testing

Import these files into Postman:

- [HomeServices.postman_collection.json](postman/HomeServices.postman_collection.json)
- [HomeServices.postman_environment.json](postman/HomeServices.postman_environment.json)

Use the environment file to load the local service URLs and test the REST APIs end to end.
