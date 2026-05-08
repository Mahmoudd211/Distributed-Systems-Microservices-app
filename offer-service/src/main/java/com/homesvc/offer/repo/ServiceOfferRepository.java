package com.homesvc.offer.repo;

import com.homesvc.offer.model.ServiceOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, Long> {

    List<ServiceOffer> findByProviderIdOrderByIdDesc(Long providerId);

    @Query("SELECT o FROM ServiceOffer o JOIN o.category c WHERE c.name = :name AND o.active = true AND o.availableDate >= :today")
    List<ServiceOffer> findActiveByCategoryName(@Param("name") String name, @Param("today") LocalDate today);
}
