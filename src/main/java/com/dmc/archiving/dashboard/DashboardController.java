package com.dmc.archiving.dashboard;

import com.dmc.archiving.archive.ArchiveService;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.user.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController extends BaseGraphQlController {

    private final UserService userService;
    private final ArchiveService archiveService;

    public DashboardController(UserService userService,
                               TenancyApi tenancyApi,
                               ArchiveService archiveService) {
        super(tenancyApi);
        this.userService = userService;
        this.archiveService = archiveService;
    }

    // GraphQL Query for Dashboard Statistics
    @QueryMapping
    public DashboardStats getDashboardStats() {
        try {
            log.info("Fetching dashboard stats via GraphQL");

            DashboardStats stats = new DashboardStats();

            // Get counts with error handling
            try {
                stats.setTotalUsers(userService.getAllUsers().size());
            } catch (Exception e) {
                log.error("Error fetching users count: {}", e.getMessage());
                stats.setTotalUsers(0);
            }

            try {
                stats.setTotalTenants(tenancyApi.getAllTenants().size());
            } catch (Exception e) {
                log.error("Error fetching tenants count: {}", e.getMessage());
                stats.setTotalTenants(0);
            }

            try {
                stats.setTotalArchives(archiveService.getAllArchives().size());

                // Get archive breakdown by status - use service methods
                stats.setActiveArchives(archiveService.countByStatus(ArchiveStatus.PUBLISHED));
                stats.setDraftArchives(archiveService.countByStatus(ArchiveStatus.DRAFT));
                stats.setArchivedArchives(archiveService.countByStatus(ArchiveStatus.ARCHIVED));
            } catch (Exception e) {
                log.error("Error fetching archives: {}", e.getMessage());
                stats.setTotalArchives(0);
                stats.setActiveArchives(0);
                stats.setDraftArchives(0);
                stats.setArchivedArchives(0);
            }

            log.info("Dashboard stats fetched successfully: users={}, tenants={}, archives={}",
                stats.getTotalUsers(), stats.getTotalTenants(), stats.getTotalArchives());

            return stats;
        } catch (Exception e) {
            log.error("Error in getDashboardStats: {}", e.getMessage(), e);
            // Return empty stats instead of throwing
            return new DashboardStats();
        }
    }

    // GraphQL Query for Tenant-Specific Dashboard Statistics
    @QueryMapping
    public TenantDashboardStats getTenantDashboardStats(@Argument Long tenantId) {
        try {
            log.info("Fetching tenant dashboard stats for tenant: {}", tenantId);

            TenantDashboardStats stats = new TenantDashboardStats();
            Tenant tenant = null;

            // Get tenant info
            try {
                tenant = tenancyApi.getTenantById(tenantId);
                if (tenant != null) {
                    stats.setTenantId(tenantId);
                    stats.setTenantName(tenant.getDisplayName() != null ? tenant.getDisplayName() : tenant.getName());
                    stats.setTenantStatus(tenant.getStatus().toString());
                    stats.setTenantPlan(tenant.getPlan().toString());
                } else {
                    log.warn("Tenant not found: {}", tenantId);
                    return stats;
                }
            } catch (Exception e) {
                log.error("Error fetching tenant info: {}", e.getMessage());
                return stats;
            }

            // Get users count for this tenant
            try {
                stats.setTotalUsers((int) tenancyApi.countUsersInTenant(tenantId));
            } catch (Exception e) {
                log.error("Error fetching tenant users count: {}", e.getMessage());
                stats.setTotalUsers(0);
            }

            // Get archives for this tenant (by ownerId)
            try {
                List<Archive> tenantArchives = archiveService.getArchivesByOwner(tenantId);
                stats.setTotalArchives(tenantArchives.size());

                // Count by status
                stats.setActiveArchives((int) tenantArchives.stream()
                    .filter(a -> a.getStatus() == ArchiveStatus.PUBLISHED)
                    .count());
                stats.setDraftArchives((int) tenantArchives.stream()
                    .filter(a -> a.getStatus() == ArchiveStatus.DRAFT)
                    .count());
                stats.setArchivedArchives((int) tenantArchives.stream()
                    .filter(a -> a.getStatus() == ArchiveStatus.ARCHIVED)
                    .count());
            } catch (Exception e) {
                log.error("Error fetching tenant archives: {}", e.getMessage());
                stats.setTotalArchives(0);
                stats.setActiveArchives(0);
                stats.setDraftArchives(0);
                stats.setArchivedArchives(0);
            }

            log.info("Tenant dashboard stats fetched successfully for tenant {}: users={}, archives={}",
                tenantId, stats.getTotalUsers(), stats.getTotalArchives());

            return stats;
        } catch (Exception e) {
            log.error("Error in getTenantDashboardStats: {}", e.getMessage(), e);
            return new TenantDashboardStats();
        }
    }

    // REST Endpoint for Dashboard Statistics
    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<?> getDashboardStatsRest() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Get counts
            stats.put("totalUsers", userService.getAllUsers().size());
            stats.put("totalTenants", tenancyApi.getAllTenants().size());
            stats.put("totalArchives", archiveService.getAllArchives().size());

            // Get archive breakdown by status - use service methods
            stats.put("activeArchives", archiveService.countByStatus(ArchiveStatus.PUBLISHED));
            stats.put("draftArchives", archiveService.countByStatus(ArchiveStatus.DRAFT));
            stats.put("archivedArchives", archiveService.countByStatus(ArchiveStatus.ARCHIVED));

            // Archive breakdown by standard - use service method
            stats.put("standardBreakdown", archiveService.getArchiveCountByStandard());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity
                .status(500)
                .body(Map.of("error", "Failed to fetch dashboard stats: " + e.getMessage()));
        }
    }

    // REST Endpoint for Quick Stats (lightweight)
    @GetMapping("/api/dashboard/health")
    @ResponseBody
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        try {
            health.put("usersAvailable", userService != null);
            health.put("tenantsAvailable", tenancyApi != null);
            health.put("archivesAvailable", archiveService != null);
        } catch (Exception e) {
            health.put("error", e.getMessage());
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/api/dashboard/quick-stats")
    @ResponseBody
    public ResponseEntity<?> getQuickStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("users", userService.getAllUsers().size());
            stats.put("tenants", tenancyApi.getAllTenants().size());
            stats.put("archives", archiveService.getAllArchives().size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity
                .status(500)
                .body(Map.of("error", "Failed to fetch quick stats: " + e.getMessage()));
        }
    }
}
