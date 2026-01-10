package com.dmc.archiving.tenancy.input;

import com.dmc.archiving.tenancy.model.TenantPlan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantInput {
    private String name;
    private String domain;
    private String displayName;
    private String description;
    private String ownerId;
    private TenantPlan plan;
}
