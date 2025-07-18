package com.example.backend.tour.repository;

import com.example.backend.cart.entity.Cart;
import com.example.backend.tour.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {

    Optional<Tour> findByContentId(String contentId);
    List<Tour> findByCart(Cart cart);
    void deleteByCartAndTourId(Cart cart, UUID tourId);
    void deleteAllByCart(Cart cart);
}