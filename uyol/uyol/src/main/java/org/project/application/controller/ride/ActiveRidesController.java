package org.project.application.controller.ride;

import java.util.List;

import org.project.application.dto.ride.RideDTO;
import org.project.application.pagination.PageRequest;
import org.project.application.service.ActiveRidesService;
import org.project.domain.ride.value_object.Location;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ride")
public class ActiveRidesController {

    private final ActiveRidesService ridesService;

    public ActiveRidesController(ActiveRidesService ridesService) {
        this.ridesService = ridesService;
    }

    @GetMapping("/date")
    public List<RideDTO> pageOf(
            @RequestParam("date") String date,
            @RequestParam("limit") int limit,
            @RequestParam("offset") int offset
    ) {
        return ridesService.pageBy(
                date,
                new PageRequest(limit, offset)
        );
    }

    @GetMapping("/actual")
    public List<RideDTO> actualFor(
            @RequestParam("date") String date,
            @RequestParam("startDesc") String startDesc,
            @RequestParam("startLat") double startLat,
            @RequestParam("startLon") double startLon,
            @RequestParam("endDesc") String endDesc,
            @RequestParam("endLat") double endLat,
            @RequestParam("endLon") double endLon,
            @RequestParam("limit") int limit,
            @RequestParam("offset") int offset
    ) {
        return ridesService.actualFor(
                date,
                new Location(startDesc, startLat, startLon),
                new Location(endDesc, endLat, endLon),
                new PageRequest(limit, offset)
        );
    }
}
