package org.project.application.controller.fleet;

import java.util.List;

import org.project.application.dto.fleet.DriverDTO;
import org.project.application.pagination.PageRequest;
import org.project.application.service.AvailableDriversService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/available/drivers")
@PreAuthorize("hasRole('USER')")
public class AvailableDriversController {

    private final AvailableDriversService availableDriversService;

    public AvailableDriversController(AvailableDriversService availableDriversService) {
        this.availableDriversService = availableDriversService;
    }

    @GetMapping
    public List<DriverDTO> availableDrivers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    ) {
        return availableDriversService.page(
                jwt.getSubject(),
                new PageRequest(size, page)
        );
    }
}
