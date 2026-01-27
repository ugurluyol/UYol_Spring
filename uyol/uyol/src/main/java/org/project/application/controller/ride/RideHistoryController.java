package org.project.application.controller.ride;

import java.util.List;

import org.project.application.dto.ride.RideDTO;
import org.project.application.pagination.PageRequest;
import org.project.application.service.RideHistoryService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ride/history")
@PreAuthorize("hasRole('USER')")
public class RideHistoryController {

    private final RideHistoryService historyService;

    public RideHistoryController(RideHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/user-rides")
    public List<RideDTO> userRides(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        return historyService.userRides(
                jwt.getSubject(),
                new PageRequest(pageSize, pageNumber)
        );
    }

    @GetMapping("/driver-rides")
    public List<RideDTO> driverRides(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        return historyService.driverRides(
                jwt.getSubject(),
                new PageRequest(pageSize, pageNumber)
        );
    }

    @GetMapping("/owner-rides")
    public List<RideDTO> ownerRides(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        return historyService.ownerRides(
                jwt.getSubject(),
                new PageRequest(pageSize, pageNumber)
        );
    }
}
