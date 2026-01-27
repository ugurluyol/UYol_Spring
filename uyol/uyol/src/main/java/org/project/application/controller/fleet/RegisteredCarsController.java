package org.project.application.controller.fleet;

import java.util.List;

import org.project.application.dto.fleet.CarDTO;
import org.project.application.pagination.PageRequest;
import org.project.application.service.RegisteredCarsService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registered/cars")
@PreAuthorize("hasRole('USER')")
public class RegisteredCarsController {

    private final RegisteredCarsService carsService;

    public RegisteredCarsController(RegisteredCarsService carsService) {
        this.carsService = carsService;
    }

    @GetMapping
    public List<CarDTO> registeredCars(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        return carsService.registeredCars(
                jwt.getSubject(),
                new PageRequest(pageSize, pageNumber)
        );
    }
}
