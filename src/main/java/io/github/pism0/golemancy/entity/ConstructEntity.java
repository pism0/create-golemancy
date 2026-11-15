package io.github.pism0.golemancy.entity;

import com.mojang.serialization.DataResult;
import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.ai.JobRunnerGoal;
import io.github.pism0.golemancy.construction.FrameDef;
import io.github.pism0.golemancy.construction.MaterialDef;
import io.github.pism0.golemancy.core.GolemancyConfig;
import io.github.pism0.golemancy.core.ModRegistries;
import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.data.ConstructState;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.lifecycle.GolemLifecycle;
import io.github.pism0.golemancy.lifecycle.Ownership;
import io.github.pism0.golemancy.magic.ConstructEnergy;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.IConstructEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * A construct: one class, registered once per frame as an entity type (SDD 10). The
 * {@link GolemSpec} is the single source of truth; attributes, texture and energy capacity are
 * derived from it whenever it loads or changes.
 */
public class ConstructEntity extends PathfinderMob implements GeoEntity, OwnableEntity {

    /** Lifecycle state of a deployed construct (SDD 11.1). */
    public enum State {
        ACTIVE, ACTIVATING, POWERED_DOWN;

        static State byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : ACTIVE;
        }
    }

    public static final int ACTIVATION_TICKS = 40;
    private static final int DEATH_TICKS = 30;

    // Only what the renderer and UI need is synced (SDD 16.1); the full spec is sent on open-screen.
    private static final EntityDataAccessor<String> DATA_MATERIAL =
            SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CORE =
            SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_ENERGY =
            SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(ConstructEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ANIM_DORMANT = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation ANIM_ACTIVATE = RawAnimation.begin().thenPlay("activate");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation ANIM_CAST = RawAnimation.begin().thenPlay("cast");
    private static final RawAnimation ANIM_HURT = RawAnimation.begin().thenPlay("hurt");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ConstructEnergy energy = new ConstructEnergy();

    @Nullable
    private GolemSpec spec;
    @Nullable
    private ConstructAttributes.Resolved resolved;
    /** Set when saved data could not be decoded: the golem becomes Remains on its next tick. */
    @Nullable
    private GolemSpec salvagedSpec;
    private float pendingHealth = -1f;
    private float pendingEnergy = ConstructState.FULL;
    private int activateTicks;
    private boolean abandoned;
    private float absorbedInWindow;
    private long absorbWindowStart;
    private long lastFieldRepair = Long.MIN_VALUE / 2;
    private long nextSpellTick;
    private float lastSyncedEnergy = -1f;

    public ConstructEntity(EntityType<? extends ConstructEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MATERIAL, GolemSpec.DEFAULT_MATERIAL.toString());
        builder.define(DATA_CORE, GolemSpec.DEFAULT_CORE.toString());
        builder.define(DATA_ENERGY, 1f);
        builder.define(DATA_STATE, (byte) State.ACTIVE.ordinal());
        builder.define(DATA_OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new JobRunnerGoal(this));
    }

    // ------------------------------------------------------------------------------------------
    // Spec
    // ------------------------------------------------------------------------------------------

    /** The golem's spec. Never null on the server once the entity has ticked or been deployed. */
    public GolemSpec getSpec() {
        if (spec == null) {
            spec = defaultSpecForType();
        }
        return spec;
    }

    /**
     * Replaces the spec (sanitised first), then recalculates everything derived from it. The only
     * way the spec changes; callers must have done their ownership checks.
     */
    public void applySpec(GolemSpec newSpec) {
        this.spec = GolemDefs.sanitize(newSpec, level().registryAccess());
        recalculate();
    }

    private GolemSpec defaultSpecForType() {
        ResourceLocation typeId = EntityType.getKey(getType());
        ResourceLocation frame = level().registryAccess().registry(ModRegistries.FRAME)
                .flatMap(reg -> reg.entrySet().stream()
                        .filter(e -> e.getValue().entityType().equals(typeId))
                        .map(e -> e.getKey().location())
                        .findFirst())
                .orElse(GolemSpec.DEFAULT_FRAME);
        GolemSpec fresh = GolemSpec.create(frame, GolemSpec.DEFAULT_MATERIAL, GolemSpec.DEFAULT_CORE);
        return fresh.withBehavior(fresh.behavior().withProfile(
                GolemDefs.frame(level().registryAccess(), frame).map(FrameDef::defaultProfile)
                        .orElse(BehaviorConfig.Profile.GUARDIAN)));
    }

    /** Recomputes attributes, energy capacity and synced render data from the spec (SDD 10.2). */
    public void recalculate() {
        if (level().isClientSide) return;
        GolemSpec current = getSpec();
        float healthFraction = getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1f;
        boolean first = resolved == null;
        resolved = ConstructAttributes.resolve(current, level().registryAccess());
        ConstructAttributes.apply(this, resolved);
        energy.setMax(resolved.maxEnergy());
        if (first) {
            // First resolve after spawn or load: restore saved health/energy against the real maxima.
            setHealth(pendingHealth >= 0f ? Math.min(pendingHealth, getMaxHealth()) : getMaxHealth());
            energy.set(pendingEnergy < 0f ? resolved.maxEnergy() : pendingEnergy);
        } else {
            setHealth(Math.max(1f, healthFraction * getMaxHealth()));
        }
        entityData.set(DATA_MATERIAL, current.material().toString());
        entityData.set(DATA_CORE, current.core().toString());
        entityData.set(DATA_OWNER, current.hasOwner() ? Optional.of(current.owner()) : Optional.empty());
        syncState();
    }

    public ConstructAttributes.Resolved getResolved() {
        if (resolved == null) recalculate();
        return resolved;
    }

    @Nullable
    public MaterialDef getMaterialDef() {
        return GolemDefs.material(level().registryAccess(), getMaterialId()).orElse(null);
    }

    @Nullable
    public CoreDef getCoreDef() {
        return GolemDefs.core(level().registryAccess(), getCoreId()).orElse(null);
    }

    /** Synced: usable on both sides. */
    public ResourceLocation getMaterialId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_MATERIAL));
        return id == null ? GolemSpec.DEFAULT_MATERIAL : id;
    }

    /** Synced: usable on both sides. */
    public ResourceLocation getCoreId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_CORE));
        return id == null ? GolemSpec.DEFAULT_CORE : id;
    }

    // ------------------------------------------------------------------------------------------
    // State and energy
    // ------------------------------------------------------------------------------------------

    public State getState() {
        return State.byId(entityData.get(DATA_STATE));
    }

    public boolean isPoweredDown() {
        return getState() == State.POWERED_DOWN;
    }

    public boolean isActivating() {
        return getState() == State.ACTIVATING;
    }

    /** Server-side energy store. */
    public IConstructEnergy getEnergy() {
        return energy;
    }

    /** Synced energy fraction (0..1) for the renderer and the UI. */
    public float getEnergyFraction() {
        return entityData.get(DATA_ENERGY);
    }

    /** Sets stored energy directly (deploying a packed-up golem, tests). */
    public void setEnergy(float value) {
        getResolved();
        energy.set(value);
        syncState();
    }

    /** Drains energy scaled by the server's drain multiplier. Returns false if it could not be afforded. */
    public boolean consumeEnergy(float amount) {
        float scaled = amount * GolemancyConfig.ENERGY_DRAIN_MULTIPLIER.get().floatValue();
        if (!energy.canAfford(scaled)) return false;
        energy.drain(scaled);
        syncState();
        return true;
    }

    /** Adds energy; a powered-down golem wakes up (SDD 6.4). Returns the energy actually stored. */
    public float rechargeEnergy(float amount) {
        boolean wasDown = isPoweredDown();
        float stored = energy.recharge(amount);
        syncState();
        if (wasDown && !isPoweredDown()) {
            beginActivation();
        }
        return stored;
    }

    /** Starts the wake-up sequence: the golem stands still for {@link #ACTIVATION_TICKS}. */
    public void beginActivation() {
        if (level().isClientSide) return;
        activateTicks = ACTIVATION_TICKS;
        syncState();
        playSound(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.4f);
    }

    private State computeState() {
        if (abandoned || energy.isEmpty() || !getSpec().hasCore()) return State.POWERED_DOWN;
        if (activateTicks > 0) return State.ACTIVATING;
        return State.ACTIVE;
    }

    private void syncState() {
        if (level().isClientSide) return;
        State before = getState();
        State now = computeState();
        if (before != now) {
            entityData.set(DATA_STATE, (byte) now.ordinal());
            if (now == State.POWERED_DOWN) {
                getNavigation().stop();
                setTarget(null);
                playSound(SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.2f);
            }
        }
        float fraction = energy.getFraction();
        if (Math.abs(fraction - lastSyncedEnergy) >= 0.01f || fraction == 0f || fraction == 1f) {
            lastSyncedEnergy = fraction;
            entityData.set(DATA_ENERGY, fraction);
        }
    }

    /** True on the ticks this golem makes AI decisions: staggered so golems do not all think at once. */
    public boolean isDecisionTick() {
        int interval = Math.max(1, GolemancyConfig.DECISION_INTERVAL.get());
        return (tickCount + getId()) % interval == 0;
    }

    public long getNextSpellTick() {
        return nextSpellTick;
    }

    public void setNextSpellTick(long tick) {
        this.nextSpellTick = tick;
    }

    /** The point a Guardian holds: its configured home, or where it stands if none is set. */
    public BlockPos getAnchor() {
        return getSpec().behavior().home().orElseGet(this::blockPosition);
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isPoweredDown() && tickCount % 40 == 0 && isAlive()) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.6, getZ(), 0, 0.02, 0);
            }
            return;
        }
        if (salvagedSpec != null) {
            // SDD 15.3: undecodable data never deletes a golem silently; it becomes Remains.
            GolemSpec salvage = salvagedSpec;
            salvagedSpec = null;
            Golemancy.LOGGER.error("Golem {} at {} had unreadable data; replacing it with Construct Remains", getUUID(), blockPosition());
            spawnAtLocation(GolemLifecycle.remainsStack(salvage));
            discard();
            return;
        }
        if (resolved == null) {
            recalculate();
        }
        if (!isAlive()) return;

        if (activateTicks > 0 && --activateTicks == 0) {
            syncState();
        }

        if (isDecisionTick()) {
            decisionTick();
        }
        if (tickCount % 20 == 0) {
            MaterialDef material = getMaterialDef();
            if (material != null && material.waterRegen() > 0f && isInWaterRainOrBubble() && getHealth() < getMaxHealth()) {
                heal(material.waterRegen());
            }
        }
    }

    /** Runs on the staggered decision interval: passive drain and the abandoned-owner check. */
    private void decisionTick() {
        CoreDef core = getCoreDef();
        if (core != null && getState() != State.POWERED_DOWN) {
            energy.drain(core.passiveDrain() * GolemancyConfig.ENERGY_DRAIN_MULTIPLIER.get().floatValue());
        }
        updateAbandoned();
        syncState();
    }

    private void updateAbandoned() {
        int timeoutMinutes = GolemancyConfig.ABANDONED_TIMEOUT_MINUTES.get();
        UUID owner = getOwnerUUID();
        if (timeoutMinutes <= 0 || owner == null || level().getServer() == null) {
            abandoned = false;
            return;
        }
        boolean wasAbandoned = abandoned;
        abandoned = GolemLifecycle.ownerOfflineLongerThan(level().getServer(), owner, timeoutMinutes);
        if (wasAbandoned && !abandoned && !energy.isEmpty()) {
            beginActivation();
        }
    }

    @Override
    protected boolean isImmobile() {
        // Powered down or waking up: no pathfinding, no AI movement (SDD 11.1).
        return super.isImmobile() || getState() != State.ACTIVE;
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Combat and damage (SDD 10.3)
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isInvulnerableTo(source) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        MaterialDef material = getMaterialDef();
        CoreDef core = getCoreDef();
        if (material != null && source.is(material.absorbs())) {
            amount = absorb(amount);
            if (amount <= 0f) return false;
        }
        if (material != null && source.is(material.resists())) {
            amount *= material.resistFactor();
        }
        if (core != null && core.resists().isPresent() && source.is(core.resists().get())) {
            amount *= 0.5f;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive()) {
            triggerAnim("action", "hurt");
        }
        return hurt;
    }

    /**
     * Absorption: a fraction of the damage heals instead, up to a cap per time window. Returns the
     * damage that is left over and must be taken normally (so absorption can never make a golem
     * immortal).
     */
    private float absorb(float amount) {
        float fraction = GolemancyConfig.ABSORPTION_FRACTION.get().floatValue();
        float cap = GolemancyConfig.ABSORPTION_CAP.get().floatValue();
        long now = level().getGameTime();
        if (now - absorbWindowStart >= GolemancyConfig.ABSORPTION_WINDOW_TICKS.get()) {
            absorbWindowStart = now;
            absorbedInWindow = 0f;
        }
        float room = cap - absorbedInWindow;
        if (fraction <= 0f || room <= 0f) return amount;
        float healed = Math.min(amount * fraction, room);
        heal(healed);
        absorbedInWindow += healed;
        return amount - healed / fraction;
    }

    /** Health restored by absorption in the current window (for the UI and tests). */
    public float getAbsorbedInWindow() {
        return absorbedInWindow;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            MaterialDef material = getMaterialDef();
            CoreDef core = getCoreDef();
            int slow = (material == null ? 0 : material.strikeSlowTicks()) + (core == null ? 0 : core.strikeSlowTicks());
            if (slow > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1), this);
            }
            if (core != null && core.strikeFireTicks() > 0) {
                living.igniteForTicks(core.strikeFireTicks());
            }
        }
        return hit;
    }

    /** Plays the attack animation on clients. */
    public void playAttackAnimation() {
        triggerAnim("action", "attack");
    }

    /** Plays the cast animation on clients. */
    public void playCastAnimation() {
        triggerAnim("action", "cast");
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return Ownership.mayAttack(this, target) && super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !level().isClientSide && !Ownership.mayAttack(this, target)) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return Ownership.isAlly(this, other) || super.isAlliedTo(other);
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.dead;
        super.die(source);
        if (!wasDead && !level().isClientSide && this.dead) {
            // SDD 11.3: a destroyed golem always leaves Remains carrying its spec.
            ItemEntity remains = spawnAtLocation(GolemLifecycle.remainsAfterDeath(this));
            if (remains != null) {
                remains.setUnlimitedLifetime();
                remains.setInvulnerable(true);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Interaction (every branch is validated on the server)
    // ------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        return GolemLifecycle.interact(this, serverPlayer, player.getItemInHand(hand));
    }

    /**
     * Field repair (SDD 11.3): restores a fraction of health, costs energy and has a cooldown.
     *
     * @return true if the repair was applied
     */
    public boolean fieldRepair() {
        long now = level().getGameTime();
        if (getHealth() >= getMaxHealth()) return false;
        if (now - lastFieldRepair < GolemancyConfig.FIELD_REPAIR_COOLDOWN.get()) return false;
        float cost = energy.getMaxEnergy() * GolemancyConfig.FIELD_REPAIR_ENERGY_COST.get().floatValue();
        if (cost > 0f && !isPoweredDown() && !energy.canAfford(cost)) return false;
        energy.drain(cost);
        lastFieldRepair = now;
        heal(getMaxHealth() * GolemancyConfig.FIELD_REPAIR_HEALTH.get().floatValue());
        syncState();
        playSound(SoundEvents.ANVIL_USE, 0.5f, 1.3f);
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Ownership
    // ------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        if (level().isClientSide) {
            return entityData.get(DATA_OWNER).orElse(null);
        }
        GolemSpec current = getSpec();
        return current.hasOwner() ? current.owner() : null;
    }

    // ------------------------------------------------------------------------------------------
    // Persistence (SDD 15.3)
    // ------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (salvagedSpec != null) {
            // Still unreadable and not yet converted: keep the best-effort spec rather than lose it.
            GolemSpec.CODEC.encodeStart(NbtOps.INSTANCE, salvagedSpec).result().ifPresent(t -> tag.put("GolemSpec", t));
            return;
        }
        GolemSpec.CODEC.encodeStart(NbtOps.INSTANCE, getSpec())
                .resultOrPartial(error -> Golemancy.LOGGER.error("Could not save golem spec: {}", error))
                .ifPresent(encoded -> tag.put("GolemSpec", encoded));
        tag.putFloat("GolemEnergy", energy.getEnergy());
        // Max health comes from transient modifiers, so vanilla's saved Health would be clamped to the
        // base value on load. Keep the real value separately.
        tag.putFloat("GolemHealth", getHealth());
        tag.putInt("ActivateTicks", activateTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("GolemSpec")) {
            return; // freshly summoned: the default spec for this entity type is used
        }
        Tag raw = tag.get("GolemSpec");
        // 1. decode (2. the codec runs GolemSpecMigrator)
        DataResult<GolemSpec> parsed = GolemSpec.CODEC.parse(NbtOps.INSTANCE, raw);
        if (parsed.isError() || parsed.result().isEmpty()) {
            Golemancy.LOGGER.error("Could not read golem spec for {}: {}", getUUID(),
                    parsed.error().map(DataResult.Error::message).orElse("unknown error"));
            this.salvagedSpec = GolemLifecycle.salvage(raw);
            return;
        }
        // 3. unknown ids -> safe defaults; 4. attributes are recalculated on the first tick
        this.spec = GolemDefs.sanitize(parsed.result().get(), level().registryAccess());
        this.resolved = null;
        this.pendingEnergy = tag.contains("GolemEnergy") ? tag.getFloat("GolemEnergy") : ConstructState.FULL;
        this.pendingHealth = tag.contains("GolemHealth") ? tag.getFloat("GolemHealth") : -1f;
        this.activateTicks = tag.getInt("ActivateTicks");
    }

    // ------------------------------------------------------------------------------------------
    // Mob behaviour tweaks
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        // SDD 15.3: entity travel through portals is disabled in v0.1; pack the golem up instead.
        return false;
    }

    @Override
    public boolean isPushable() {
        return getState() == State.ACTIVE && super.isPushable();
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DEEPSLATE_BRICKS_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DEEPSLATE_BREAK;
    }

    @Override
    protected Component getTypeName() {
        // "Stone Guardian", "Clay Brute": material + frame, never a vanilla mob name (SDD 10.1).
        return Component.translatable("entity.golemancy.construct.name",
                Component.translatable("golemancy.material." + getMaterialId().toLanguageKey()),
                super.getTypeName());
    }

    // ------------------------------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainAnimation));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("attack", ANIM_ATTACK)
                .triggerableAnim("cast", ANIM_CAST)
                .triggerableAnim("hurt", ANIM_HURT));
    }

    private PlayState mainAnimation(AnimationState<ConstructEntity> state) {
        if (isDeadOrDying()) return state.setAndContinue(ANIM_DEATH);
        return switch (getState()) {
            case POWERED_DOWN -> state.setAndContinue(ANIM_DORMANT);
            case ACTIVATING -> state.setAndContinue(ANIM_ACTIVATE);
            case ACTIVE -> state.setAndContinue(state.isMoving() ? ANIM_WALK : ANIM_IDLE);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // ------------------------------------------------------------------------------------------
    // Lifecycle helpers used by GolemLifecycle
    // ------------------------------------------------------------------------------------------

    /** Loads the runtime state of a packed-up golem into this freshly created entity. */
    public void loadFromItem(GolemSpec itemSpec, ConstructState state) {
        this.pendingEnergy = state.energy();
        this.pendingHealth = -1f;
        this.resolved = null;
        applySpec(itemSpec);
        setHealth(Math.max(1f, getMaxHealth() * Math.max(0f, Math.min(1f, state.healthFraction()))));
        state.name().ifPresent(name -> setCustomName(Component.literal(name)));
    }

    /** Captures the runtime state for packing this golem into an item. */
    public ConstructState captureState() {
        return new ConstructState(energy.getEnergy(), getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1f,
                Optional.ofNullable(getCustomName()).map(Component::getString));
    }

    /** Server level accessor that fails loudly on the client. */
    public ServerLevel serverLevel() {
        return (ServerLevel) level();
    }
}
