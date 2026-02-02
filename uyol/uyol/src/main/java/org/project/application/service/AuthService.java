package org.project.application.service;

import static org.project.application.util.RestUtil.required;
import static org.project.application.util.RestUtil.responseException;

import java.util.Objects;

import org.project.application.dto.auth.*;
import org.project.domain.shared.containers.Result;
import org.project.domain.user.entities.OTP;
import org.project.domain.user.entities.User;
import org.project.domain.user.factories.IdentifierFactory;
import org.project.domain.user.repositories.OTPRepository;
import org.project.domain.user.repositories.UserRepository;
import org.project.domain.user.value_objects.*;
import org.project.infrastructure.communication.EmailInteractionService;
import org.project.infrastructure.communication.PhoneInteractionService;
import org.project.infrastructure.security.HOTPGenerator;
import org.project.infrastructure.security.PasswordEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final HOTPGenerator hotpGenerator = new HOTPGenerator();
    private final OTPRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailInteractionService emailInteractionService;
    private final PhoneInteractionService phoneInteractionService;

    public AuthService(
            UserRepository userRepository,
            OTPRepository otpRepository,
            EmailInteractionService emailInteractionService,
            PhoneInteractionService phoneInteractionService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailInteractionService = emailInteractionService;
        this.phoneInteractionService = phoneInteractionService;
        this.passwordEncoder = passwordEncoder;
    }

    /* ================= REGISTRATION ================= */

    public void registration(RegistrationForm form) {
        required("registrationForm", form);

        if (!Objects.equals(form.password(), form.passwordConfirmation()))
            throw responseException(HttpStatus.BAD_REQUEST, "Passwords do not match");

        Password.validate(form.password());

        if (form.email() != null && userRepository.isEmailExists(new Email(form.email())))
            throw responseException(HttpStatus.CONFLICT, "Email already used");

        if (form.phone() != null && userRepository.isPhoneExists(new Phone(form.phone())))
            throw responseException(HttpStatus.CONFLICT, "Phone already used");

        PersonalData data = new PersonalData(
                form.firstname(),
                form.surname(),
                form.phone(),
                passwordEncoder.encode(form.password()),
                form.email(),
                form.birthDate()
        );

        User user = User.of(data, HOTPGenerator.generateSecretKey());
        userRepository.save(user).orElseThrow();

        generateAndSendOTP(user);
    }

    /* ================= LOGIN ================= */

    public Tokens login(LoginForm form) {
        required("loginForm", form);

        User user = verifiedUserBy(form.identifier());

        if (!passwordEncoder.verify(form.password(), user.personalData().password().orElseThrow()))
            throw responseException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        return dummyTokens();
    }

    /* ================= OIDC (DISABLED) ================= */

    public Tokens oidcAuth(String idToken) {
        throw responseException(HttpStatus.NOT_IMPLEMENTED, "OIDC disabled in local dev");
    }

    /* ================= OTP ================= */

    public void resendOTP(String identifier) {
        User user = verifiedUserBy(identifier);
        generateAndSendOTP(user);
    }

    public void verification(String receivedOTP) {
        OTP.validate(receivedOTP);

        OTP otp = otpRepository.findBy(receivedOTP)
                .orElseThrow(() -> responseException(HttpStatus.NOT_FOUND, "OTP not found"));

        if (otp.isExpired())
            throw responseException(HttpStatus.GONE, "OTP expired");

        User user = userRepository.findBy(otp.userID()).orElseThrow();

        otp.confirm();
        otpRepository.updateConfirmation(otp).orElseThrow();

        user.enable();
        userRepository.updateVerification(user).orElseThrow();
    }

    /* ================= 2FA ================= */

    public void enable2FA(LoginForm form) {
        required("loginForm", form);
        User user = verifiedUserBy(form.identifier());
        generateAndSendOTP(user);
    }

    public Tokens twoFactorAuth(String otp) {
        return dummyTokens();
    }

    /* ================= PASSWORD CHANGE ================= */

    public void startPasswordChange(String identifier) {
        User user = userRepository.findBy(IdentifierFactory.from(identifier)).orElseThrow();
        generateAndSendOTP(user);
    }

    public void applyPasswordChange(PasswordChangeForm form) {
        required("passwordChangeForm", form);

        if (!Objects.equals(form.newPassword(), form.passwordConfirmation()))
            throw responseException(HttpStatus.BAD_REQUEST, "Passwords do not match");

        Password.validate(form.newPassword());

        OTP otp = otpRepository.findBy(form.otp())
                .orElseThrow(() -> responseException(HttpStatus.NOT_FOUND, "OTP not found"));

        User user = userRepository.findBy(otp.userID()).orElseThrow();
        user.changePassword(new Password(passwordEncoder.encode(form.newPassword())));
        userRepository.updatePassword(user).orElseThrow();
    }

    /* ================= REFRESH TOKEN ================= */

    public Token refreshToken(String refreshToken) {
        throw responseException(HttpStatus.NOT_IMPLEMENTED, "Refresh token disabled");
    }

    /* ================= HELPERS ================= */

    private User verifiedUserBy(String identifier) {
        Result<User, Throwable> result = userRepository.findBy(IdentifierFactory.from(identifier));
        if (result.isFailure())
            throw responseException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        User user = result.get();
        if (!user.isVerified())
            throw responseException(HttpStatus.FORBIDDEN, "Account not verified");

        return user;
    }

    private void generateAndSendOTP(User user) {
        OTP otp = OTP.of(user,
                hotpGenerator.generateHOTP(user.keyAndCounter().key(), user.keyAndCounter().counter()));

        otpRepository.save(otp).orElseThrow();
        user.incrementCounter();
        userRepository.updateCounter(user).orElseThrow();

        user.personalData().email()
                .ifPresentOrElse(
                        e -> emailInteractionService.sendOTP(otp, new Email(e)),
                        () -> phoneInteractionService.sendOTP(
                                new Phone(user.personalData().phone().orElseThrow()), otp)
                );
    }

    private Tokens dummyTokens() {
        return new Tokens("DUMMY_ACCESS_TOKEN", "DUMMY_REFRESH_TOKEN");
    }
}
