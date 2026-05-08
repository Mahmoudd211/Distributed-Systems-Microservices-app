package com.homesvc.booking.messaging;

public final class RabbitTopology {

    public static final String EXCHANGE = "booking.exchange";
    public static final String SUCCESS_QUEUE = "booking.success.queue";
    public static final String FAILURE_QUEUE = "booking.failure.queue";
    public static final String ROUTING_SUCCESS = "booking.success";
    public static final String ROUTING_FAILURE = "booking.failure";

    private RabbitTopology() {
    }
}
