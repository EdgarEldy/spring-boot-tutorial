package com.edgareldy.springboottutorial.dto.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;

/**
 * Request body for {@code PATCH /api/v1/roles/{roleId}/permissions}. Carries
 * the IDs of the permissions to add to the role and the IDs of the ones to
 * remove from it. Both sets are optional, but at least one must be non-empty
 * and a permission ID cannot appear in both.
 * <p>
 * Created edgar.muhamyangabo on 11/12/26
 * Author : edgar.muhamyangabo
 * Date : 11/12/26
 * Project : spring-boot-tutorial
 */
public record UpdateRolePermissionsRequest(

        Set<@NotNull(message = "Permission ID to add must not be null") Long> add,

        Set<@NotNull(message = "Permission ID to remove must not be null") Long> remove
) {

    public UpdateRolePermissionsRequest {
        add = add == null ? Set.of() : add;
        remove = remove == null ? Set.of() : remove;
    }

    @JsonIgnore
    @AssertTrue(message = "At least one permission ID to add or remove is required")
    public boolean isChangeProvided() {
        return !add.isEmpty() || !remove.isEmpty();
    }

    @JsonIgnore
    @AssertTrue(message = "The same permission ID must not be both added and removed")
    public boolean isAddAndRemoveDisjoint() {
        return Collections.disjoint(add, remove);
    }
}
