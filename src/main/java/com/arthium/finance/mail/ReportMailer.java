package com.arthium.finance.mail;

import com.arthium.finance.mail.template.ReportTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportMailer {

    private final MailerService mailerService;

    public ReportMailer(MailerService mailerService) {
        this.mailerService = mailerService;
    }

    public void send(String email, String username, ReportEmailData report, String frequency) {
        String html = ReportTemplate.render(username, report, frequency);

        String text = """

                Your %s Financial Report (%s)
                Income: %s
                Expenses: %s
                Balance: %s
                Savings Rate: %.2f%%

                %s

                """.formatted(
                frequency,
                report.period(),
                MailerService.formatCurrency(report.totalIncome()),
                MailerService.formatCurrency(report.totalExpenses()),
                MailerService.formatCurrency(report.availableBalance()),
                report.savingsRate(),
                String.join(System.lineSeparator(), report.insights()));

        mailerService.send(
                email,
                frequency + " Financial Report - " + report.period(),
                text,
                html);
    }
}
