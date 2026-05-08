package com.homesvc.booking.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ApplicationPath("/")
public class BookingApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                BookingApplication.class,
                BookingResource.class,
                JwtAuthFilter.class
        );
    }
}
