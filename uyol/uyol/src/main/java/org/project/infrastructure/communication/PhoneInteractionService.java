package org.project.infrastructure.communication;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import org.project.domain.user.entities.OTP;
import org.project.domain.user.value_objects.Phone;

import org.springframework.stereotype.Service;

@Service
public class PhoneInteractionService {

    private static final String UYOL_PHONE = "+15005550006";

    public void sendOTP(Phone phone, OTP otp) {
        Message.creator(
                new PhoneNumber(phone.phoneNumber()),
                new PhoneNumber(UYOL_PHONE),
                otp.otp()
        ).create();
    }

    public void sendMessage(Phone phone, String message) {
        Message.creator(
                new PhoneNumber(phone.phoneNumber()),
                new PhoneNumber(UYOL_PHONE),
                message
        ).create();
    }
}
