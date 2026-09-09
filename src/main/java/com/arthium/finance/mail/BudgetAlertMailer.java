package com.arthium.finance.mail;

import com.arthium.finance.budget.BudgetStatus;
import com.arthium.finance.mail.template.BudgetAlertTemplate;
import org.springframework.stereotype.Service;

@Service
public class BudgetAlertMailer {

    private final MailerService mailerService;

    public BudgetAlertMailer(MailerService mailerService) {
        this.mailerService = mailerService;
    }

    public void send(String email, String username, BudgetAlert alert) {
        String html = BudgetAlertTemplate.render(username, alert);
        String verb = alert.level() == BudgetStatus.EXCEEDED ? "exceeded" : "is nearly at";
        String subject = "Budget alert: " + alert.category() + " " + verb + " its limit";

        String text = """
                Hi %s,

                Your %s budget for %s %s its limit.

                Spent:     %s
                Limit:     %s
                Used:      %s%%
                Remaining: %s

                Manage your budgets anytime in Arthium.
                """.formatted(
                username,
                alert.category(),
                alert.periodLabel(),
                verb,
                MailerService.formatCurrency(alert.spent()),
                MailerService.formatCurrency(alert.limitAmount()),
                alert.percentageUsed(),
                MailerService.formatCurrency(alert.remaining()));

        mailerService.send(email, subject, text, html);
    }
}
