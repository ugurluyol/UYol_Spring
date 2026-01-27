package org.project.application.service;

import static org.project.application.util.RestUtil.responseException;

import java.util.List;
import java.util.UUID;

import org.project.application.dto.ride.RideContractDTO;
import org.project.application.pagination.PageRequest;
import org.project.domain.ride.entities.RideContract;
import org.project.domain.ride.repositories.RideContractRepository;
import org.project.domain.ride.value_object.RideContractID;
import org.project.domain.ride.value_object.RideID;
import org.project.domain.shared.value_objects.Pageable;
import org.project.domain.shared.value_objects.UserID;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideContractService {

    private final UserRepository userRepository;
    private final RideContractRepository contractsRepository;

    public RideContractService(
            UserRepository userRepository,
            RideContractRepository contractsRepository
    ) {
        this.userRepository = userRepository;
        this.contractsRepository = contractsRepository;
    }

    @Transactional(readOnly = true)
    public RideContractDTO of(String identifier, UUID rideContractID) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        RideContract rideContract = contractsRepository
                .findBy(new RideContractID(rideContractID))
                .orElseThrow(() ->
                        responseException(HttpStatus.BAD_REQUEST, "Cannot find ride contract.")
                );

        boolean notOwnerOfContract = !rideContract.userID().value().equals(user.id());
        if (notOwnerOfContract)
            throw responseException(
                    HttpStatus.FORBIDDEN,
                    "You are not an owner of this contract."
            );

        return RideContractDTO.from(rideContract);
    }

    @Transactional(readOnly = true)
    public List<RideContractDTO> ofRide(
            String identifier,
            UUID rideID,
            Pageable pageable
    ) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        List<RideContract> rideContracts = contractsRepository
                .findBy(new RideID(rideID), pageable)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Cannot find ride contracts for this ride."
                        )
                );

        rideContracts.forEach(rideContract -> {
            boolean notOwnerOfContract =
                    !rideContract.userID().value().equals(user.id());
            if (notOwnerOfContract)
                throw responseException(
                        HttpStatus.FORBIDDEN,
                        "You are not an owner of this contract."
                );
        });

        return rideContracts
                .stream()
                .map(RideContractDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideContractDTO> ofUser(
            String identifier,
            PageRequest pageRequest
    ) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        return contractsRepository
                .findBy(new UserID(user.id()), pageRequest)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "User contracts not found"
                        )
                )
                .stream()
                .map(RideContractDTO::from)
                .toList();
    }
}
