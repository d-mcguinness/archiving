package com.dmc.archiving.dashboard;

import com.dmc.archiving.archive.ArchiveService;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.tenancy.service.TenancyService;
import com.dmc.archiving.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final UserService userService;
    private final TenancyService tenancyService;
    private final ArchiveService archiveService;

    public DashboardController(UserService userService,
                               TenancyService tenancyService,
                               ArchiveService archiveService) {
        this.userService = userService;
        this.tenancyService = tenancyService;
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
                stats.setTotalTenants(tenancyService.getAllTenants().size());
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

    // REST Endpoint for Dashboard Statistics
    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<?> getDashboardStatsRest() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Get counts
            stats.put("totalUsers", userService.getAllUsers().size());
            stats.put("totalTenants", tenancyService.getAllTenants().size());
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
            health.put("tenantsAvailable", tenancyService != null);
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
            stats.put("tenants", tenancyService.getAllTenants().size());
            stats.put("archives", archiveService.getAllArchives().size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity
                .status(500)
                .body(Map.of("error", "Failed to fetch quick stats: " + e.getMessage()));
        }
    }
}
