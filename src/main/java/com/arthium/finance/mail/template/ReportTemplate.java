package com.arthium.finance.mail.template;

import com.arthium.finance.mail.MailerService;
import com.arthium.finance.mail.ReportEmailData;
import com.arthium.finance.report.dto.TopCategory;

import java.time.Year;
import java.util.Locale;

public final class ReportTemplate {

    private ReportTemplate() {
    }

    public static String render(String username, ReportEmailData data, String frequency) {
        String reportTitle = capitalizeFirstLetter(frequency) + "-Report";

        StringBuilder categoryList = new StringBuilder();
        for (TopCategory category : data.topCategories()) {
            categoryList.append("<li>")
                    .append(category.name())
                    .append(" - ")
                    .append(MailerService.formatCurrency(category.amount()))
                    .append(" (")
                    .append(trimNumber(category.percent()))
                    .append("%)</li>");
        }

        StringBuilder insightsList = new StringBuilder();
        for (String insight : data.insights()) {
            insightsList.append("<li>").append(insight).append("</li>");
        }

        return TEMPLATE
                .replace("__REPORT_TITLE__", reportTitle)
                .replace("__USERNAME__", username == null ? "" : username)
                .replace("__PERIOD__", data.period())
                .replace("__TOTAL_INCOME__", MailerService.formatCurrency(data.totalIncome()))
                .replace("__TOTAL_EXPENSES__", MailerService.formatCurrency(data.totalExpenses()))
                .replace("__AVAILABLE_BALANCE__", MailerService.formatCurrency(data.availableBalance()))
                .replace("__SAVINGS_RATE__", String.format(Locale.US, "%.2f", data.savingsRate()))
                .replace("__CATEGORY_LIST__", categoryList.toString())
                .replace("__INSIGHTS_LIST__", insightsList.toString())
                .replace("__YEAR__", String.valueOf(Year.now().getValue()));
    }

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private static String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <title>__REPORT_TITLE__</title>
                <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap" rel="stylesheet">
              </head>
              <body style="margin: 0; padding: 0; font-family: 'Roboto', Arial, sans-serif; background-color: #f7f7f7; font-size: 16px;">
                <table cellpadding="0" cellspacing="0" width="100%" style="background-color: #f7f7f7; padding: 20px;">
                  <tr>
                    <td>
                      <table cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 0 10px rgba(0,0,0,0.05);">
                        <tr>
                          <td style="background-color: #00bc7d; padding: 20px 30px; color: #ffffff; text-align: center;">
                            <h2 style="margin: 0; font-size: 24px; text-transform: capitalize">__REPORT_TITLE__</h2>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding: 20px 30px;">
                            <p style="margin: 0 0 10px; font-size: 16px;">Hi <strong>__USERNAME__</strong>,</p>
                            <p style="margin: 0 0 20px; font-size: 16px;">Here's your financial summary for <strong>__PERIOD__</strong>.</p>

                            <table width="100%" style="border-collapse: collapse;">
                              <tr>
                                <td style="padding: 8px 0; font-size: 16px;"><strong>Total Income:</strong></td>
                                <td style="text-align: right; font-size: 16px;">__TOTAL_INCOME__</td>
                              </tr>
                              <tr>
                                <td style="padding: 8px 0; font-size: 16px;"><strong>Total Expenses:</strong></td>
                                <td style="text-align: right; font-size: 16px;">__TOTAL_EXPENSES__</td>
                              </tr>
                              <tr>
                                <td style="padding: 8px 0; font-size: 16px;"><strong>Available Balance:</strong></td>
                                <td style="text-align: right; font-size: 16px;">__AVAILABLE_BALANCE__</td>
                              </tr>
                              <tr>
                                <td style="padding: 8px 0; font-size: 16px;"><strong>Savings Rate:</strong></td>
                                <td style="text-align: right; font-size: 16px;">__SAVINGS_RATE__%</td>
                              </tr>
                            </table>
                            <hr style="margin: 20px 0; border: none; border-top: 1px solid #e0e0e0;" />
                            <h4 style="margin: 0 0 10px; font-size: 16px;">Top Spending Categories</h4>
                            <ul style="padding-left: 20px; margin: 0; font-size: 16px;">
                              __CATEGORY_LIST__
                            </ul>
                            <hr style="margin: 20px 0; border: none; border-top: 1px solid #e0e0e0;" />
                            <h4 style="margin: 0 0 10px; font-size: 16px;">Insights</h4>
                            <ul style="padding-left: 20px; margin: 0; font-size: 16px;">
                              __INSIGHTS_LIST__
                            </ul>
                            <p style="margin-top: 30px; font-size: 13px; color: #888;">This report was generated automatically based on your recent activity.</p>
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
