package com.edgareldy.springboottutorial.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;

/**
 * Request body for {@code PATCH /api/v1/users/{id}/roles}. Carries the IDs of
 * the roles to add to the user and the IDs of the ones to remove from them.
 * Both sets are optional, but at least one must be non-empty and a role ID
 * cannot appear in both.
 * <p>
 * Created edgar.muhamyangabo on 11/12/26
 * Author : edgar.muhamyangabo
 * Date : 11/12/26
 * Project : spring-boot-tutorial
 */
public record UpdateUserRolesRequest(

        Set<@NotNull(message = "Role ID to add must not be null") Long> add,

        Set<@NotNull(message = "Role ID to remove must not be null") Long> remove
) {

    public UpdateUserRolesRequest {
        add = add == null ? Set.of() : add;
        remove = remove == null ? Set.of() : remove;
    }

    @JsonIgnore
    @AssertTrue(message = "At least one role ID to add or remove is required")
    public boolean isChangeProvided() {
        return !add.isEmpty() || !remove.isEmpty();
    }

    @JsonIgnore
    @AssertTrue(message = "The same role ID must not be both added and removed")
    public boolean isAddAndRemoveDisjoint() {
        return Collections.disjoint(add, remove);
    }
}
