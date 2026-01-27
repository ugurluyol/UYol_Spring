package org.project.application.service;

import static org.project.application.util.RestUtil.responseException;

import java.time.LocalDate;
import java.util.List;

import org.project.application.dto.ride.RideDTO;
import org.project.application.pagination.PageRequest;
import org.project.domain.ride.repositories.RideRepository;
import org.project.domain.ride.value_object.Location;
import org.project.domain.shared.containers.Result;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActiveRidesService {

    private final RideRepository rideRepository;

    public ActiveRidesService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    @Transactional(readOnly = true)
    public List<RideDTO> pageBy(String date, PageRequest pageRequest) {
        LocalDate localDate = Result
                .ofThrowable(() -> LocalDate.parse(date))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid date format"
                        )
                );

        return rideRepository
                .pageOf(localDate, pageRequest)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "No data found for this page"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<RideDTO> actualFor(
            String date,
            Location startLocation,
            Location endLocation,
            PageRequest pageRequest
    ) {
        LocalDate localDate = Result
                .ofThrowable(() -> LocalDate.parse(date))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid date format"
                        )
                );

        return rideRepository
                .actualFor(startLocation, endLocation, localDate, pageRequest)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "No data found for this page"
                        )
                );
    }
}
