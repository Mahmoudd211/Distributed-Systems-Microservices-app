package com.homesvc.booking.ejb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homesvc.dto.BookingEventDTO;
import com.homesvc.booking.messaging.NotificationDispatcher;
import com.homesvc.booking.messaging.RabbitTopology;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Consumes booking success/failure events from RabbitMQ and notifies users.
 * Per implementation_prompt.md, a {@code @Singleton @Startup} consumer using {@code com.rabbitmq.client.Channel}
 * is an acceptable alternative when a WildFly JMS–RabbitMQ bridge is not configured.
 */
@Singleton
@Startup
@DependsOn("RabbitEventPublisher")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class BookingResultMDB {

    private static final String HOST = System.getProperty("rabbitmq.host", "localhost");
    private static final String USER = System.getProperty("rabbitmq.user", "admin");
    private static final String PASS = System.getProperty("rabbitmq.pass", "admin");

    @Inject
    private NotificationDispatcher notificationDispatcher;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Connection connection;

    @PostConstruct
    public void init() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setUsername(USER);
        factory.setPassword(PASS);
        connection = factory.newConnection();
        startConsumer(RabbitTopology.SUCCESS_QUEUE);
        startConsumer(RabbitTopology.FAILURE_QUEUE);
    }

    private void startConsumer(String queue) throws IOException {
        Channel ch = connection.createChannel();
        ch.basicConsume(queue, false, (consumerTag, delivery) -> {
            try {
                String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
                BookingEventDTO dto = mapper.readValue(json, BookingEventDTO.class);
                notificationDispatcher.dispatch(dto);
            } catch (Exception ignored) {
            } finally {
                try {
                    ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException ignored) {
                }
            }
        }, consumerTag -> {
        });
    }

    @PreDestroy
    public void shutdown() throws IOException, TimeoutException {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}
