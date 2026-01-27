package org.project.application.controller.fleet;

import java.util.List;
import java.util.UUID;

import org.project.application.dto.fleet.CarDTO;
import org.project.application.dto.ride.DriverRideForm;
import org.project.application.dto.ride.RideDTO;
import org.project.application.dto.ride.RideRequestToDriver;
import org.project.application.service.DriverService;
import org.project.domain.ride.enumerations.RideRule;
import org.project.domain.ride.value_object.RideRequestID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/driver")
@PreAuthorize("hasRole('USER')")
public class DriverController {

    private final DriverService service;

    public DriverController(DriverService service) {
        this.service = service;
    }

    @PostMapping("/registration")
    public ResponseEntity<Void> registerDriver(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("driver_license") String driverLicense
    ) {
        service.register(jwt.getSubject(), driverLicense);
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

    @PostMapping("/create/ride")
    public RideDTO createRide(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DriverRideForm rideForm
    ) {
        return service.createRide(jwt.getSubject(), rideForm);
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

    @GetMapping("/ride-requests")
    public List<RideRequestToDriver> rideRequests(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.rideRequests(jwt.getSubject());
    }

    @PostMapping("/accept/ride-request")
    public RideDTO acceptRideRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("rideRequestID") UUID rideRequestID
    ) {
        return service.acceptRideRequest(
                jwt.getSubject(),
                new RideRequestID(rideRequestID)
        );
    }
}
