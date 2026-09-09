package com.arthium.finance.budget;

import com.arthium.finance.budget.dto.BudgetCreateRequest;
import com.arthium.finance.budget.dto.BudgetDeleteResponse;
import com.arthium.finance.budget.dto.BudgetItem;
import com.arthium.finance.budget.dto.BudgetListResponse;
import com.arthium.finance.budget.dto.BudgetResponse;
import com.arthium.finance.budget.dto.BudgetUpdateRequest;
import com.arthium.finance.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetCreateRequest request,
                                 @AuthenticationPrincipal User currentUser) {
        BudgetItem budget = budgetService.create(request, currentUser.getIdAsString());
        return new BudgetResponse("Budget created successfully", budget);
    }

    @GetMapping("/all")
    public BudgetListResponse getAll(@AuthenticationPrincipal User currentUser) {
        return budgetService.getAll(currentUser.getIdAsString());
    }

    @PutMapping("/{budgetId}")
    public BudgetResponse update(@PathVariable String budgetId,
                                 @Valid @RequestBody BudgetUpdateRequest request,
                                 @AuthenticationPrincipal User currentUser) {
        BudgetItem budget = budgetService.update(budgetId, request, currentUser.getIdAsString());
        return new BudgetResponse("Budget updated successfully", budget);
    }

    @DeleteMapping("/{budgetId}")
    public BudgetDeleteResponse delete(@PathVariable String budgetId,
                                       @AuthenticationPrincipal User currentUser) {
        String deletedId = budgetService.delete(budgetId, currentUser.getIdAsString());
        return new BudgetDeleteResponse("Budget deleted successfully", deletedId);
    }
}
