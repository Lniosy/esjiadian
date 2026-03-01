package com.lniosy.usedappliance.controller;

import com.lniosy.usedappliance.common.ApiResponse;
import com.lniosy.usedappliance.dto.admin.ReviewDecisionRequest;
import com.lniosy.usedappliance.dto.admin.ChatReportDecisionRequest;
import com.lniosy.usedappliance.dto.dispute.DisputeResolveRequest;
import com.lniosy.usedappliance.service.*;
import com.lniosy.usedappliance.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final ProductService productService;
    private final UserService userService;
    private final DisputeService disputeService;
    private final MonitorService monitorService;
    private final MonitorAlertEventService monitorAlertEventService;
    private final EvaluationService evaluationService;
    private final SmsMockService smsMockService;
    private final ChatReportAdminService chatReportAdminService;
    private final OperationLogService operationLogService;

    public AdminController(AdminService adminService, ProductService productService,
                           UserService userService, DisputeService disputeService,
                           MonitorService monitorService, MonitorAlertEventService monitorAlertEventService,
                           EvaluationService evaluationService, SmsMockService smsMockService,
                           ChatReportAdminService chatReportAdminService,
                           OperationLogService operationLogService) {
        this.adminService = adminService;
        this.productService = productService;
        this.userService = userService;
        this.disputeService = disputeService;
        this.monitorService = monitorService;
        this.monitorAlertEventService = monitorAlertEventService;
        this.evaluationService = evaluationService;
        this.smsMockService = smsMockService;
        this.chatReportAdminService = chatReportAdminService;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/users")
    public ApiResponse<?> users(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Boolean enabled,
                                @RequestParam(required = false) String role) {
        return ApiResponse.ok(adminService.users(keyword, enabled, role));
    }

    @PostMapping("/users/{userId}/enable")
    public ApiResponse<Void> enableUser(@PathVariable Long userId) {
        adminService.setUserEnabled(userId, true);
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "ENABLE_USER", "USER", userId, "启用用户");
        return ApiResponse.ok("用户已启用", null);
    }

    @PostMapping("/users/{userId}/disable")
    public ApiResponse<Void> disableUser(@PathVariable Long userId) {
        adminService.setUserEnabled(userId, false);
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "DISABLE_USER", "USER", userId, "禁用用户");
        return ApiResponse.ok("用户已禁用", null);
    }

    @GetMapping("/orders")
    public ApiResponse<?> orders() {
        return ApiResponse.ok(adminService.orders());
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders() {
        String csv = adminService.exportOrdersCsv();
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping("/products/reviews/{productId}/approve")
    public ApiResponse<Void> approveProduct(@PathVariable Long productId) {
        productService.adminApprove(productId);
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "APPROVE_PRODUCT", "PRODUCT", productId, "商品审核通过");
        return ApiResponse.ok("审核通过", null);
    }

    @GetMapping("/products/reviews")
    public ApiResponse<?> reviewList(@RequestParam(defaultValue = "PENDING_REVIEW") String status,
                                     @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(productService.adminReviewList(status, keyword));
    }

    @PostMapping("/products/reviews/{productId}/reject")
    public ApiResponse<Void> rejectProduct(@PathVariable Long productId, @RequestBody(required = false) ReviewDecisionRequest req) {
        String reason = req == null ? "不符合规范" : req.reason();
        productService.adminReject(productId, reason);
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "REJECT_PRODUCT", "PRODUCT", productId, reason);
        return ApiResponse.ok("审核驳回", null);
    }

    @PostMapping("/verification/{userId}/approve")
    public ApiResponse<Void> approveRealName(@PathVariable Long userId) {
        userService.approveRealName(userId);
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "APPROVE_REALNAME", "USER", userId, "实名审核通过");
        return ApiResponse.ok("实名审核通过", null);
    }

    @PostMapping("/verification/{userId}/reject")
    public ApiResponse<Void> rejectRealName(@PathVariable Long userId) {
        userService.rejectRealName(userId);
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "REJECT_REALNAME", "USER", userId, "实名审核驳回");
        return ApiResponse.ok("实名审核驳回", null);
    }

    @GetMapping("/stats/overview")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(adminService.stats());
    }

    @PostMapping("/stats/recompute-seller-scores")
    public ApiResponse<?> recomputeSellerScores() {
        int count = evaluationService.recomputeAllSellerScores();
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "RECOMPUTE_SELLER_SCORE", "SYSTEM", "-", "重算卖家数=" + count);
        return ApiResponse.ok(Map.of("recomputed", count));
    }

    @GetMapping("/stats/trends")
    public ApiResponse<Map<String, Object>> trends(@RequestParam(defaultValue = "7") Integer days) {
        return ApiResponse.ok(adminService.trend(days));
    }

    @GetMapping("/stats/trends/export")
    public ResponseEntity<byte[]> exportTrends(@RequestParam(defaultValue = "7") Integer days) {
        String csv = adminService.exportTrendCsv(days);
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stats-trends.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/monitor")
    public ApiResponse<Map<String, Object>> monitor() {
        return ApiResponse.ok(monitorService.snapshot());
    }

    @GetMapping("/monitor/alerts")
    public ApiResponse<?> monitorAlerts(@RequestParam(defaultValue = "OPEN") String status) {
        return ApiResponse.ok(monitorAlertEventService.list(status));
    }

    @GetMapping("/monitor/mysql/slow-queries")
    public ApiResponse<?> monitorMysqlSlowQueries(@RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.ok(monitorService.mysqlSlowQueryDetails(limit));
    }

    @PostMapping("/monitor/alerts/{id}/ack")
    public ApiResponse<Void> ackMonitorAlert(@PathVariable Long id) {
        monitorAlertEventService.ack(id, SecurityUtils.currentUserId());
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "ACK_ALERT", "MONITOR_ALERT", id, "确认告警");
        return ApiResponse.ok("告警已确认", null);
    }

    @GetMapping("/notifications/sms-logs")
    public ApiResponse<?> smsLogs(@RequestParam(required = false) String type,
                                  @RequestParam(defaultValue = "100") Integer limit) {
        return ApiResponse.ok(smsMockService.listAll(type, limit));
    }

    @GetMapping("/operation-logs")
    public ApiResponse<?> operationLogs(@RequestParam(defaultValue = "100") Integer limit) {
        return ApiResponse.ok(operationLogService.latest(limit));
    }

    @GetMapping("/chat-reports")
    public ApiResponse<?> chatReports(@RequestParam(defaultValue = "PENDING") String status,
                                      @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(chatReportAdminService.list(status, keyword));
    }

    @PostMapping("/chat-reports/{id}/resolve")
    public ApiResponse<Void> resolveChatReport(@PathVariable Long id,
                                               @RequestBody ChatReportDecisionRequest req) {
        chatReportAdminService.resolve(id, SecurityUtils.currentUserId(), req.status(), req.note());
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "RESOLVE_CHAT_REPORT", "CHAT_REPORT", id,
                "status=" + req.status() + ",note=" + (req.note() == null ? "" : req.note()));
        return ApiResponse.ok("举报处理完成", null);
    }

    @GetMapping("/disputes")
    public ApiResponse<?> disputes() {
        return ApiResponse.ok(disputeService.allDisputes());
    }

    @PostMapping("/disputes/{id}/resolve")
    public ApiResponse<?> resolveDispute(@PathVariable Long id, @RequestBody DisputeResolveRequest req) {
        ApiResponse<?> response = ApiResponse.ok(disputeService.resolve(id, req));
        operationLogService.log(SecurityUtils.currentUserId(), "ADMIN", "RESOLVE_DISPUTE", "DISPUTE", id,
                "status=" + req.status() + ",note=" + (req.resultNote() == null ? "" : req.resultNote()));
        return response;
    }
}
