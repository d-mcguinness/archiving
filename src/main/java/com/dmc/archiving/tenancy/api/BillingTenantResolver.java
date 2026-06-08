package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.tenancy.repository.TenantMembershipRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The single authoritative rule for which tenant a billable resource belongs to.
 *
 * <p>Resolution is driven by the authenticated caller's membership, never by a
 * raw client-supplied tenantId. This is what makes per-tenant billing
 * trustworthy: a caller cannot attribute a document/archive/package to a tenant
 * they are not a member of.
 *
 * <ul>
 *   <li>ADMIN — platform operator; must explicitly name the tenant a resource
 *       belongs to (no implicit tenant).</li>
 *   <li>TENANT / USER — the claimed tenant (if any) must be one of the caller's
 *       memberships; if none is claimed and the caller belongs to exactly one
 *       tenant, that tenant is used; multiple memberships with no claim is
 *       ambiguous and rejected.</li>
 * </ul>
 */
@Component
public class BillingTenantResolver {

    private final TenantMembershipRepository memberships;

    public BillingTenantResolver(TenantMembershipRepository memberships) {
        this.memberships = memberships;
    }

    /**
     * @param ctx              the authenticated caller
     * @param claimedTenantId  the tenantId the request asked for (may be null)
     * @return the authoritative tenant id the resource must be attributed to
     * @throws AccessDeniedException if unauthenticated, the claim is not a
     *         membership, the caller has no tenant, or the tenant is ambiguous
     */
    public Long resolve(AuthContext ctx, Long claimedTenantId) {
        if (ctx == null || !ctx.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        if (ctx.isAdmin()) {
            if (claimedTenantId == null) {
                throw new AccessDeniedException(
                        "ADMIN must specify a tenantId for a tenant-scoped resource");
            }
            return claimedTenantId;
        }

        // TENANT / USER: the resource must land in a tenant the caller belongs to.
        if (claimedTenantId != null) {
            if (!memberships.existsByTenantIdAndUserId(claimedTenantId, ctx.userId())) {
                throw new AccessDeniedException(
                        "Access denied: user " + ctx.userId()
                                + " is not a member of tenant " + claimedTenantId);
            }
            return claimedTenantId;
        }

        List<Long> tenantIds = memberships.findByUserId(ctx.userId()).stream()
                .map(m -> m.getTenantId())
                .distinct()
                .toList();

        if (tenantIds.isEmpty()) {
            throw new AccessDeniedException(
                    "Access denied: user " + ctx.userId() + " belongs to no tenant");
        }
        if (tenantIds.size() > 1) {
            throw new AccessDeniedException(
                    "Ambiguous tenant: caller belongs to multiple tenants; specify tenantId");
        }
        return tenantIds.get(0);
    }
}
