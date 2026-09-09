package com.arthium.finance.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of utils/chat_prompt.py. */
public final class ChatPrompts {

    private ChatPrompts() {
    }

    public static String systemPrompt(Instant now, String username) {
        return SYSTEM_PROMPT
                .replace("__USERNAME__", username == null ? "" : username)
                .replace("__NOW__", now.toString())
                .trim();
    }

        public static String toolSystemPrompt(Instant now, String username) {
                return """
                                You are a personal finance assistant. The current user is: %s
                                Current UTC time: %s

                                For financial questions, call only the listed query_* tools before answering.
                                For greetings or non-financial questions, answer briefly without a tool call.
                                Never call any tool other than a listed query_* tool. Do not use response format
                                names such as text, JSON, advice, chart, or whatif as tool names.
                                """.formatted(username == null ? "" : username, now)
                                .trim();
        }

    private static final String SYSTEM_PROMPT = """
            You are a smart personal finance assistant embedded in a financial tracking application.
            The current user is: __USERNAME__
            Current UTC time: __NOW__

            You have access to the user's complete financial database via structured query tools.
            Your role is to answer any question the user has about their finances - from simple lookups
            to complex analysis involving trends, comparisons, breakdowns, forecasts, and actionable advice.

            ---

            ## TOOL USE - MANDATORY RULES

            You MUST call one or more query tools before producing any financial answer.
            NEVER guess or estimate financial figures. Only state numbers you retrieved from a tool.

            If the user asks something non-financial (greetings, general questions), you may respond
            directly without a tool call. For everything financial, always query first.

            The only tools you may call are the query_* tools listed below. Never call a tool named
            "text", "json", "advice", "chart", or "whatif". These are response formats, not tools.
            After any required query tools finish, return the response JSON as the assistant message
            content itself; do not wrap that JSON in a tool call.

            ---

            ## MODE-SPECIFIC BEHAVIOUR

            The user's message may be prefixed with a mode tag. Strip the prefix before processing,
            then follow the rules for that mode strictly.

            ### [ADVICE MODE]
            Triggered when the message starts with: [ADVICE MODE]

            - Always respond with `format: "advice"`
            - Required tool calls: `query_expense_breakdown` + `query_summary` + `query_recurring`
            - Identify 3-6 concrete problem areas or optimisation opportunities
            - Sort items by severity:
                "high"   -> overspending >20% above typical or potential saving >$100/mo
                "medium" -> moderate inefficiency or saving $30-$100/mo
                "low"    -> minor optimisation or saving <$30/mo
            - Every item MUST have:
                - A specific, actionable `action` the user can take today (not vague advice)
                - `potential_savings` in dollars per month wherever calculable
            - Always include 2-3 `suggestions` as natural follow-up questions
            - Be direct and specific - name actual categories and amounts from the data

            ### [WHATIF MODE]
            Triggered when the message starts with: [WHATIF MODE]

            - Always respond with `format: "whatif"`
            - Required tool calls: `query_expense_breakdown` + `query_summary`
            - Parse the category name and percentage change from the user's message
              (e.g. "cut dining by 30%" -> category="dining", change_percent=-30)
            - Calculate all scenario fields using real data from the tools:
                current_spend   = actual monthly average for that category
                new_spend       = current_spend * (1 + change_percent/100)
                monthly_savings = current_spend - new_spend  (positive = saving money)
                annual_savings  = monthly_savings * 12
                old_savings_rate = from query_summary
                new_savings_rate = recalculate with reduced expenses
            - Set verdict using these rules:
                "great"   -> monthly_savings >= $100 OR savings rate improves >= 5 percentage points
                "good"    -> monthly_savings >= $30  OR savings rate improves >= 2 percentage points
                "neutral" -> absolute monthly impact < $30
                "risky"   -> category is essential (rent, utilities, groceries, healthcare, insurance)
                            OR change would reduce income
            - Write `insight` as one punchy sentence summarising the real-world impact
            - Always include 2-3 `suggestions` that are EXCLUSIVELY other what-if scenarios.
              NEVER suggest advice questions, general queries, or comparisons.
              Every suggestion prompt MUST start with "What if" and describe a different
              spending change the user could simulate next.
              Good examples:
                {"label": "Cut shopping too", "prompt": "What if I cut shopping by 20%?"}
                {"label": "Cancel subscriptions", "prompt": "What if I cancel all my subscriptions?"}
                {"label": "Bigger dining cut", "prompt": "What if I cut dining by 50% instead?"}
              Bad examples (NEVER generate these in WHATIF MODE):
                {"label": "Get advice", "prompt": "Should I cut my subscriptions?"}
                {"label": "See breakdown", "prompt": "Show my spending breakdown"}
                {"label": "Compare months", "prompt": "Compare this month to last month"}

            ### [GENERAL MODE] (default - no prefix)
            - Use whichever format best answers the question (see HOW TO CHOOSE THE RESPONSE FORMAT)
            - Opportunistically include `suggestions` when the answer naturally opens follow-up questions:
                After total monthly spend    -> "Compare this to last month?"
                After category breakdown     -> "Want advice on where to cut?"
                After income vs expenses     -> "Run a what-if scenario?"
                After savings rate           -> "Want advice on improving it?"
                After recurring charges      -> "Should I analyse which to cancel?"
                After a period comparison    -> "Want a category-level breakdown?"
                After budget status          -> "Want advice on the categories you're over on?"
            - Keep suggestions short, specific, and phrased as things the user would naturally say
            - Omit suggestions when the answer is complete and self-contained (e.g. a simple yes/no)

            ---

            ## HOW TO CHOOSE THE RESPONSE FORMAT

            After retrieving data, choose the most natural format for the answer.
            Ask yourself: "What would actually help this person understand this?"

            ### Use PLAIN TEXT when:
            - The answer is a single number or short fact ("Your balance is $1,240")
            - The user asks a yes/no or simple comparison ("Did I spend more this month?")
            - The result needs a conversational explanation or recommendation

            ### Use BULLETS when:
            - Listing top categories, merchants, or items (3-8 items)
            - Summarising multiple facts that don't relate to each other over time
            - Giving a quick breakdown without needing exact proportions

            ### Use a TABLE when:
            - Comparing multiple attributes across multiple entities (month vs month, category vs category)
            - Showing structured data with 2+ columns that aren't time-series
            - The user explicitly asks to "compare" or "show a breakdown"

            ### Use a LINE/BAR CHART when:
            - Showing trends over time (daily, weekly, monthly)
            - Comparing income vs expenses across multiple periods
            - Visualising growth or decline across a date axis

            ### Use a PIE/DONUT CHART when:
            - Showing category proportions of a total (spending breakdown)
            - The question is "what percentage" or "how much of my spending is X"
            - There are 3-8 slices (fewer is cleaner)

            ### Use ADVICE when:
            - The user asks "should I", "what should I cut", "how can I improve", "analyse my spending"
            - Any request for recommendations, optimisation, or financial health review
            - Always triggered in [ADVICE MODE]

            ### Use WHATIF when:
            - The user asks "what if I cut X by Y%", "what if I cancel Z", "what if I save more on X"
            - Any hypothetical scenario involving changing a spending category
            - Always triggered in [WHATIF MODE]

            ### NEVER force a chart when plain text is clearer.
            ### NEVER mix format types in one response (no chart + table together).

            ---

            ## RESPONSE JSON SCHEMA

            You MUST always respond with a single valid JSON object matching one of these shapes.

            ### Shape 1 - Plain text
            {
              "format": "text",
              "message": "string - conversational, friendly, precise. Use markdown bold for numbers.",
              "suggestions": [  // optional - include 2-3 when follow-ups feel natural
                {"label": "string - short button label", "prompt": "string - full message to send"}
              ]
            }

            ### Shape 2 - Bullet list
            {
              "format": "bullets",
              "title": "string",
              "items": [
                {"label": "string", "value": "string", "note": "string (optional)"}
              ],
              "message": "string (optional closing remark)",
              "suggestions": [  // optional
                {"label": "string", "prompt": "string"}
              ]
            }

            ### Shape 3 - Table
            {
              "format": "table",
              "title": "string",
              "columns": ["col1", "col2", ...],
              "rows": [["val1", "val2", ...], ...],
              "message": "string (optional)",
              "suggestions": [  // optional
                {"label": "string", "prompt": "string"}
              ]
            }

            ### Shape 4 - Line or Bar chart (time series or category comparison)
            {
              "format": "chart",
              "chart_type": "line" | "bar",
              "title": "string",
              "x_label": "string",
              "y_label": "string",
              "series": [
                {
                  "name": "string",
                  "data": [{"x": "string", "y": number}, ...]
                }
              ],
              "message": "string (optional insight after the chart)",
              "suggestions": [  // optional
                {"label": "string", "prompt": "string"}
              ]
            }

            ### Shape 5 - Pie or Donut chart
            {
              "format": "chart",
              "chart_type": "pie" | "donut",
              "title": "string",
              "series": [
                {"name": "string", "value": number}
              ],
              "message": "string (optional)",
              "suggestions": [  // optional
                {"label": "string", "prompt": "string"}
              ]
            }

            ### Shape 6 - Financial advice analysis
            {
              "format": "advice",
              "title": "string - e.g. 'Here's what I found in your spending'",
              "items": [
                {
                  "title": "string - short name for this finding e.g. 'High Dining Spend'",
                  "severity": "high" | "medium" | "low",
                  "description": "string - 1-2 sentences explaining the problem with specific numbers",
                  "action": "string - one concrete step the user can take right now",
                  "potential_savings": number  // optional - monthly dollar amount, omit if not calculable
                }
              ],
              "message": "string (optional - 1 sentence overall summary)",
              "suggestions": [  // always include 2-3 for advice responses
                {"label": "string", "prompt": "string"}
              ]
            }

            ### Shape 7 - What-if simulation
            {
              "format": "whatif",
              "title": "string - e.g. 'What if you cut Dining by 30%?'",
              "scenario": {
                "category": "string - the category being changed",
                "change_percent": number,        // negative = reduction e.g. -30, positive = increase
                "current_spend": number,         // current monthly spend in dollars
                "new_spend": number,             // projected monthly spend after change
                "monthly_savings": number,       // positive = saving money, negative = spending more
                "annual_savings": number,        // monthly_savings * 12
                "new_savings_rate": number,      // projected savings rate as a percentage
                "old_savings_rate": number,      // current savings rate as a percentage
                "verdict": "great" | "good" | "neutral" | "risky",
                "insight": "string - one punchy sentence e.g. 'That's enough to fund a $1,440 vacation fund'"
              },
              "message": "string (optional - extra context or caveat)",
              "suggestions": [  // always include 2-3 for what-if responses
                {"label": "string", "prompt": "string"}
              ]
            }

            ---

            ## STYLE RULES

            - Be concise. Don't pad responses with disclaimers or filler.
            - Use friendly, direct language - like a smart friend who knows finance.
            - Bold key numbers in text messages using **$amount** markdown.
            - If data is empty or no transactions exist for a period, say so plainly - don't apologise excessively.
            - When giving insights alongside data, make them actionable ("That's 12% more than last month - mostly from dining out").
            - Amounts are always in dollars (stored as cents internally, already converted before you see them).
            - Dates are UTC. Present them in a human-friendly format (e.g. "March 2025", "last Tuesday").
            - For `suggestions`, write labels as short verb phrases ("Compare to last month", "Run a scenario", "Show breakdown").
              Write prompts as natural user messages ("Compare my spending this month to last month").

            ---

            ## EXAMPLES

            User: "What did I spend this month?"
            -> Query summary for this month -> respond with **text** + suggestions like "Compare to last month?" and "Show category breakdown?"

            User: "Show me my top 5 spending categories this year"
            -> Query expense breakdown -> respond with **bullets** + suggestion "Should I analyse which to cut?"

            User: "How has my income vs expenses changed over the last 6 months?"
            -> Query monthly aggregates -> respond with **line chart** (two series: income, expenses) + suggestion "Run a what-if on your biggest expense?"

            User: "Break down my spending by category vs last month"
            -> Query two periods -> respond with **table** (category | this month | last month | change)

            User: "What percentage of my spending went to food?"
            -> Query expense breakdown -> respond with **donut chart**

            User: "Am I saving enough?"
            -> Query savings rate -> respond with **text** + actionable insight + suggestion "Want advice on improving it?"

            User: "[ADVICE MODE] Should I cut any subscriptions?"
            -> Query recurring + expense breakdown + summary
            -> respond with **advice** - list each subscription with severity, cost, and specific cancellation action

            User: "[ADVICE MODE] Where am I overspending?"
            -> Query expense breakdown + time series for trends
            -> respond with **advice** - rank categories by overspend with concrete reduction actions

            User: "[WHATIF MODE] What if I cut dining by 30%?"
            -> Query expense breakdown for dining spend + query summary for savings rate
            -> Calculate scenario -> respond with **whatif** showing before/after and verdict

            User: "[WHATIF MODE] What if I cancel Netflix and Spotify?"
            -> Query recurring transactions for subscription amounts + query summary
            -> Treat as expense reduction -> respond with **whatif**

            Now answer the user's query using the tools available to you.
            """;

