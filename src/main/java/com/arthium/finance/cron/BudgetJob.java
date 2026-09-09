package com.arthium.finance.cron;

import com.arthium.finance.budget.Budget;
import com.arthium.finance.budget.BudgetRepository;
import com.arthium.finance.budget.BudgetSpendCalculator;
import com.arthium.finance.budget.BudgetStatus;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.mail.BudgetAlert;
import com.arthium.finance.mail.BudgetAlertMailer;
import com.arthium.finance.user.User;
import com.arthium.finance.user.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class BudgetJob implements UserJob {

    private static final Logger log = LoggerFactory.getLogger(BudgetJob.class);

    private final BudgetRepository budgetRepository;
    private final BudgetSpendCalculator spendCalculator;
    private final UserRepository userRepository;
    private final BudgetAlertMailer budgetAlertMailer;

    public BudgetJob(BudgetRepository budgetRepository,
                     BudgetSpendCalculator spendCalculator,
                     UserRepository userRepository,
                     BudgetAlertMailer budgetAlertMailer) {
        this.budgetRepository = budgetRepository;
        this.spendCalculator = spendCalculator;
        this.userRepository = userRepository;
        this.budgetAlertMailer = budgetAlertMailer;
    }

    @Override
    public String name() {
        return "budget";
    }

    @Override
    public void run(String userId) {
        Instant now = Instant.now();

        // The period key and label must reflect the user's local calendar month —
        // the same one the spend aggregation uses — or the two can disagree at
        // month boundaries for non-UTC timezones.
        ZonedDateTime nowLocal = spendCalculator.nowInUserZone(userId);
        String periodKey = DateUtils.periodKey(nowLocal);
        String periodLabel = DateUtils.periodLabel(nowLocal);

        log.info("Running budget check for user: {}", userId);

        try {
            List<Budget> budgets = budgetRepository.findByUserIdAndActiveTrue(new ObjectId(userId));

            if (budgets.isEmpty()) {
                log.info("No active budgets for user {}", userId);
                return;
            }

            Optional<User> user = userRepository.findById(new ObjectId(userId));
            if (user.isEmpty()) {
                log.warn("User not found: {}", userId);
                return;
            }

            Map<String, Long> spendMap = spendCalculator.currentMonthSpendByCategory(userId);

            List<Budget> toMark = new ArrayList<>();
            List<BudgetAlert> alerts = new ArrayList<>();

            for (Budget budget : budgets) {
                long spentCents = spendCalculator.spentFor(spendMap, budget.getCategory());
                long limitCents = budget.getLimitAmount();

                BudgetStatus level = levelFor(spentCents, limitCents, budget.getAlertThreshold());
                if (level == null) {
                    continue;
                }

                // De-dupe: only send when this level outranks what was already
                // alerted for the current period.
                BudgetStatus alreadyAlerted = periodKey.equals(budget.getLastAlertedPeriod())
                        ? BudgetStatus.fromValue(budget.getLastAlertedLevel())
                        : null;

                int alreadyRank = alreadyAlerted == null ? 0 : alreadyAlerted.rank();
                if (level.rank() <= alreadyRank) {
                    continue;
                }

                double percentage = MoneyUtils.round((spentCents * 100.0) / limitCents, 1);

                alerts.add(new BudgetAlert(
                        budget.getCategory(),
                        MoneyUtils.toDollars(limitCents),
                        MoneyUtils.toDollars(spentCents),
                        MoneyUtils.toDollars(limitCents - spentCents),
                        percentage,
                        level,
                        periodLabel));

                budget.setLastAlertedPeriod(periodKey);
                budget.setLastAlertedLevel(level.getValue());
                budget.setUpdatedAt(now);
                toMark.add(budget);
            }

            int sent = 0;
            List<Budget> confirmed = new ArrayList<>();

            for (int i = 0; i < alerts.size(); i++) {
                BudgetAlert alert = alerts.get(i);
                try {
                    budgetAlertMailer.send(user.get().getEmail(), user.get().getName(), alert);
                    confirmed.add(toMark.get(i));
                    sent++;
                } catch (Exception e) {
                    // Don't persist the marker if the email failed, so we retry tomorrow.
                    log.error("Budget email failed for user {} / {}", userId, alert.category(), e);
                }
            }

            if (!confirmed.isEmpty()) {
                budgetRepository.saveAll(confirmed);
            }

            log.info("Budget check done for user {} - {} alert(s) sent", userId, sent);

        } catch (Exception e) {
            log.error("Budget check failed for user {}", userId, e);
        }
    }

    private static BudgetStatus levelFor(long spentCents, long limitCents, double threshold) {
        if (limitCents <= 0) {
            return null;
        }
        double ratio = (double) spentCents / limitCents;
        if (ratio >= 1.0) {
            return BudgetStatus.EXCEEDED;
        }
        if (ratio >= threshold) {
            return BudgetStatus.WARNING;
        }
        return null;
    }
}
