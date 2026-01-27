package org.project.application.service;

import static org.project.application.util.RestUtil.responseException;

import java.util.List;

import org.project.application.dto.fleet.DriverDTO;
import org.project.application.pagination.PageRequest;
import org.project.domain.fleet.repositories.DriverRepository;
import org.project.domain.fleet.repositories.OwnerRepository;
import org.project.domain.shared.value_objects.UserID;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailableDriversService {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final DriverRepository driverRepository;

    public AvailableDriversService(
            UserRepository userRepository,
            OwnerRepository ownerRepository,
            DriverRepository driverRepository
    ) {
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional(readOnly = true)
    public List<DriverDTO> page(String identifier, PageRequest pageRequest) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        if (!ownerRepository.isOwnerExists(new UserID(user.id())))
            throw responseException(
                    HttpStatus.FORBIDDEN,
                    "Owner account is not registered"
            );

        return driverRepository
                .page(pageRequest)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Cannot find available drivers"
                        )
                );
    }
}