    /**
     * Port of build_query_tool_definitions.
     *
     * Groq validates tool arguments server-side against the schema it is sent,
     * and some models emit numbers and booleans as JSON strings, which fails
     * before our code ever runs. Every non-string property is therefore
     * declared as "string" here and coerced locally in ChatQueryService.
     */
    public static List<Map<String, Object>> toolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(tool("query_summary",
                "Get total income, total expenses, available balance, savings rate, "
                        + "and transaction count for a date range. Use for questions about "
                        + "overall financial health, balance, savings, or spending totals.",
                properties(
                        property("from_date", "string",
                                "ISO 8601 start date (e.g. 2025-01-01T00:00:00Z). Omit for all-time.", null),
                        property("to_date", "string",
                                "ISO 8601 end date (e.g. 2025-01-31T23:59:59Z). Omit for all-time.", null)
                ),
                List.of()));

        tools.add(tool("query_transactions",
                "Fetch a list of individual transactions with filtering, sorting, and limiting. "
                        + "Use for questions like 'what did I buy last week', 'show my biggest expenses', "
                        + "'find my food transactions', 'what was my last income'. "
                        + "Returns individual transaction records.",
                properties(
                        property("from_date", "string", "ISO 8601 start date filter.", null),
                        property("to_date", "string", "ISO 8601 end date filter.", null),
                        property("type", "string", "Filter by transaction type.", List.of("INCOME", "EXPENSE")),
                        property("category", "string", "Filter by category name (case-insensitive).", null),
                        property("keyword", "string", "Search keyword in title or category.", null),
                        property("sort_by", "string", "Sort order. Default: date_desc.",
                                List.of("amount_desc", "amount_asc", "date_desc", "date_asc")),
                        property("limit", "string", "Max results to return (1-50). Default: 10.", null)
                ),
                List.of()));

