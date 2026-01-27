package org.project.application.controller.user;

import java.io.InputStream;

import org.project.application.dto.profile.ProfilePictureDTO;
import org.project.application.dto.profile.UserProfileDTO;
import org.project.application.service.UserProfileService;
import org.project.domain.user.value_objects.ProfilePicture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/profile")
@PreAuthorize("hasRole('USER')")
public class UserProfileController {

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserProfileDTO profile(@AuthenticationPrincipal Jwt jwt) {
        return profileService.of(jwt.getSubject());
    }

    @GetMapping("/picture")
    public ProfilePictureDTO picture(@AuthenticationPrincipal Jwt jwt) {
        ProfilePicture profilePicture =
                profileService.profilePictureOf(jwt.getSubject());

        return new ProfilePictureDTO(
                profilePicture.profilePicture(),
                profilePicture.imageType()
        );
    }

    @PutMapping(
            value = "/picture/change",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public ResponseEntity<Void> changePicture(
            @AuthenticationPrincipal Jwt jwt,
            InputStream inputStream
    ) {
        profileService.changeProfilePictureOf(jwt.getSubject(), inputStream);
        return ResponseEntity.accepted().build();
    }
}
