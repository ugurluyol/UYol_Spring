package org.project.application.controller.ride;

import java.util.UUID;

import org.project.application.dto.ride.BookingForm;
import org.project.application.dto.ride.RideContractDTO;
import org.project.application.service.RideReservationService;
import org.project.domain.ride.value_object.RideID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ride/reservation")
@PreAuthorize("hasRole('USER')")
public class RideReservationController {

    private final RideReservationService rideReservationService;

    public RideReservationController(RideReservationService rideReservationService) {
        this.rideReservationService = rideReservationService;
    }

    @PostMapping("/book")
    public RideContractDTO book(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BookingForm bookingForm
    ) {
        return rideReservationService.book(jwt.getSubject(), bookingForm);
    }

    @PostMapping("/rate/driver")
    public ResponseEntity<Void> rateDriver(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("rideID") UUID rideID,
            @RequestParam("score") int score
    ) {
        rideReservationService.rateDriver(
                jwt.getSubject(),
                new RideID(rideID),
                score
        );
        return ResponseEntity.accepted().build();
    }
}