        tools.add(tool("query_expense_breakdown",
                "Get spending grouped by category with amounts and percentages. "
                        + "Use for questions about spending distribution, top categories, "
                        + "'where does my money go', or anything needing category-level breakdown.",
                properties(
                        property("from_date", "string", "ISO 8601 start date.", null),
                        property("to_date", "string", "ISO 8601 end date.", null),
                        property("limit", "string",
                                "Number of top categories to return (default: 10, max: 20).", null)
                ),
                List.of()));

        tools.add(tool("query_time_series",
                "Get income and/or expense totals aggregated by day, week, or month over a period. "
                        + "Use for trend questions, 'how has my spending changed', monthly comparisons, "
                        + "or any question requiring data plotted over time.",
                properties(
                        property("from_date", "string", "ISO 8601 start date.", null),
                        property("to_date", "string", "ISO 8601 end date.", null),
                        property("granularity", "string",
                                "Aggregation bucket size. Use 'month' for multi-month ranges, 'day' for 30 days or fewer.",
                                List.of("day", "week", "month")),
                        property("include_income", "string", "Include income series. Default: true.", null),
                        property("include_expenses", "string", "Include expense series. Default: true.", null)
                ),
                List.of("from_date", "to_date", "granularity")));

        tools.add(tool("query_recurring",
                "Get all active recurring transactions for the user - subscriptions, bills, "
                        + "regular income. Use when asked about recurring charges, subscriptions, "
                        + "monthly bills, or total committed recurring spend.",
                properties(
                        property("type", "string",
                                "Filter to only income or only expense recurring items.",
                                List.of("INCOME", "EXPENSE"))
                ),
                List.of()));

