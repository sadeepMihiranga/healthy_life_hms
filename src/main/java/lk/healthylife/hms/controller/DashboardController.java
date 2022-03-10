package lk.healthylife.hms.controller;

import lk.healthylife.hms.dto.SummeryCountsDTO;
import lk.healthylife.hms.response.SuccessResponse;
import lk.healthylife.hms.response.SuccessResponseHandler;
import lk.healthylife.hms.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/summery")
    public ResponseEntity<SuccessResponse> getSummeryCounts(@RequestBody SummeryCountsDTO summeryCountsDTO) {
        return SuccessResponseHandler.generateResponse(dashboardService.getSummeryCounts(summeryCountsDTO));
    }
}
