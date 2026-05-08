package com.homesvc.booking.messaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class RabbitEventPublisher {

    private static final String HOST = System.getProperty("rabbitmq.host", "localhost");
    private static final String USER = System.getProperty("rabbitmq.user", "admin");
    private static final String PASS = System.getProperty("rabbitmq.pass", "admin");

    private Connection connection;
    private Channel channel;

    @PostConstruct
    public void init() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setUsername(USER);
        factory.setPassword(PASS);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(RabbitTopology.EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.queueDeclare(RabbitTopology.SUCCESS_QUEUE, true, false, false, null);
        channel.queueDeclare(RabbitTopology.FAILURE_QUEUE, true, false, false, null);
        channel.queueBind(RabbitTopology.SUCCESS_QUEUE, RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_SUCCESS);
        channel.queueBind(RabbitTopology.FAILURE_QUEUE, RabbitTopology.EXCHANGE, RabbitTopology.ROUTING_FAILURE);
    }

    @PreDestroy
    public void shutdown() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }

    public void publish(String routingKey, byte[] body) throws IOException {
        channel.basicPublish(RabbitTopology.EXCHANGE, routingKey, null, body);
    }
}
