package com.homesvc.booking.ejb;

import com.homesvc.booking.entity.Booking;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class BookingQueryBean {

    @PersistenceContext(unitName = "bookingPU")
    private EntityManager em;

    public List<Booking> findAll() {
        return em.createQuery("select b from Booking b order by b.bookedAt desc", Booking.class)
                .getResultList();
    }

    public List<Booking> findByCustomerId(Long customerId) {
        return em.createQuery("select b from Booking b where b.customerId = :c order by b.bookedAt desc", Booking.class)
                .setParameter("c", customerId)
                .getResultList();
    }

    public Booking findById(Long id) {
        return em.find(Booking.class, id);
    }
}
