package com.arthium.finance.transaction;

import com.arthium.finance.ai.GeminiService;
import com.arthium.finance.ai.Prompts;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.DateUtils;
import com.arthium.finance.common.Json;
import com.arthium.finance.common.MoneyUtils;
import com.arthium.finance.common.PaginationDto;
import com.arthium.finance.storage.CloudinaryService;
import com.arthium.finance.transaction.dto.BulkTransactionCreateRequest;
import com.arthium.finance.transaction.dto.ScanReceiptResponse;
import com.arthium.finance.transaction.dto.TransactionCreateRequest;
import com.arthium.finance.transaction.dto.TransactionListResponse;
import com.arthium.finance.transaction.dto.TransactionResponse;
import com.arthium.finance.transaction.dto.TransactionUpdateRequest;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MongoTemplate mongoTemplate;
    private final CloudinaryService cloudinaryService;
    private final GeminiService geminiService;
    private final Json json;

    public TransactionService(TransactionRepository transactionRepository,
                              MongoTemplate mongoTemplate,
                              CloudinaryService cloudinaryService,
                              GeminiService geminiService,
                              Json json) {
        this.transactionRepository = transactionRepository;
        this.mongoTemplate = mongoTemplate;
        this.cloudinaryService = cloudinaryService;
        this.geminiService = geminiService;
        this.json = json;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    public TransactionResponse create(TransactionCreateRequest request, String userId) {
        Instant now = Instant.now();
        Instant nextRecurringDate = null;

        if (Boolean.TRUE.equals(request.isRecurring()) && request.recurringInterval() != null) {
            Instant calculated = DateUtils.nextOccurrence(request.date(), request.recurringInterval());
            nextRecurringDate = calculated.isBefore(now)
                    ? DateUtils.nextOccurrence(now, request.recurringInterval())
                    : calculated;
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(new ObjectId(userId));
        transaction.setTitle(request.title());
        transaction.setAmount(MoneyUtils.toCents(request.amount()));
        transaction.setCategory(request.category());
        transaction.setType(request.type());
        transaction.setReceiptUrl(request.receiptUrl());
        transaction.setRecurring(Boolean.TRUE.equals(request.isRecurring()));
        transaction.setRecurringInterval(request.recurringInterval());
        transaction.setNextRecurringDate(nextRecurringDate);
        transaction.setLastProcessed(null);
        transaction.setDescription(request.description());
        transaction.setDate(request.date());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    public int bulkCreate(BulkTransactionCreateRequest request, String userId) {
        Instant now = Instant.now();
        ObjectId ownerId = new ObjectId(userId);

        List<Transaction> batch = new ArrayList<>();
        for (TransactionCreateRequest tx : request.transactions()) {
            Transaction transaction = new Transaction();
            transaction.setUserId(ownerId);
            transaction.setTitle(tx.title());
            transaction.setType(tx.type());
            transaction.setAmount(MoneyUtils.toCents(tx.amount()));
            transaction.setCategory(tx.category());
            transaction.setDate(tx.date());
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setPaymentMethod(tx.paymentMethod());
            transaction.setDescription(tx.description());
            transaction.setReceiptUrl(tx.receiptUrl());
            transaction.setRecurring(false);
            transaction.setRecurringInterval(null);
            transaction.setNextRecurringDate(null);
            transaction.setLastProcessed(null);
            transaction.setCreatedAt(now);
            transaction.setUpdatedAt(now);
            batch.add(transaction);
        }

        return transactionRepository.insert(batch).size();
    }

    // ── Receipt scanning ─────────────────────────────────────────────────────

    public ScanReceiptResponse scanReceipt(MultipartFile file) {
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("Unable to read the uploaded file");
        }

        String receiptUrl = cloudinaryService.uploadFile(
                fileBytes, file.getOriginalFilename(), "receipts");

        String responseText = geminiService.generateContent(
                Prompts.RECEIPT_PROMPT, fileBytes, file.getContentType());

        String cleanText = Json.stripCodeFences(responseText);
        if (cleanText.isBlank()) {
            throw ApiException.badRequest("Could not read receipt content");
        }

        Map<String, Object> data = json.readMap(cleanText);
        Double amount = Json.dbl(data, "amount");
        if (amount == null || amount == 0d) {
            throw ApiException.badRequest("Receipt missing required information");
        }

        return new ScanReceiptResponse(
                data.containsKey("title") ? Json.str(data, "title") : "Receipt",
                amount,
                parseFlexibleDate(Json.str(data, "date")),
                Json.str(data, "description"),
                Json.str(data, "category"),
                parseEnum(Json.str(data, "paymentMethod"), PaymentMethod.class, PaymentMethod.OTHER),
                parseEnum(Json.str(data, "type"), TransactionType.class, TransactionType.EXPENSE),
                receiptUrl
        );
    }

    // ── Duplicate ────────────────────────────────────────────────────────────

    public TransactionResponse duplicate(String transactionId, String userId) {
        Transaction source = findOwned(transactionId, userId, "Transaction details not found");
        Instant now = Instant.now();

        Transaction copy = new Transaction();
        copy.setUserId(source.getUserId());
        copy.setTitle("Duplicate - " + source.getTitle());
        copy.setType(source.getType());
        copy.setAmount(source.getAmount());
        copy.setCategory(source.getCategory());
        copy.setReceiptUrl(source.getReceiptUrl());
        copy.setDescription(source.getDescription() != null
                ? source.getDescription() + " - (Duplicated)"
                : "Duplicated Transaction");
        copy.setDate(source.getDate());
        copy.setStatus(source.getStatus());
        copy.setPaymentMethod(source.getPaymentMethod());
        copy.setRecurring(false);
        copy.setRecurringInterval(null);
        copy.setNextRecurringDate(null);
        copy.setLastProcessed(source.getLastProcessed());
        copy.setCreatedAt(now);
        copy.setUpdatedAt(now);

        return TransactionResponse.from(transactionRepository.save(copy));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public void update(String transactionId, TransactionUpdateRequest request, String userId) {
        Transaction transaction = findOwned(transactionId, userId, "Transaction not found");
        Instant now = Instant.now();

        boolean recurring = request.isRecurring() != null ? request.isRecurring() : transaction.isRecurring();
        Instant date = request.date() != null ? request.date() : transaction.getDate();
        RecurringInterval interval = request.recurringInterval() != null
                ? request.recurringInterval()
                : transaction.getRecurringInterval();

        Instant nextRecurringDate = null;
        if (recurring && interval != null) {
            Instant calculated = DateUtils.nextOccurrence(now, interval);
            nextRecurringDate = calculated.isBefore(now)
                    ? DateUtils.nextOccurrence(now, interval)
                    : calculated;
        }

        transaction.setRecurring(recurring);
        transaction.setDate(date);
        transaction.setRecurringInterval(interval);
        transaction.setNextRecurringDate(nextRecurringDate);
        transaction.setUpdatedAt(now);

        if (request.title() != null) {
            transaction.setTitle(request.title());
        }
        if (request.description() != null) {
            transaction.setDescription(request.description());
        }
        if (request.type() != null) {
            transaction.setType(request.type());
        }
        if (request.category() != null) {
            transaction.setCategory(request.category());
        }
        if (request.paymentMethod() != null) {
            transaction.setPaymentMethod(request.paymentMethod());
        }
        if (request.status() != null) {
            transaction.setStatus(request.status());
        }
        if (request.amount() != null) {
            transaction.setAmount(MoneyUtils.toCents(request.amount()));
        }

        transactionRepository.save(transaction);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    public long bulkDelete(List<String> transactionIds, String userId) {
        List<ObjectId> ids = new ArrayList<>();
        for (String id : transactionIds) {
            if (ObjectId.isValid(id)) {
                ids.add(new ObjectId(id));
            }
        }

        long deleted = 0;
        if (!ids.isEmpty()) {
            Query query = new Query(Criteria.where("_id").in(ids)
                    .and("user_id").is(new ObjectId(userId)));
            deleted = mongoTemplate.remove(query, Transaction.class).getDeletedCount();
        }

        if (deleted == 0) {
            throw ApiException.notFound("No transactions found");
        }
        return deleted;
    }

    public void delete(String transactionId, String userId) {
        Transaction transaction = findOwned(transactionId, userId, "Transaction not found");
        transactionRepository.delete(transaction);
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    public TransactionListResponse getAll(String userId,
                                          String keyword,
                                          TransactionType type,
                                          RecurringStatus recurringStatus,
                                          int pageNumber,
                                          int pageSize) {

        List<Criteria> conditions = new ArrayList<>();
        conditions.add(Criteria.where("user_id").is(new ObjectId(userId)));

        if (keyword != null && !keyword.isBlank()) {
            conditions.add(new Criteria().orOperator(
                    Criteria.where("title").regex(keyword, "i"),
                    Criteria.where("category").regex(keyword, "i")
            ));
        }
        if (type != null) {
            conditions.add(Criteria.where("type").is(type.name()));
        }
        if (recurringStatus != null) {
            conditions.add(Criteria.where("is_recurring").is(recurringStatus == RecurringStatus.RECURRING));
        }

        Criteria criteria = new Criteria().andOperator(conditions.toArray(new Criteria[0]));

        long skip = (long) (pageNumber - 1) * pageSize;

        Query query = new Query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "created_at"))
                .skip(skip)
                .limit(pageSize);

        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        long totalCount = mongoTemplate.count(new Query(criteria), Transaction.class);

        return new TransactionListResponse(
                "Transactions fetched successfully",
                transactions.stream().map(TransactionResponse::from).toList(),
                PaginationDto.of(pageNumber, pageSize, totalCount)
        );
    }

    public TransactionResponse getOne(String transactionId, String userId) {
        return TransactionResponse.from(findOwned(transactionId, userId, "Transaction data not found"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Transaction findOwned(String transactionId, String userId, String notFoundMessage) {
        if (!ObjectId.isValid(transactionId)) {
            throw ApiException.notFound(notFoundMessage);
        }
        return transactionRepository
                .findByIdAndUserId(new ObjectId(transactionId), new ObjectId(userId))
                .orElseThrow(() -> ApiException.notFound(notFoundMessage));
    }

    private static Instant parseFlexibleDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(raw).atStartOfDay(DateUtils.UTC).toInstant();
        } catch (Exception ignored) {
            return Instant.now();
        }
    }

    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> enumType, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }
}
