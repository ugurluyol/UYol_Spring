package org.project.application.service;

import static org.project.application.util.RestUtil.required;
import static org.project.application.util.RestUtil.responseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
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
import org.project.infrastructure.security.JWTUtility;
import org.project.infrastructure.security.PasswordEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final JWTUtility jwtUtility;
    private final HOTPGenerator hotpGenerator;
    private final OTPRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailInteractionService emailInteractionService;
    private final PhoneInteractionService phoneInteractionService;

    public AuthService(
            JWTUtility jwtUtility,
            UserRepository userRepository,
            OTPRepository otpRepository,
            EmailInteractionService emailInteractionService,
            PhoneInteractionService phoneInteractionService,
            PasswordEncoder passwordEncoder
    ) {
        this.jwtUtility = jwtUtility;
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailInteractionService = emailInteractionService;
        this.phoneInteractionService = phoneInteractionService;
        this.passwordEncoder = passwordEncoder;
        this.hotpGenerator = new HOTPGenerator();
    }

    /* ================= REGISTRATION ================= */

    public void registration(RegistrationForm registrationForm) {
        required("registrationForm", registrationForm);

        if (!Objects.equals(registrationForm.password(), registrationForm.passwordConfirmation()))
            throw responseException(HttpStatus.BAD_REQUEST, "Passwords do not match");

        Password.validate(registrationForm.password());

        if (registrationForm.email() != null && userRepository.isEmailExists(new Email(registrationForm.email())))
            throw responseException(HttpStatus.CONFLICT, "Email already used");

        if (registrationForm.phone() != null && userRepository.isPhoneExists(new Phone(registrationForm.phone())))
            throw responseException(HttpStatus.CONFLICT, "Phone already used");

        PersonalData personalData = new PersonalData(
                registrationForm.firstname(),
                registrationForm.surname(),
                registrationForm.phone(),
                passwordEncoder.encode(registrationForm.password()),
                registrationForm.email(),
                registrationForm.birthDate()
        );

        User user = User.of(personalData, HOTPGenerator.generateSecretKey());

        userRepository.save(user)
                .orElseThrow(() -> responseException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to register user"));

        generateAndSendOTP(user);
    }

    /* ================= LOGIN ================= */

    public Tokens login(LoginForm form) {
        required("loginForm", form);

        User user = verifiedUserBy(form.identifier());

        if (!passwordEncoder.verify(form.password(), user.personalData().password().orElseThrow()))
            throw responseException(HttpStatus.UNAUTHORIZED, "Invalid credentials");

        Tokens tokens = generateTokens(user);
        userRepository.saveRefreshToken(new RefreshToken(user.id(), tokens.refreshToken()))
                .orElseThrow(() -> responseException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot save refresh token"));

        return tokens;
    }

    /* ================= OIDC ================= */

    public Tokens oidcAuth(String idToken) {
        try {
            Jwt claims = jwtUtility.verifyAndParse(idToken)
                    .orElseThrow(() -> responseException(HttpStatus.FORBIDDEN, "Invalid id token"));

            Email email = new Email(claims.getClaimAsString("email"));
            User user = userRepository.isEmailExists(email)
                    ? userRepository.findBy(email).orElseThrow()
                    : registerNonExistedUser(claims, email);

            Tokens tokens = generateTokens(user);
            userRepository.saveRefreshToken(new RefreshToken(user.id(), tokens.refreshToken()))
                    .orElseThrow(() -> responseException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to authenticate"));

            return tokens;

        } catch (DateTimeParseException e) {
            throw responseException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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

    public void enable2FA(LoginForm loginForm) {
        required("loginForm", loginForm);

        User user = verifiedUserBy(loginForm.identifier());

        if (!passwordEncoder.verify(loginForm.password(), user.personalData().password().orElseThrow()))
            throw responseException(HttpStatus.BAD_REQUEST, "Password does not match");

        if (otpRepository.contains(user.id()))
            throw responseException(HttpStatus.BAD_REQUEST, "2FA already requested");

        generateAndSendOTP(user);
    }

    public Tokens twoFactorAuth(String otpValue) {
        OTP.validate(otpValue);

        OTP otp = otpRepository.findBy(otpValue)
                .orElseThrow(() -> responseException(HttpStatus.NOT_FOUND, "OTP not found"));

        if (otp.isExpired())
            throw responseException(HttpStatus.GONE, "OTP expired");

        User user = userRepository.findBy(otp.userID()).orElseThrow();

        otp.confirm();
        otpRepository.updateConfirmation(otp).orElseThrow();

        if (!user.is2FAEnabled()) {
            user.enable2FA();
            userRepository.update2FA(user).orElseThrow();
        }

        Tokens tokens = generateTokens(user);
        userRepository.saveRefreshToken(new RefreshToken(user.id(), tokens.refreshToken()))
                .orElseThrow();

        return tokens;
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

        if (otp.isExpired())
            throw responseException(HttpStatus.GONE, "OTP expired");

        User user = userRepository.findBy(otp.userID()).orElseThrow();

        user.changePassword(new Password(passwordEncoder.encode(form.newPassword())));
        userRepository.updatePassword(user).orElseThrow();
    }

    /* ================= REFRESH TOKEN ================= */

    public Token refreshToken(String refreshToken) {
        RefreshToken pair = userRepository.findRefreshToken(refreshToken)
                .orElseThrow(() -> responseException(HttpStatus.NOT_FOUND, "Refresh token not found"));

        Jwt jwt = jwtUtility.parse(pair.refreshToken())
                .orElseThrow(() -> responseException(HttpStatus.BAD_REQUEST, "Invalid refresh token"));

        LocalDateTime expiresAt = jwt.getExpiresAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();

        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(expiresAt))
            throw responseException(HttpStatus.BAD_REQUEST, "Refresh token expired");

        User user = userRepository.findBy(pair.userID()).orElseThrow();
        return new Token(jwtUtility.generateToken(user));
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

    private User registerNonExistedUser(Jwt claims, Email email) {
        PersonalData data = new PersonalData(
                claims.getClaimAsString("firstname"),
                claims.getClaimAsString("lastname"),
                null,
                null,
                email.email(),
                LocalDate.parse(claims.getClaimAsString("birthDate"))
        );

        User user = User.of(data, HOTPGenerator.generateSecretKey());
        userRepository.save(user).orElseThrow();
        emailInteractionService.sendSoftVerificationMessage(email);
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
                        () -> phoneInteractionService.sendOTP(new Phone(user.personalData().phone().orElseThrow()), otp)
                );
    }

    private Tokens generateTokens(User user) {
        return new Tokens(
                jwtUtility.generateToken(user),
                jwtUtility.generateRefreshToken(user)
        );
    }
}
