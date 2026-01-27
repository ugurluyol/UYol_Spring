package org.project.application.controller.fleet;

import java.util.UUID;

import org.project.application.dto.fleet.CarDTO;
import org.project.application.dto.ride.RideRequestToDriver;
import org.project.application.service.OwnerService;
import org.project.domain.ride.enumerations.RideRule;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner")
@PreAuthorize("hasRole('USER')")
public class OwnerController {

    private final OwnerService service;

    public OwnerController(OwnerService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerOwner(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("voen") String voen
    ) {
        service.register(jwt.getSubject(), voen);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/car/save")
    public ResponseEntity<Void> saveCar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CarDTO carDTO
    ) {
        service.saveCar(jwt.getSubject(), carDTO);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/ride/request")
    public ResponseEntity<Void> requestRide(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RideRequestToDriver rideForm
    ) {
        service.request(jwt.getSubject(), rideForm);
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/add/ride-rule")
    public ResponseEntity<Void> addRideRule(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("ride-rule") RideRule rideRule,
            @RequestParam("rideID") UUID rideID
    ) {
        service.addRideRule(jwt.getSubject(), rideRule, rideID);
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/remove/ride-rule")
    public ResponseEntity<Void> removeRideRule(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("ride-rule") RideRule rideRule,
            @RequestParam("rideID") UUID rideID
    ) {
        service.removeRideRule(jwt.getSubject(), rideRule, rideID);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/start/ride")
    public ResponseEntity<Void> startRide(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("rideID") UUID rideID
    ) {
        service.startRide(jwt.getSubject(), rideID);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/cancel/ride")
    public ResponseEntity<Void> cancelRide(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("rideID") UUID rideID
    ) {
        service.cancelRide(jwt.getSubject(), rideID);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/finish/ride")
    public ResponseEntity<Void> finishRide(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("rideID") UUID rideID
    ) {
        service.finishRide(jwt.getSubject(), rideID);
        return ResponseEntity.accepted().build();
    }
}
