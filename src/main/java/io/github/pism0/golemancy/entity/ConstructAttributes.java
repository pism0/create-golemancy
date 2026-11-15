package io.github.pism0.golemancy.entity;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.construction.FrameDef;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.magic.CoreDef;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Derives a golem's attributes from its {@link GolemSpec} (SDD 10.2):
 * {@code final = frame base × material multiplier × config multiplier}.
 *
 * <p>Values are applied as transient attribute modifiers with fixed ids, so recalculating replaces
 * them instead of stacking, and nothing derived is ever written to the save.
 */
public final class ConstructAttributes {
    private ConstructAttributes() {
    }

    private static final ResourceLocation HEALTH = Golemancy.id("spec_health");
    private static final ResourceLocation ARMOR = Golemancy.id("spec_armor");
    private static final ResourceLocation ATTACK = Golemancy.id("spec_attack_damage");
    private static final ResourceLocation SPEED = Golemancy.id("spec_movement_speed");
    private static final ResourceLocation KNOCKBACK = Golemancy.id("spec_knockback_resistance");
    private static final ResourceLocation FOLLOW = Golemancy.id("spec_follow_range");

    /** The numbers a spec resolves to. */
    public record Resolved(float health, float armor, float attackDamage, float movementSpeed,
                           float knockbackResistance, float followRange, float maxEnergy, float spellPower,
                           int attackInterval) {
    }

    public static Resolved resolve(GolemSpec spec, RegistryAccess access) {
        FrameDef frame = GolemDefs.frame(access, spec.frame())
                .or(() -> GolemDefs.frame(access, GolemSpec.DEFAULT_FRAME)).orElseThrow(
                        () -> new IllegalStateException("No golem frame definitions are loaded"));
        MaterialDef material = GolemDefs.material(access, spec.material())
                .or(() -> GolemDefs.material(access, GolemSpec.DEFAULT_MATERIAL)).orElseThrow(
                        () -> new IllegalStateException("No golem material definitions are loaded"));
        CoreDef core = spec.hasCore() ? GolemDefs.core(access, spec.core()).orElse(null) : null;

        float durability = GolemancyConfig.DURABILITY_MULTIPLIER.get().floatValue();
        float damage = GolemancyConfig.DAMAGE_MULTIPLIER.get().floatValue();

        float health = Math.max(1f, frame.health() * material.healthMultiplier() * durability);
        float armor = Math.min(30f, frame.armor() * material.armorMultiplier());
        float attack = frame.attackDamage() * damage;
        float speed = frame.movementSpeed() * material.speedMultiplier();
        float knockback = Math.min(1f, frame.knockbackResistance() + material.knockbackResistance());
        // A damaged core holds less: capacity runs from 50% (integrity 0) to 100% (integrity 1).
        float integrityFactor = 0.5f + 0.5f * spec.coreIntegrity();
        float maxEnergy = core == null ? 0f
                : core.maxEnergy() * frame.energyMultiplier() * material.conductivity() * integrityFactor;
        float spellPower = material.conductivity() * integrityFactor;
        return new Resolved(health, armor, attack, speed, knockback, frame.followRange(), maxEnergy, spellPower,
                frame.attackInterval());
    }

    /** Applies the resolved values to the entity's attributes. Safe to call any number of times. */
    public static void apply(LivingEntity entity, Resolved r) {
        set(entity, Attributes.MAX_HEALTH, HEALTH, r.health());
        set(entity, Attributes.ARMOR, ARMOR, r.armor());
        set(entity, Attributes.ATTACK_DAMAGE, ATTACK, r.attackDamage());
        set(entity, Attributes.MOVEMENT_SPEED, SPEED, r.movementSpeed());
        set(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, r.knockbackResistance());
        set(entity, Attributes.FOLLOW_RANGE, FOLLOW, r.followRange());
    }

    private static void set(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double target) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.addOrUpdateTransientModifier(
                new AttributeModifier(id, target - instance.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
    }
}