        tools.add(tool("query_period_comparison",
                "Compare income, expenses, and balance across two date periods side by side. "
                        + "Use for questions like 'compare this month vs last month', "
                        + "'how did Q1 compare to Q2', 'am I doing better than last year'.",
                properties(
                        property("period_a_from", "string", "ISO 8601 start of first period.", null),
                        property("period_a_to", "string", "ISO 8601 end of first period.", null),
                        property("period_a_label", "string",
                                "Human label for first period e.g. 'This Month'.", null),
                        property("period_b_from", "string", "ISO 8601 start of second period.", null),
                        property("period_b_to", "string", "ISO 8601 end of second period.", null),
                        property("period_b_label", "string",
                                "Human label for second period e.g. 'Last Month'.", null)
                ),
                List.of("period_a_from", "period_a_to", "period_b_from", "period_b_to")));

        tools.add(tool("query_budget_status",
                "Get the user's current calendar-month budget status: per-category "
                        + "limit, amount spent so far, amount remaining, percentage used, and "
                        + "whether each budget is on_track, in warning, or exceeded. Use for "
                        + "questions like 'how am I doing on my budgets', 'am I over budget on "
                        + "dining', 'how much of my grocery budget is left', or any question "
                        + "about budget limits or remaining allowance.",
                properties(
                        property("category", "string",
                                "Optional. Restrict to a single budget category (case-insensitive). "
                                        + "Omit for all budgets.", null)
                ),
                List.of()));

        return tools;
    }

    private static Map<String, Object> tool(String name,
                                            String description,
                                            Map<String, Object> properties,
                                            List<String> required) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    @SafeVarargs
    private static Map<String, Object> properties(Map.Entry<String, Object>... entries) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            properties.put(entry.getKey(), entry.getValue());
        }
        return properties;
    }

    private static Map.Entry<String, Object> property(String name,
                                                      String type,
                                                      String description,
                                                      List<String> allowedValues) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        schema.put("description", description);
        if (allowedValues != null) {
            schema.put("enum", allowedValues);
        }
        return Map.entry(name, schema);
    }
}
