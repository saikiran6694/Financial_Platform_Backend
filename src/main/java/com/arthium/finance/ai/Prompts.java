package com.arthium.finance.ai;

import com.arthium.finance.transaction.PaymentMethod;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** Port of utils/prompts.py. */
public final class Prompts {

    private Prompts() {
    }

    public static final String PAYMENT_METHODS = Arrays.stream(PaymentMethod.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    public static final String RECEIPT_PROMPT = """
            You are a financial assistant that helps users analyze and extract transaction details from receipt image (base64 encoded)
            Analyze this receipt image (base64 encoded) and extract transaction details matching this exact JSON format:
            {
              "title": "string",          // Merchant/store name or brief description
              "amount": number,           // Total amount (positive number)
              "date": "ISO date string",  // Transaction date in YYYY-MM-DD format
              "description": "string",    // Items purchased summary (max 50 words)
              "category": "string",       // category of the transaction
              "type": "EXPENSE"           // Always "EXPENSE" for receipts
              "paymentMethod": "string",  // One of: __PAYMENT_METHODS__
            }

            Rules:
            1. Amount must be positive
            2. Date must be valid and in ISO format. If in case date is not present in the image please add the current date in ISO format
            3. Payment method must match enum values of type __PAYMENT_METHODS__
            4. If uncertain about any field, omit it
            5. If not a receipt, return {}

            Example valid response:
            {
              "title": "Walmart Groceries",
              "amount": 58.43,
              "date": "2025-05-08",
              "description": "Groceries: milk, eggs, bread",
              "category": "groceries",
              "paymentMethod": "CARD",
              "type": "EXPENSE"
            }
            """.replace("__PAYMENT_METHODS__", PAYMENT_METHODS);

    /** Port of report_insight_prompt. `categories` maps name -> {amount, percentage}. */
    public static String reportInsightPrompt(double totalIncome,
                                             double totalExpenses,
                                             double availableBalance,
                                             double savingsRate,
                                             Map<String, Map<String, Object>> categories,
                                             String periodLabel) {

        String categoryList = categories.entrySet().stream()
                .map(entry -> "- " + entry.getKey() + ": " + entry.getValue().get("amount")
                        + " (" + entry.getValue().get("percentage") + "%)")
                .collect(Collectors.joining("\n"));

        return """
                You are a friendly and smart financial coach, not a robot.

                Your job is to give **exactly 3 good short insights** to the user based on their data that feel like you're talking to them directly.

                Each insight should reflect the actual data and sound like something a smart money coach would say based on the data - short, clear, and practical.

                Report for: __PERIOD__
                - Total Income: $__INCOME__
                - Total Expenses: $__EXPENSES__
                - Available Balance: $__BALANCE__
                - Savings Rate: __SAVINGS_RATE__%

                Top Expense Categories:
                __CATEGORIES__

                Guidelines:
                - Keep each insight to one short, realistic, personalized, natural sentence
                - Use conversational language, correct wordings & Avoid sounding robotic, or generic
                - Include specific data when helpful and comma to amount
                - Be encouraging if user spent less than they earned
                - Format your response **exactly** like this:

                ["Insight 1", "Insight 2", "Insight 3"]

                Example:
                [
                  "Nice! You kept $7,458 after expenses - that's solid breathing room.",
                  "You spent the most on 'Meals' this period - 32%. Maybe worth keeping an eye on.",
                  "You stayed under budget this time. That's a win - keep the momentum"
                ]

                Output only a **JSON array of 3 strings**. Do not include any explanation, markdown, or notes.
                """
                .replace("__PERIOD__", periodLabel)
                .replace("__INCOME__", String.format("%.2f", totalIncome))
                .replace("__EXPENSES__", String.format("%.2f", totalExpenses))
                .replace("__BALANCE__", String.format("%.2f", availableBalance))
                .replace("__SAVINGS_RATE__", String.valueOf(savingsRate))
                .replace("__CATEGORIES__", categoryList)
                .trim();
    }
}
