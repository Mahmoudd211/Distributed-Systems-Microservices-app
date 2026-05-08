package com.homesvc.booking.rest;

import com.homesvc.booking.ejb.BookingProcessorBean;
import com.homesvc.booking.ejb.BookingQueryBean;
import com.homesvc.booking.entity.Booking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/bookings")
@SecuredApi
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    @Inject
    private BookingProcessorBean bookingProcessorBean;

    @Inject
    private BookingQueryBean bookingQueryBean;

    @POST
    public Response create(@Context ContainerRequestContext ctx, Map<String, Object> body) throws IOException {
        String role = (String) ctx.getProperty("role");
        if (!"CUSTOMER".equals(role)) {
            return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", "CUSTOMER role required")).build();
        }
        Long customerId = (Long) ctx.getProperty("userId");
        Object oid = body.get("offerId");
        if (oid == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "offerId required")).build();
        }
        long offerId = ((Number) oid).longValue();
        Booking b = bookingProcessorBean.createBooking(customerId, offerId);
        return Response.status(Response.Status.CREATED).entity(toMap(b)).build();
    }

    @GET
    @Path("customer/{id}")
    public Response listForCustomer(@Context ContainerRequestContext ctx, @PathParam("id") Long id) {
        String role = (String) ctx.getProperty("role");
        Long userId = (Long) ctx.getProperty("userId");
        if (!"CUSTOMER".equals(role) || !userId.equals(id)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<Map<String, Object>> list = bookingQueryBean.findByCustomerId(id).stream().map(this::toMap).collect(Collectors.toList());
        return Response.ok(list).build();
    }

    @GET
    @Path("{id}")
    public Response getOne(@PathParam("id") Long id) {
        Booking b = bookingQueryBean.findById(id);
        if (b == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toMap(b)).build();
    }

    @GET
    public Response listAll(@Context ContainerRequestContext ctx) {
        String role = (String) ctx.getProperty("role");
        if (!"ADMIN".equals(role)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<Map<String, Object>> list = bookingQueryBean.findAll().stream().map(this::toMap).collect(Collectors.toList());
        return Response.ok(list).build();
    }

    private Map<String, Object> toMap(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("customerId", b.getCustomerId());
        m.put("providerId", b.getProviderId());
        m.put("offerId", b.getOfferId());
        m.put("amount", b.getAmount());
        m.put("status", b.getStatus() != null ? b.getStatus().name() : null);
        m.put("bookedAt", b.getBookedAt() != null ? b.getBookedAt().toString() : null);
        m.put("failureReason", b.getFailureReason());
        return m;
    }
}
