package com.example.backend.tour.entity;


import com.example.backend.cart.entity.Cart;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tour")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tour_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID tourId;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "image", length = 255)
    private String image;

    @Column(name = "tema", length = 50)
    private String tema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cartId;

}
