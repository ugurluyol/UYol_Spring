package org.project.application.controller.ride;

import java.util.List;
import java.util.UUID;

import org.project.application.dto.ride.RideContractDTO;
import org.project.application.pagination.PageRequest;
import org.project.application.service.RideContractService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ride/contract")
@PreAuthorize("hasRole('USER')")
public class RideContractController {

    private final RideContractService rideContractService;

    public RideContractController(RideContractService rideContractService) {
        this.rideContractService = rideContractService;
    }

    @GetMapping
    public RideContractDTO rideContract(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("rideContractID") UUID rideContractID
    ) {
        return rideContractService.of(jwt.getSubject(), rideContractID);
    }

    @GetMapping("/of/ride")
    public List<RideContractDTO> rideContractsOfRide(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("rideID") UUID rideID,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return rideContractService.ofRide(
                jwt.getSubject(),
                rideID,
                new PageRequest(size, page)
        );
    }

    @GetMapping("/all")
    public List<RideContractDTO> rideContractsOfUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return rideContractService.ofUser(
                jwt.getSubject(),
                new PageRequest(size, page)
        );
    }
}
