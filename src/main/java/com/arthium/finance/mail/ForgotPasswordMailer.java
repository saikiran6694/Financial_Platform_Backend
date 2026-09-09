package com.arthium.finance.mail;

import com.arthium.finance.mail.template.ForgotPasswordTemplate;
import org.springframework.stereotype.Service;

@Service
public class ForgotPasswordMailer {

    private final MailerService mailerService;

    public ForgotPasswordMailer(MailerService mailerService) {
        this.mailerService = mailerService;
    }

    public void send(String email, String username, String otp) {
        mailerService.send(
                email,
                "Password Reset OTP",
                "",
                ForgotPasswordTemplate.render(username, otp));
    }
}
