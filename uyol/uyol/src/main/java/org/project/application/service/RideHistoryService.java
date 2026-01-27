package org.project.application.service;

import static org.project.application.util.RestUtil.required;
import static org.project.application.util.RestUtil.responseException;

import java.util.List;

import org.project.application.dto.ride.RideDTO;
import org.project.domain.fleet.entities.Driver;
import org.project.domain.fleet.entities.Owner;
import org.project.domain.fleet.repositories.DriverRepository;
import org.project.domain.fleet.repositories.OwnerRepository;
import org.project.domain.ride.repositories.RideRepository;
import org.project.domain.shared.value_objects.Pageable;
import org.project.domain.shared.value_objects.UserID;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideHistoryService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final DriverRepository driverRepository;

    public RideHistoryService(
            RideRepository rideRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            OwnerRepository ownerRepository
    ) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional(readOnly = true)
    public List<RideDTO> userRides(String identifier, Pageable page) {
        required("page", page);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        return rideRepository
                .pageOf(new UserID(user.id()), page)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Ride history not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<RideDTO> driverRides(String identifier, Pageable page) {
        required("page", page);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        Driver driver = driverRepository
                .findBy(new UserID(user.id()))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Driver account not found"
                        )
                );

        return rideRepository
                .pageOf(driver.id(), page)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Ride history not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<RideDTO> ownerRides(String identifier, Pageable page) {
        required("page", page);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        Owner owner = ownerRepository
                .findBy(new UserID(user.id()))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Owner account not found"
                        )
                );

        return rideRepository
                .pageOf(owner.id(), page)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Owner history not found"
                        )
                );
    }
}
