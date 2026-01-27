package org.project.infrastructure.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.project.domain.shared.exceptions.IllegalDomainArgumentException;
import org.project.domain.user.entities.User;
import org.project.domain.user.value_objects.ProfilePicture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class ProfilePictureRepository {

    private static final Logger log =
            LoggerFactory.getLogger(ProfilePictureRepository.class);

    public void put(User user) {
        ProfilePicture picture = user.profilePicture()
                .orElseThrow(() ->
                        new IllegalDomainArgumentException(
                                "Can't get profile picture. User doesn't contain profile picture."
                        )
                );

        String path = picture.path();
        byte[] pictureBytes = picture.profilePicture();

        try {
            Path profilePicturePath = Path.of(path);
            Files.createDirectories(profilePicturePath.getParent());
            Files.write(
                    profilePicturePath,
                    pictureBytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            log.info("Profile picture saved to {}", path);
        } catch (IOException e) {
            log.error("Failed to write profile picture to {}", path, e);
            throw new IllegalDomainArgumentException(
                    "Failed to save profile picture: " + e.getMessage(),
                    e
            );
        }
    }

    public Optional<ProfilePicture> load(String path) {
        Path profilePicturePath = Path.of(path);
        try {
            byte[] bytes = Files.readAllBytes(profilePicturePath);
            log.info("Successfully loaded profile picture from {}", path);
            return Optional.of(ProfilePicture.fromRepository(path, bytes));
        } catch (IOException e) {
            log.error("Failed to load profile picture from {}", path, e);
            return Optional.empty();
        }
    }
}
