package com.arthium.finance.mail.template;

import java.time.Year;

/** Port of mailers/templates/forgot_password_template.py. */
public final class ForgotPasswordTemplate {

    private ForgotPasswordTemplate() {
    }

    public static String render(String username, String otp) {
        return TEMPLATE
                .replace("__USERNAME__", username == null ? "" : username)
                .replace("__OTP__", otp == null ? "" : otp)
                .replace("__YEAR__", String.valueOf(Year.now().getValue()));
    }

    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <title>Password Reset OTP</title>
                <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap" rel="stylesheet">
              </head>
              <body style="margin: 0; padding: 0; font-family: 'Roboto', Arial, sans-serif; background-color: #f7f7f7; font-size: 16px;">
                <table cellpadding="0" cellspacing="0" width="100%" style="background-color: #f7f7f7; padding: 20px;">
                  <tr>
                    <td>
                      <table cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 0 10px rgba(0,0,0,0.05);">

                        <tr>
                          <td style="background-color: #ff6b6b; padding: 20px 30px; color: #ffffff; text-align: center;">
                            <h2 style="margin: 0; font-size: 24px;">Password Reset Request</h2>
                          </td>
                        </tr>

                        <tr>
                          <td style="padding: 20px 30px;">
                            <p style="margin: 0 0 10px;">Hi <strong>__USERNAME__</strong>,</p>

                            <p style="margin: 0 0 20px;">
                              We received a request to reset your password. Use the OTP below to proceed:
                            </p>

                            <div style="text-align: center; margin: 30px 0;">
                              <span style="display: inline-block; padding: 15px 30px; font-size: 28px; letter-spacing: 5px; font-weight: bold; background-color: #f0f0f0; border-radius: 8px; color: #333;">
                                __OTP__
                              </span>
                            </div>

                            <p style="margin: 0 0 10px;">
                              This OTP is valid for <strong>10 minutes</strong>.
                            </p>

                            <p style="margin: 0 0 20px;">
                              If you did not request a password reset, you can safely ignore this email.
                            </p>

                            <p style="margin: 20px 0 0;">
                              Thanks,<br/>
                              <strong>Arthium Team</strong>
                            </p>
                          </td>
                        </tr>

                        <tr>
                          <td style="background-color: #f0f0f0; text-align: center; padding: 15px; font-size: 12px; color: #999;">
                            &copy; __YEAR__ Arthium. All rights reserved.
                          </td>
                        </tr>

                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """;
}
