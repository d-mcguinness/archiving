package com.dmc.archiving.tenancy.input;

import com.dmc.archiving.tenancy.model.TenantStatus;
import com.dmc.archiving.tenancy.model.TenantPlan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantInput {
    private Long tenantId;
    private String name;
    private String displayName;
    private String description;
    private TenantStatus status;
    private TenantPlan plan;
}
