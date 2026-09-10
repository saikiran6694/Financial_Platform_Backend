package com.arthium.finance.transaction;

import com.arthium.finance.ai.GeminiService;
import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.Json;
import com.arthium.finance.storage.CloudinaryService;
import com.arthium.finance.transaction.dto.BulkTransactionCreateRequest;
import com.arthium.finance.transaction.dto.ScanReceiptResponse;
import com.arthium.finance.transaction.dto.TransactionCreateRequest;
import com.arthium.finance.transaction.dto.TransactionListResponse;
import com.arthium.finance.transaction.dto.TransactionResponse;
import com.arthium.finance.transaction.dto.TransactionUpdateRequest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private GeminiService geminiService;
    @Mock
    private Json json;

    private TransactionService transactionService;
    private final String userId = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, mongoTemplate, cloudinaryService, geminiService, json);
    }

    private static TransactionCreateRequest createRequest(Instant date, Boolean isRecurring, RecurringInterval interval) {
        return new TransactionCreateRequest(
                "Groceries", TransactionType.EXPENSE, 42.5, "Food",
                date, isRecurring, interval, "desc", null, PaymentMethod.CASH);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_nonRecurring_convertsAmountToCentsAndLeavesNextRecurringDateNull() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService.create(
                createRequest(Instant.now(), false, null), userId);

        assertThat(response.amount()).isEqualTo(42.5);
        assertThat(response.nextRecurringDate()).isNull();
        assertThat(response.isRecurring()).isFalse();
    }

    @Test
    void create_recurringWithFutureOccurrence_usesCalculatedNextRecurringDate() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Instant futureDate = Instant.now().plus(1, ChronoUnit.DAYS);
        transactionService.create(createRequest(futureDate, true, RecurringInterval.WEEKLY), userId);

        Transaction saved = captor.getValue();
        assertThat(saved.isRecurring()).isTrue();
        assertThat(saved.getNextRecurringDate()).isAfter(Instant.now());
    }

    @Test
    void create_recurringWithPastOccurrence_recomputesNextRecurringDateFromNow() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Instant pastDate = Instant.now().minus(60, ChronoUnit.DAYS);
        transactionService.create(createRequest(pastDate, true, RecurringInterval.MONTHLY), userId);

        Transaction saved = captor.getValue();
        // A monthly occurrence from 60 days ago has already elapsed, so the service
        // must recompute from "now" rather than leaving a next-occurrence in the past.
        assertThat(saved.getNextRecurringDate()).isAfter(Instant.now());
    }

    @Test
    void create_recurringWithoutInterval_leavesNextRecurringDateNull() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService.create(
                createRequest(Instant.now(), true, null), userId);

        assertThat(response.nextRecurringDate()).isNull();
    }

    // ── bulkCreate ───────────────────────────────────────────────────────────

    @Test
    void bulkCreate_forcesEveryTransactionToBeNonRecurring() {
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        when(transactionRepository.insert(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<TransactionCreateRequest> requests = List.of(
                createRequest(Instant.now(), true, RecurringInterval.DAILY),
                createRequest(Instant.now(), true, RecurringInterval.YEARLY));
        transactionService.bulkCreate(new BulkTransactionCreateRequest(requests), userId);

        verify(transactionRepository).insert(captor.capture());
        assertThat(captor.getValue()).allSatisfy(tx -> {
            assertThat(tx.isRecurring()).isFalse();
            assertThat(tx.getRecurringInterval()).isNull();
            assertThat(tx.getNextRecurringDate()).isNull();
        });
    }

    @Test
    void bulkCreate_returnsCountOfInsertedTransactions() {
        List<TransactionCreateRequest> requests = List.of(
                createRequest(Instant.now(), false, null),
                createRequest(Instant.now(), false, null),
                createRequest(Instant.now(), false, null));
        when(transactionRepository.insert(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int inserted = transactionService.bulkCreate(new BulkTransactionCreateRequest(requests), userId);

        assertThat(inserted).isEqualTo(3);
    }

    // ── scanReceipt ──────────────────────────────────────────────────────────

    @Test
    void scanReceipt_unreadableFile_throwsBadRequest() throws IOException {
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> transactionService.scanReceipt(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to read the uploaded file");
    }

    @Test
    void scanReceipt_blankAiResponse_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "bytes".getBytes());
        when(cloudinaryService.uploadFile(any(), any(), eq("receipts"))).thenReturn("https://cdn/receipt.jpg");
        when(geminiService.generateContent(any(), any(), any())).thenReturn("   ");

        assertThatThrownBy(() -> transactionService.scanReceipt(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Could not read receipt content");
    }

    @Test
    void scanReceipt_missingAmount_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "bytes".getBytes());
        when(cloudinaryService.uploadFile(any(), any(), eq("receipts"))).thenReturn("https://cdn/receipt.jpg");
        when(geminiService.generateContent(any(), any(), any())).thenReturn("{\"title\":\"Coffee\"}");
        when(json.readMap("{\"title\":\"Coffee\"}")).thenReturn(Map.of("title", "Coffee"));

        assertThatThrownBy(() -> transactionService.scanReceipt(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Receipt missing required information");
    }

    @Test
    void scanReceipt_zeroAmount_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "bytes".getBytes());
        when(cloudinaryService.uploadFile(any(), any(), eq("receipts"))).thenReturn("https://cdn/receipt.jpg");
        when(geminiService.generateContent(any(), any(), any())).thenReturn("{\"amount\":0}");
        when(json.readMap("{\"amount\":0}")).thenReturn(Map.of("amount", 0));

        assertThatThrownBy(() -> transactionService.scanReceipt(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Receipt missing required information");
    }

    @Test
    void scanReceipt_missingTitle_defaultsToReceipt() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "bytes".getBytes());
        when(cloudinaryService.uploadFile(any(), any(), eq("receipts"))).thenReturn("https://cdn/receipt.jpg");
        String raw = "{\"amount\":19.99}";
        when(geminiService.generateContent(any(), any(), any())).thenReturn(raw);
        when(json.readMap(raw)).thenReturn(Map.of("amount", 19.99));

        ScanReceiptResponse response = transactionService.scanReceipt(file);

        assertThat(response.title()).isEqualTo("Receipt");
        assertThat(response.amount()).isEqualTo(19.99);
        assertThat(response.receiptUrl()).isEqualTo("https://cdn/receipt.jpg");
    }

    @Test
    void scanReceipt_invalidPaymentMethodAndType_fallBackToDefaults() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "bytes".getBytes());
        when(cloudinaryService.uploadFile(any(), any(), eq("receipts"))).thenReturn("https://cdn/receipt.jpg");
        String raw = "{\"amount\":10,\"paymentMethod\":\"bogus\",\"type\":\"bogus\"}";
        when(geminiService.generateContent(any(), any(), any())).thenReturn(raw);
        when(json.readMap(raw)).thenReturn(Map.of("amount", 10, "paymentMethod", "bogus", "type", "bogus"));

        ScanReceiptResponse response = transactionService.scanReceipt(file);

        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.OTHER);
        assertThat(response.type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void scanReceipt_validPaymentMethodAndType_areParsedCaseInsensitively() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "bytes".getBytes());
        when(cloudinaryService.uploadFile(any(), any(), eq("receipts"))).thenReturn("https://cdn/receipt.jpg");
        String raw = "{\"amount\":10,\"paymentMethod\":\"card\",\"type\":\"income\"}";
        when(geminiService.generateContent(any(), any(), any())).thenReturn(raw);
        when(json.readMap(raw)).thenReturn(Map.of("amount", 10, "paymentMethod", "card", "type", "income"));

        ScanReceiptResponse response = transactionService.scanReceipt(file);

        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.type()).isEqualTo(TransactionType.INCOME);
    }

    // ── duplicate ────────────────────────────────────────────────────────────

    @Test
    void duplicate_invalidTransactionId_throwsNotFound() {
        assertThatThrownBy(() -> transactionService.duplicate("not-an-id", userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Transaction details not found");
    }

    @Test
    void duplicate_transactionNotOwnedOrMissing_throwsNotFound() {
        String txId = new ObjectId().toHexString();
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> transactionService.duplicate(txId, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Transaction details not found");
    }

    @Test
    void duplicate_success_prefixesTitleAndAppendsToDescription() {
        String txId = new ObjectId().toHexString();
        Transaction source = new Transaction();
        source.setUserId(new ObjectId(userId));
        source.setTitle("Groceries");
        source.setDescription("Weekly shop");
        source.setRecurring(true);
        source.setRecurringInterval(RecurringInterval.WEEKLY);
        source.setNextRecurringDate(Instant.now());
        source.setLastProcessed(Instant.now().minus(1, ChronoUnit.DAYS));
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.of(source));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService.duplicate(txId, userId);

        assertThat(response.title()).isEqualTo("Duplicate - Groceries");
        assertThat(response.description()).isEqualTo("Weekly shop - (Duplicated)");
        assertThat(response.isRecurring()).isFalse();
        assertThat(response.recurringInterval()).isNull();
        assertThat(response.nextRecurringDate()).isNull();
        assertThat(response.lastProcessed()).isEqualTo(source.getLastProcessed());
    }

    @Test
    void duplicate_missingDescription_defaultsToDuplicatedTransaction() {
        String txId = new ObjectId().toHexString();
        Transaction source = new Transaction();
        source.setUserId(new ObjectId(userId));
        source.setTitle("Groceries");
        source.setDescription(null);
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.of(source));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService.duplicate(txId, userId);

        assertThat(response.description()).isEqualTo("Duplicated Transaction");
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_transactionNotFound_throwsNotFound() {
        String txId = new ObjectId().toHexString();
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.empty());

        TransactionUpdateRequest request = new TransactionUpdateRequest(
                "New title", null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> transactionService.update(txId, request, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void update_onlyOverwritesFieldsPresentInRequest() {
        String txId = new ObjectId().toHexString();
        Transaction existing = new Transaction();
        existing.setUserId(new ObjectId(userId));
        existing.setTitle("Old title");
        existing.setCategory("Old category");
        existing.setAmount(1000L);
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.of(existing));

        TransactionUpdateRequest request = new TransactionUpdateRequest(
                "New title", null, null, null, null, null, null, null, null, null, null);
        transactionService.update(txId, request, userId);

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getCategory()).isEqualTo("Old category");
        assertThat(existing.getAmount()).isEqualTo(1000L);
        verify(transactionRepository).save(existing);
    }

    @Test
    void update_settingRecurringWithFutureInterval_computesNextRecurringDate() {
        String txId = new ObjectId().toHexString();
        Transaction existing = new Transaction();
        existing.setUserId(new ObjectId(userId));
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.of(existing));

        TransactionUpdateRequest request = new TransactionUpdateRequest(
                null, null, null, null, null, true, RecurringInterval.MONTHLY, null, null, null, null);
        transactionService.update(txId, request, userId);

        assertThat(existing.isRecurring()).isTrue();
        assertThat(existing.getNextRecurringDate()).isAfter(Instant.now());
    }

    @Test
    void update_turningOffRecurring_clearsNextRecurringDate() {
        String txId = new ObjectId().toHexString();
        Transaction existing = new Transaction();
        existing.setUserId(new ObjectId(userId));
        existing.setRecurring(true);
        existing.setRecurringInterval(RecurringInterval.WEEKLY);
        existing.setNextRecurringDate(Instant.now().plus(1, ChronoUnit.DAYS));
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.of(existing));

        TransactionUpdateRequest request = new TransactionUpdateRequest(
                null, null, null, null, null, false, null, null, null, null, null);
        transactionService.update(txId, request, userId);

        assertThat(existing.isRecurring()).isFalse();
        assertThat(existing.getNextRecurringDate()).isNull();
    }

    // ── bulkDelete ───────────────────────────────────────────────────────────

    @Test
    void bulkDelete_filtersOutInvalidIdsSilently() {
        String validId = new ObjectId().toHexString();
        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(1L);
        when(mongoTemplate.remove(any(Query.class), eq(Transaction.class))).thenReturn(deleteResult);

        long deleted = transactionService.bulkDelete(List.of(validId, "not-a-valid-id"), userId);

        assertThat(deleted).isEqualTo(1L);
    }

    @Test
    void bulkDelete_allIdsInvalid_throwsNotFound() {
        assertThatThrownBy(() -> transactionService.bulkDelete(List.of("bad-1", "bad-2"), userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No transactions found");

        verify(mongoTemplate, never()).remove(any(Query.class), eq(Transaction.class));
    }

    @Test
    void bulkDelete_noneMatched_throwsNotFound() {
        String validId = new ObjectId().toHexString();
        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(0L);
        when(mongoTemplate.remove(any(Query.class), eq(Transaction.class))).thenReturn(deleteResult);

        assertThatThrownBy(() -> transactionService.bulkDelete(List.of(validId), userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No transactions found");
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_transactionNotFound_throwsNotFound() {
        String txId = new ObjectId().toHexString();
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(txId, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void delete_success_removesTransaction() {
        String txId = new ObjectId().toHexString();
        Transaction existing = new Transaction();
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.of(existing));

        transactionService.delete(txId, userId);

        verify(transactionRepository).delete(existing);
    }

    // ── getAll ───────────────────────────────────────────────────────────────

    @Test
    void getAll_computesPaginationFromTotalCount() {
        when(mongoTemplate.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(Transaction.class))).thenReturn(45L);

        TransactionListResponse response = transactionService.getAll(
                userId, null, null, null, 2, 20);

        assertThat(response.pagination().totalPages()).isEqualTo(3);
        assertThat(response.pagination().skip()).isEqualTo(20);
    }

    @Test
    void getAll_mapsTransactionsToResponses() {
        Transaction tx = new Transaction();
        tx.setUserId(new ObjectId(userId));
        tx.setTitle("Groceries");
        when(mongoTemplate.find(any(Query.class), eq(Transaction.class))).thenReturn(List.of(tx));
        when(mongoTemplate.count(any(Query.class), eq(Transaction.class))).thenReturn(1L);

        TransactionListResponse response = transactionService.getAll(
                userId, "groceries", TransactionType.EXPENSE, RecurringStatus.NON_RECURRING, 1, 20);

        assertThat(response.transactions()).hasSize(1);
        assertThat(response.transactions().get(0).title()).isEqualTo("Groceries");
    }

    // ── getOne ───────────────────────────────────────────────────────────────

    @Test
    void getOne_transactionNotFound_throwsNotFoundWithSpecificMessage() {
        String txId = new ObjectId().toHexString();
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> transactionService.getOne(txId, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Transaction data not found");
    }

    @Test
    void getOne_success_returnsMappedTransaction() {
        String txId = new ObjectId().toHexString();
        Transaction tx = new Transaction();
        tx.setTitle("Rent");
        when(transactionRepository.findByIdAndUserId(new ObjectId(txId), new ObjectId(userId)))
                .thenReturn(java.util.Optional.of(tx));

        TransactionResponse response = transactionService.getOne(txId, userId);

        assertThat(response.title()).isEqualTo("Rent");
    }
}
