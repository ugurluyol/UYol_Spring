package org.project.application.service;

import static org.project.application.util.RestUtil.required;
import static org.project.application.util.RestUtil.responseException;

import java.io.InputStream;

import org.project.application.dto.profile.UserProfileDTO;
import org.project.domain.fleet.entities.Driver;
import org.project.domain.fleet.entities.Owner;
import org.project.domain.fleet.repositories.DriverRepository;
import org.project.domain.fleet.repositories.OwnerRepository;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.UserRepository;
import org.project.domain.user.value_objects.ProfilePicture;
import org.project.infrastructure.files.ProfilePictureRepository;
import org.project.infrastructure.files.StreamUtils;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final DriverRepository driverRepository;
    private final ProfilePictureRepository pictureRepository;

    public UserProfileService(
            UserRepository userRepository,
            OwnerRepository ownerRepository,
            DriverRepository driverRepository,
            ProfilePictureRepository pictureRepository
    ) {
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.driverRepository = driverRepository;
        this.pictureRepository = pictureRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileDTO of(String identifier) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        Driver driver = driverRepository
                .findBy(user.userID())
                .orElse(null);

        Owner owner = ownerRepository
                .findBy(user.userID())
                .orElse(null);

        return UserProfileDTO.from(user, driver, owner);
    }

    @Transactional
    public void changeProfilePictureOf(String identifier, InputStream inputStream) {
        required("Picture", inputStream);

        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        byte[] pictureBytes = StreamUtils
                .toByteArray(inputStream)
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid picture provided."
                        )
                );

        user.profilePicture(ProfilePicture.of(pictureBytes, user));
        pictureRepository.put(user);
    }

    @Transactional(readOnly = true)
    public ProfilePicture profilePictureOf(String identifier) {
        User user = userRepository
                .findBy(IdentifierFactory.from(identifier))
                .orElseThrow();

        return pictureRepository
                .load(ProfilePicture.profilePicturePath(user))
                .orElseThrow(() ->
                        responseException(
                                HttpStatus.NOT_FOUND,
                                "Profile picture not found."
                        )
                );
    }
}
