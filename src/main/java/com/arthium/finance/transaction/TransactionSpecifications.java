package com.arthium.finance.transaction;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/** Composable {@link Specification} filters, mirroring the dynamic Mongo {@code Criteria} sets these queries used to build by hand. */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> userIdEquals(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Transaction> dateBetween(Instant from, Instant to) {
        if (from == null || to == null) {
            return null;
        }
        return (root, query, cb) -> cb.between(root.get("date"), from, to);
    }

    public static Specification<Transaction> typeEquals(TransactionType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> categoryContainsIgnoreCase(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String pattern = "%" + category.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("category")), pattern);
    }

    public static Specification<Transaction> titleOrCategoryContainsIgnoreCase(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("category")), pattern));
    }

    public static Specification<Transaction> recurringEquals(boolean recurring) {
        return (root, query, cb) -> cb.equal(root.get("recurring"), recurring);
    }

    @SafeVarargs
    public static Specification<Transaction> combine(Specification<Transaction>... specs) {
        Specification<Transaction> combined = null;
        for (Specification<Transaction> spec : specs) {
            if (spec == null) {
                continue;
            }
            combined = combined == null ? spec : combined.and(spec);
        }
        return combined;
    }
}
