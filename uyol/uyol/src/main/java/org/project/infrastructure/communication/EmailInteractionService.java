package org.project.infrastructure.communication;

import org.project.domain.user.entities.OTP;
import org.project.domain.user.value_objects.Email;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
//@Profile("!mail")
public class EmailInteractionService {

    private final JavaMailSender mailSender;

    public static final String SOFT_VERIFICATION_SUBJECT = "You’ve been signed up on UYol";

    public static final String SOFT_VERIFICATION_BODY = """
            Hello,

            This email address was used to sign up for an account on UYol.
            If this was you, no further action is required.

            If you did not create this account, please contact our support team immediately so we can investigate and secure your information.

            Thank you,
            The UYol Team
            """;

    public static final String OTP_SUBJECT = "Your One-Time Password (OTP)";

    public static final String OTP_BODY = """
            Hello,

            Your one-time password (OTP) for account verification is:

            %s

            This code is valid for the next 5 minutes.
            If you did not request this, please ignore this email.

            Best regards,
            UYol Support Team
            """;

    public EmailInteractionService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOTP(OTP otp, Email email) {
        sendMessage(email, OTP_SUBJECT, OTP_BODY.formatted(otp.otp()));
    }

    public void sendSoftVerificationMessage(Email email) {
        sendMessage(email, SOFT_VERIFICATION_SUBJECT, SOFT_VERIFICATION_BODY);
    }

    public void sendMessage(Email email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email.email());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
