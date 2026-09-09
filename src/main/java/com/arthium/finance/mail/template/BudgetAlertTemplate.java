package com.arthium.finance.mail.template;

import com.arthium.finance.budget.BudgetStatus;
import com.arthium.finance.mail.BudgetAlert;
import com.arthium.finance.mail.MailerService;

/** Port of mailers/templates/budget_alert_template.py. */
public final class BudgetAlertTemplate {

    private BudgetAlertTemplate() {
    }

    public static String render(String username, BudgetAlert alert) {
        boolean exceeded = alert.level() == BudgetStatus.EXCEEDED;

        String accent = exceeded ? "#dc2626" : "#d97706";
        String headline = exceeded
                ? "You've gone over your " + alert.category() + " budget"
                : "You're close to your " + alert.category() + " budget";

        double barPercentage = Math.min(alert.percentageUsed(), 100);
        String remainingLine = alert.remaining() < 0
                ? "Over by " + MailerService.formatCurrency(Math.abs(alert.remaining()))
                : MailerService.formatCurrency(alert.remaining()) + " left";

        return TEMPLATE
                .replace("__ACCENT__", accent)
                .replace("__USERNAME__", username == null ? "" : username)
                .replace("__HEADLINE__", headline)
                .replace("__PERIOD_LABEL__", alert.periodLabel())
                .replace("__CATEGORY__", alert.category())
                .replace("__SPENT__", MailerService.formatCurrency(alert.spent()))
                .replace("__LIMIT__", MailerService.formatCurrency(alert.limitAmount()))
                .replace("__BAR_PCT__", String.valueOf(barPercentage))
                .replace("__PERCENTAGE_USED__", String.valueOf(alert.percentageUsed()))
                .replace("__REMAINING_LINE__", remainingLine);
    }

    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html>
              <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
                <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:24px 0;">
                  <tr>
                    <td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">
                        <tr>
                          <td style="background:__ACCENT__;padding:20px 28px;">
                            <span style="color:#ffffff;font-size:18px;font-weight:bold;">Arthium Budget Alert</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px;">
                            <p style="margin:0 0 8px;color:#111827;font-size:16px;">Hi __USERNAME__,</p>
                            <p style="margin:0 0 20px;color:#374151;font-size:15px;line-height:1.5;">__HEADLINE__ for <strong>__PERIOD_LABEL__</strong>.</p>

                            <table width="100%" cellpadding="0" cellspacing="0" style="background:#f9fafb;border-radius:10px;padding:18px;">
                              <tr>
                                <td style="padding:14px 18px;">
                                  <div style="color:#6b7280;font-size:13px;text-transform:uppercase;letter-spacing:.04em;">__CATEGORY__</div>
                                  <div style="color:#111827;font-size:24px;font-weight:bold;margin-top:4px;">
                                    __SPENT__ <span style="color:#9ca3af;font-size:15px;font-weight:normal;">of __LIMIT__</span>
                                  </div>
                                  <div style="background:#e5e7eb;border-radius:999px;height:10px;margin:14px 0 8px;">
                                    <div style="background:__ACCENT__;width:__BAR_PCT__%;height:10px;border-radius:999px;"></div>
                                  </div>
                                  <div style="color:__ACCENT__;font-size:14px;font-weight:bold;">__PERCENTAGE_USED__% used &middot; __REMAINING_LINE__</div>
                                </td>
                              </tr>
                            </table>

                            <p style="margin:22px 0 0;color:#6b7280;font-size:13px;line-height:1.5;">
                              This is an automated heads-up so nothing sneaks up on you. You can adjust this budget or its alert threshold anytime in Arthium.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 28px;background:#f9fafb;color:#9ca3af;font-size:12px;">
                            You're receiving this because you set a budget in Arthium.
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
