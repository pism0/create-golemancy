package io.github.pism0.golemancy.gametest;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.lifecycle.GolemLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/** Shared helpers for the GameTests. */
final class GolemTestUtil {
    private GolemTestUtil() {
    }

    static final String PLATFORM = "gametest/platform";
    static final String ARENA = "gametest/arena";

    static GolemSpec spec(String frame, String material, String core) {
        return GolemSpec.create(Golemancy.id(frame), Golemancy.id(material), Golemancy.id(core));
    }

    /** (Template y = 0 is relative y = 1: relative coordinates start at the structure block.) Deploys a golem standing on the structure floor at the given relative x/z, through the real deploy path. */
    static ConstructEntity deploy(GameTestHelper helper, GolemSpec spec, int x, int z) {
        return deploy(helper, GolemLifecycle.dormantStack(spec, ConstructState.FRESH), x, z);
    }

    static ConstructEntity deploy(GameTestHelper helper, ItemStack dormant, int x, int z) {
        BlockPos floor = helper.absolutePos(new BlockPos(x, 1, z));
        ConstructEntity golem = GolemLifecycle.deploy(dormant, helper.getLevel(), floor, Direction.UP, null)
                .orElseThrow(() -> new IllegalStateException("deploy was refused"));
        // Skip the wake-up delay unless a test is about it.
        for (int i = 0; i <= ConstructEntity.ACTIVATION_TICKS; i++) {
            if (golem.getState() == ConstructEntity.State.ACTIVATING) golem.tick();
        }
        return golem;
    }

    static UUID someOwner() {
        return UUID.fromString("7f9c2a10-1111-4222-8333-444455556666");
    }

    /** Item entities inside the test structure. */
    static List<ItemEntity> itemsInStructure(GameTestHelper helper) {
        AABB bounds = helper.getBounds().inflate(2);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
    }
}
