package com.arthium.finance.transaction;

import com.arthium.finance.common.ApiException;
import com.arthium.finance.transaction.dto.BulkTransactionCreateRequest;
import com.arthium.finance.transaction.dto.BulkTransactionCreateResponse;
import com.arthium.finance.transaction.dto.BulkTransactionDeleteResponse;
import com.arthium.finance.transaction.dto.ScanReceiptResponse;
import com.arthium.finance.transaction.dto.TransactionCreateRequest;
import com.arthium.finance.transaction.dto.TransactionListResponse;
import com.arthium.finance.transaction.dto.TransactionResponse;
import com.arthium.finance.transaction.dto.TransactionUpdateRequest;
import com.arthium.finance.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Port of routes/transactions.py. */
@RestController
@RequestMapping("/api/transaction")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionCreateRequest request,
                                      @AuthenticationPrincipal User currentUser) {
        return transactionService.create(request, currentUser.getIdAsString());
    }

    @PostMapping("/bulk-transaction")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkTransactionCreateResponse bulkCreate(@Valid @RequestBody BulkTransactionCreateRequest request,
                                                    @AuthenticationPrincipal User currentUser) {
        int insertedCount = transactionService.bulkCreate(request, currentUser.getIdAsString());
        return new BulkTransactionCreateResponse(insertedCount, true);
    }

    @PostMapping(value = "/scan-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScanReceiptResponse scanReceipt(@AuthenticationPrincipal User currentUser,
                                           @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file uploaded");
        }
        return transactionService.scanReceipt(file);
    }

    @PutMapping("/duplicate/{transactionId}")
    public TransactionResponse duplicate(@PathVariable String transactionId,
                                         @AuthenticationPrincipal User currentUser) {
        return transactionService.duplicate(transactionId, currentUser.getIdAsString());
    }

    @PutMapping("/{transactionId}")
    public Map<String, String> update(@PathVariable String transactionId,
                                      @RequestBody TransactionUpdateRequest request,
                                      @AuthenticationPrincipal User currentUser) {
        transactionService.update(transactionId, request, currentUser.getIdAsString());
        return Map.of("message", "Transaction updated successfully");
    }

    @DeleteMapping("/bulk-delete")
    public BulkTransactionDeleteResponse bulkDelete(@RequestBody List<String> transactionIds,
                                                    @AuthenticationPrincipal User currentUser) {
        long deletedCount = transactionService.bulkDelete(transactionIds, currentUser.getIdAsString());
        return new BulkTransactionDeleteResponse(true, deletedCount);
    }

    @DeleteMapping("/{transactionId}")
    public Map<String, Object> delete(@PathVariable String transactionId,
                                      @AuthenticationPrincipal User currentUser) {
        transactionService.delete(transactionId, currentUser.getIdAsString());
        return Map.of("success", true, "detail", "Transaction deleted successfully");
    }

    @GetMapping("/all")
    public TransactionListResponse getAll(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(name = "recurring_status", required = false) RecurringStatus recurringStatus,
            @RequestParam(name = "page_size", defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(name = "page_number", defaultValue = "1") @Min(1) int pageNumber) {

        return transactionService.getAll(
                currentUser.getIdAsString(), keyword, type, recurringStatus, pageNumber, pageSize);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getOne(@PathVariable String transactionId,
                                      @AuthenticationPrincipal User currentUser) {
        return transactionService.getOne(transactionId, currentUser.getIdAsString());
    }
}
