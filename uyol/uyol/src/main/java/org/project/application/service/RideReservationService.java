package org.project.application.service;

import static org.project.application.util.RestUtil.required;
import static org.project.application.util.RestUtil.responseException;

import org.project.application.dto.ride.BookingForm;
import org.project.application.dto.ride.RideContractDTO;
import org.project.application.util.RestUtil;
import org.project.domain.fleet.entities.Driver;
import org.project.domain.fleet.repositories.DriverRepository;
import org.project.domain.ride.entities.Ride;
import org.project.domain.ride.entities.RideContract;
import org.project.domain.ride.repositories.RideContractRepository;
import org.project.domain.ride.repositories.RideRepository;
import org.project.domain.ride.value_object.BookedSeats;
import org.project.domain.ride.value_object.RideID;
import org.project.domain.shared.value_objects.UserID;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideReservationService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final RideContractRepository rideContractRepository;

    public RideReservationService(
            UserRepository userRepository,
            RideRepository rideRepository,
            DriverRepository driverRepository,
            RideContractRepository rideContractRepository
    ) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
        this.rideContractRepository = rideContractRepository;
    }

    @Transactional
    public RideContractDTO book(String identifier, BookingForm bookingForm) {
        required("bookingForm", bookingForm);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        RideID rideID = new RideID(bookingForm.rideID());

        Ride ride = rideRepository.findBy(rideID)
                .orElseThrow(() ->
                        responseException(HttpStatus.BAD_REQUEST, "This ride does not exist.")
                );

        RideContract rideContract = ride.book(
                new UserID(user.id()),
                new BookedSeats(bookingForm.bookedSeats())
        );

        rideContractRepository.save(rideContract)
                .orElseThrow(RestUtil::unableToProcessRequestException);

        rideRepository.updateSeats(ride)
                .orElseThrow(RestUtil::unableToProcessRequestException);

        return RideContractDTO.from(rideContract);
    }

    @Transactional
    public void rateDriver(String identifier, RideID rideID, int score) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        Ride ride = rideRepository.findBy(rideID)
                .orElseThrow(() ->
                        responseException(HttpStatus.NOT_FOUND, "This ride does not exist.")
                );

        if (!ride.isFinished())
            throw responseException(
                    HttpStatus.BAD_REQUEST,
                    "You can’t rate the driver until the ride is finished."
            );

        if (!rideContractRepository.isExists(new UserID(user.id()), ride.id()))
            throw responseException(
                    HttpStatus.FORBIDDEN,
                    "You weren’t a part of this ride"
            );

        Driver driver = driverRepository
                .findBy(ride.rideOwner().driverID())
                .orElseThrow(RestUtil::unableToProcessRequestException);

        boolean selfRating = driver.id().value().equals(user.id());
        if (selfRating)
            throw responseException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot rate yourself"
            );

        driver.rate(score);

        driverRepository.updateRating(driver)
                .orElseThrow(RestUtil::unableToProcessRequestException);
    }
}
