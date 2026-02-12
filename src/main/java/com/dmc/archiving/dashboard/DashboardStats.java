package com.dmc.archiving.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private int totalUsers;
    private int totalTenants;
    private int totalArchives;
    private int activeArchives;
    private int draftArchives;
    private int archivedArchives;
}
