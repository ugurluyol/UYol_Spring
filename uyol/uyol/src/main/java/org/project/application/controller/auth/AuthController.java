package org.project.application.controller.auth;

import org.project.application.dto.auth.*;
import org.project.application.dto.common.Info;
import org.project.application.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registration")
    public ResponseEntity<Void> registration(@RequestBody RegistrationForm registrationForm) {
        authService.registration(registrationForm);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/oidc")
    public Tokens oidcAuth(@RequestHeader("X-ID-TOKEN") String idToken) {
        return authService.oidcAuth(idToken);
    }

    @PostMapping("/login")
    public Tokens login(@RequestBody LoginForm loginForm) {
        return authService.login(loginForm);
    }

    @GetMapping("/resend-otp")
    public ResponseEntity<Void> resendOTP(@RequestParam String identifier) {
        authService.resendOTP(identifier);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/verification")
    public ResponseEntity<Void> verification(@RequestParam String otp) {
        authService.verification(otp);
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/refresh-token")
    public Token refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    @PostMapping("/2FA")
    public Info initiate2FA(@RequestBody LoginForm loginForm) {
        authService.enable2FA(loginForm);
        return new Info("OTP sent. Please verify.");
    }

    @PatchMapping("/2FA/verification")
    public Tokens verify2FA(@RequestParam String otp) {
        return authService.twoFactorAuth(otp);
    }

    @PostMapping("/start/password/change")
    public Info startPasswordChange(@RequestParam String identifier) {
        authService.startPasswordChange(identifier);
        return new Info("Confirm OTP.");
    }

    @PatchMapping("/apply/password/change")
    public ResponseEntity<Void> applyPasswordChange(
            @RequestBody PasswordChangeForm passwordChangeForm) {
        authService.applyPasswordChange(passwordChangeForm);
        return ResponseEntity.accepted().build();
    }
}
