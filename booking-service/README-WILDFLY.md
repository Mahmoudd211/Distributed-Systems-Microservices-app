# Deploying booking-service on WildFly 30+

## 1. MySQL JDBC driver as a module

Install the MySQL connector as a WildFly module (paths relative to `WILDFLY_HOME`).

Create `modules/com/mysql/main/module.xml`:

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

Place `mysql-connector-j-8.3.0.jar` in the same folder.

## 2. Datasource `BookingDS`

Run `jboss-cli` connected to the server (use `standalone-full.xml` if you rely on embedded messaging for other features):

```bash
/subsystem=datasources/jdbc-driver=mysql:add(driver-name=mysql,driver-module-name=com.mysql,driver-class-name=com.mysql.cj.jdbc.Driver)
/subsystem=datasources/data-source=BookingDS:add(jndi-name=java:jboss/datasources/BookingDS,driver-name=mysql,connection-url=jdbc:mysql://localhost:3306/booking_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8,user-name=root,password=root,min-pool-size=5,max-pool-size=20,enabled=true,jta=true)
```

## 3. Deploy WAR

```bash
mvn -f booking-service/pom.xml package
cp booking-service/target/booking-service.war $WILDFLY_HOME/standalone/deployments/
```

Or use the WildFly Maven plugin from the `booking-service` directory.

## 4. RabbitMQ / HTTP endpoints

The booking service uses:

- RabbitMQ defaults: host `localhost`, user `admin`, pass `admin` (override with system properties `rabbitmq.host`, `rabbitmq.user`, `rabbitmq.pass`).
- REST dependencies: offer `http://localhost:8083`, wallet `http://localhost:8082`, notification `http://localhost:8084` (override with `offer.service.base-url`, `wallet.service.base-url`, `notification.service.base-url`).

## 5. Optional: strict `@MessageDriven` + JMS

To use a classic MDB on a JMS queue bridged from RabbitMQ, configure a JMS resource adapter / bridge (e.g. RabbitMQ JMS client RA) and add an MDB with `destinationLookup` pointing at your JMS queue. The prompt allows the Singleton AMQP consumer shipped here instead.
