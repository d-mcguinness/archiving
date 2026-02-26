package com.dmc.archiving.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantDashboardStats {
    private Long tenantId;
    private String tenantName;
    private String tenantStatus;
    private String tenantPlan;
    private int totalUsers;
    private int totalArchives;
    private int activeArchives;
    private int draftArchives;
    private int archivedArchives;
}

