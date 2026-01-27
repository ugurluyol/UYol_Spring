package org.project.application.service;

import static org.project.application.util.RestUtil.responseException;

import java.util.List;

import org.project.application.dto.fleet.CarDTO;
import org.project.domain.fleet.repositories.CarRepository;
import org.project.domain.shared.value_objects.Pageable;
import org.project.domain.shared.value_objects.UserID;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisteredCarsService {

    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public RegisteredCarsService(
            CarRepository carRepository,
            UserRepository userRepository
    ) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CarDTO> registeredCars(String identifier, Pageable page) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        return carRepository
                .pageOf(page, new UserID(user.id()))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "No cars found"
                        )
                );
    }
}
