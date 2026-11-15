package io.github.pism0.golemancy.ai;

import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.lifecycle.Ownership;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Target selection. Runs only on decision ticks (SDD 16.1), never every tick, and always goes
 * through {@link Ownership} so golems cannot be turned on their owner or allies.
 */
public final class Targeting {
    private Targeting() {
    }

    /** The nearest hostile within {@code radius} of {@code center}, or null. */
    @Nullable
    public static LivingEntity nearestHostile(ConstructEntity golem, Vec3 center, double radius) {
        if (!GolemancyConfig.AUTONOMOUS_COMBAT.get()) return null;
        AABB box = AABB.ofSize(center, radius * 2, radius, radius * 2);
        double radiusSqr = radius * radius;
        List<LivingEntity> candidates = golem.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != golem && Ownership.isHostile(golem, e) && e.position().distanceToSqr(center) <= radiusSqr);
        return candidates.stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    }

    /** Whoever most recently attacked the golem or its owner, if the golem is allowed to hit back. */
    @Nullable
    public static LivingEntity aggressor(ConstructEntity golem) {
        LivingEntity attacker = golem.getLastHurtByMob();
        if (attacker != null && Ownership.mayAttack(golem, attacker)) return attacker;
        LivingEntity owner = golem.getOwner();
        if (owner != null) {
            LivingEntity ownerAttacker = owner.getLastHurtByMob();
            if (ownerAttacker != null && Ownership.mayAttack(golem, ownerAttacker)
                    && owner.tickCount - owner.getLastHurtByMobTimestamp() < 200) {
                return ownerAttacker;
            }
            LivingEntity ownerTarget = owner.getLastHurtMob();
            if (ownerTarget != null && Ownership.mayAttack(golem, ownerTarget)
                    && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
                return ownerTarget;
            }
        }
        return null;
    }

    public static boolean stillValid(ConstructEntity golem, @Nullable LivingEntity target, Vec3 center, double leash) {
        return target != null && target.isAlive() && Ownership.mayAttack(golem, target)
                && target.level() == golem.level()
                && target.position().distanceToSqr(center) <= leash * leash;
    }

    public static Vec3 centerOf(BlockPos pos) {
        return Vec3.atBottomCenterOf(pos);
    }
}
