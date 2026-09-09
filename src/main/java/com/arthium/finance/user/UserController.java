package com.arthium.finance.user;

import com.arthium.finance.report.ReportService;
import com.arthium.finance.user.dto.ReportScheduleRequest;
import com.arthium.finance.user.dto.ReportScheduleResponse;
import com.arthium.finance.user.dto.UserPrivateDto;
import com.arthium.finance.user.dto.UserProfileUpdateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Port of routes/users.py. */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final ReportService reportService;

    public UserController(UserService userService, ReportService reportService) {
        this.userService = userService;
        this.reportService = reportService;
    }

    @GetMapping("/me")
    public UserPrivateDto getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return UserPrivateDto.from(currentUser);
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileUpdateResponse updateUser(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "profile_picture", required = false) MultipartFile profilePicture) {

        User updated = userService.updateUser(currentUser.getIdAsString(), profilePicture, name);
        return new UserProfileUpdateResponse("Profile Updated Successfully", UserPrivateDto.from(updated));
    }

    @PostMapping("/schedule")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> scheduleReportJob(@AuthenticationPrincipal User currentUser,
                                                 @Valid @RequestBody ReportScheduleRequest request) {
        reportService.scheduleReportJob(currentUser.getIdAsString(), request.timezone(), request.scheduledTime());
        return Map.of("message", "Report job scheduled successfully");
    }

    @GetMapping("/schedule-time")
    public ReportScheduleResponse getScheduleTime(@AuthenticationPrincipal User currentUser) {
        return reportService.getReportSchedule(currentUser.getIdAsString());
    }
}
