package org.project.application.service;

import static org.project.application.util.RestUtil.required;
import static org.project.application.util.RestUtil.responseException;

import java.util.Optional;
import java.util.UUID;

import org.project.application.dto.fleet.CarDTO;
import org.project.application.dto.ride.RideRequestToDriver;
import org.project.application.util.RestUtil;
import org.project.domain.fleet.entities.Car;
import org.project.domain.fleet.entities.Driver;
import org.project.domain.fleet.entities.Owner;
import org.project.domain.fleet.repositories.CarRepository;
import org.project.domain.fleet.repositories.DriverRepository;
import org.project.domain.fleet.repositories.OwnerRepository;
import org.project.domain.fleet.value_objects.CarBrand;
import org.project.domain.fleet.value_objects.CarColor;
import org.project.domain.fleet.value_objects.CarModel;
import org.project.domain.fleet.value_objects.CarYear;
import org.project.domain.fleet.value_objects.LicensePlate;
import org.project.domain.fleet.value_objects.SeatCount;
import org.project.domain.fleet.value_objects.Voen;
import org.project.domain.ride.entities.Ride;
import org.project.domain.ride.entities.RideRequest;
import org.project.domain.ride.enumerations.RideRule;
import org.project.domain.ride.repositories.RideRepository;
import org.project.domain.ride.value_object.RideID;
import org.project.domain.shared.value_objects.DriverID;
import org.project.domain.shared.value_objects.UserID;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.UserRepository;
import org.project.infrastructure.cache.RideRequests;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerService {

    private final RideRequests rideRequests;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    public OwnerService(
            Optional<RideRequests> rideRequests,
            UserRepository userRepository,
            OwnerRepository ownerRepository,
            CarRepository carRepository,
            DriverRepository driverRepository,
            RideRepository rideRepository
    ) {
        this.rideRequests = rideRequests.orElse(null);
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.carRepository = carRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    @Transactional
    public void register(String identifier, String voenRaw) {
        Voen voen = new Voen(voenRaw);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        UserID userID = new UserID(user.id());

        if (ownerRepository.isOwnerExists(userID))
            throw responseException(
                    HttpStatus.CONFLICT,
                    "Owner is already registered to this account"
            );

        if (ownerRepository.isVoenExists(voen))
            throw responseException(
                    HttpStatus.CONFLICT,
                    "Voen is already used"
            );

        Owner owner = Owner.of(userID, voen);

        ownerRepository.save(owner)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.CONFLICT,
                                "Unable to process your request at the moment. Please try again."
                        )
                );
    }

    @Transactional
    public void saveCar(String identifier, CarDTO carDTO) {
        required("carForm", carDTO);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        UserID userID = new UserID(user.id());

        if (!ownerRepository.isOwnerExists(userID))
            throw responseException(
                    HttpStatus.NOT_FOUND,
                    "Owner account is not found."
            );

        Car car = Car.of(
                userID,
                new LicensePlate(carDTO.licensePlate()),
                new CarBrand(carDTO.carBrand()),
                new CarModel(carDTO.carModel()),
                new CarColor(carDTO.carColor()),
                new CarYear(carDTO.carYear()),
                new SeatCount(carDTO.seatCount())
        );

        carRepository.save(car)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Unable to process your request at the moment. Please try again."
                        )
                );
    }

    @Transactional
    public void request(String identifier, RideRequestToDriver rideForm) {
        required("rideForm", rideForm);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        UserID userID = new UserID(user.id());

        Owner owner = ownerRepository
                .findBy(userID)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Owner account is not found."
                        )
                );

        RideRequest rideRequest = rideForm.toRideRequest(owner.id());

        Car car = carRepository
                .findBy(new LicensePlate(rideForm.licensePlate()))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Car by this driver account is not found."
                        )
                );

        if (!car.owner().equals(userID))
            throw responseException(
                    HttpStatus.FORBIDDEN,
                    "You are not the owner of the car or license plate is wrong."
            );

        if (!car.isAvailableForTheTrip())
            throw responseException(
                    HttpStatus.CONFLICT,
                    "Selected car is already on the road."
            );

        Driver driver = driverRepository
                .findBy(new DriverID(rideForm.driverID()))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Driver account is not found."
                        )
                );

        if (!driver.isAvailable())
            throw responseException(
                    HttpStatus.CONFLICT,
                    "Driver is not available."
            );

        rideRequests.put(driver.id(), rideRequest);
    }

    @Transactional
    public void addRideRule(String identifier, RideRule rideRule, UUID rideUUID) {
        Ride ride = validateAndRetrieveRide(identifier, rideUUID);
        ride.addRideRule(rideRule);
        rideRepository.updateRules(ride)
                .orElseThrow(RestUtil::unableToProcessRequestException);
    }

    @Transactional
    public void removeRideRule(String identifier, RideRule rideRule, UUID rideUUID) {
        Ride ride = validateAndRetrieveRide(identifier, rideUUID);
        ride.removeRideRule(rideRule);
        rideRepository.updateRules(ride)
                .orElseThrow(RestUtil::unableToProcessRequestException);
    }

    @Transactional
    public void startRide(String identifier, UUID rideUUID) {
        Ride ride = validateAndRetrieveRide(identifier, rideUUID);
        ride.start();
        rideRepository.updateStatus(ride)
                .orElseThrow(RestUtil::unableToProcessRequestException);
    }

    @Transactional
    public void cancelRide(String identifier, UUID rideUUID) {
        Ride ride = validateAndRetrieveRide(identifier, rideUUID);
        ride.cancel();
        rideRepository.updateStatus(ride)
                .orElseThrow(RestUtil::unableToProcessRequestException);
    }

    @Transactional
    public void finishRide(String identifier, UUID rideUUID) {
        Ride ride = validateAndRetrieveRide(identifier, rideUUID);
        ride.finish();
        rideRepository.updateStatus(ride)
                .orElseThrow(RestUtil::unableToProcessRequestException);
    }

    @Transactional(readOnly = true)
    protected Ride validateAndRetrieveRide(String identifier, UUID rideUUID) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        UserID userID = new UserID(user.id());

        Owner owner = ownerRepository
                .findBy(userID)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Owner account is not found."
                        )
                );

        Ride ride = rideRepository
                .findBy(new RideID(rideUUID))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Ride is not found."
                        )
                );

        boolean notAnOwnerOfThisRide = ride.rideOwner()
                .ownerID()
                .map(ownerId -> !ownerId.equals(owner.id()))
                .orElse(true);

        if (notAnOwnerOfThisRide)
            throw responseException(
                    HttpStatus.FORBIDDEN,
                    "You can't modify someone else's ride"
            );

        return ride;
    }
}
