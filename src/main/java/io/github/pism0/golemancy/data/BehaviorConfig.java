package io.github.pism0.golemancy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;
import java.util.function.IntFunction;

/**
 * How a golem behaves (SDD 12). Stored inside {@link GolemSpec}. Only ever changed on the server,
 * after an ownership check.
 */
public record BehaviorConfig(Profile profile, Optional<BlockPos> home, int radius, boolean combat,
                             MoveMode moveMode, boolean castSpells) {

    public static final int MIN_RADIUS = 2;
    public static final int MAX_RADIUS = 32;
    public static final int DEFAULT_RADIUS = 12;

    public static final BehaviorConfig DEFAULT =
            new BehaviorConfig(Profile.GUARDIAN, Optional.empty(), DEFAULT_RADIUS, true, MoveMode.STAY, true);

    public static final Codec<BehaviorConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Profile.CODEC.optionalFieldOf("profile", Profile.GUARDIAN).forGetter(BehaviorConfig::profile),
            BlockPos.CODEC.optionalFieldOf("home").forGetter(BehaviorConfig::home),
            Codec.INT.optionalFieldOf("radius", DEFAULT_RADIUS).forGetter(BehaviorConfig::radius),
            Codec.BOOL.optionalFieldOf("combat", true).forGetter(BehaviorConfig::combat),
            MoveMode.CODEC.optionalFieldOf("move_mode", MoveMode.STAY).forGetter(BehaviorConfig::moveMode),
            Codec.BOOL.optionalFieldOf("cast_spells", true).forGetter(BehaviorConfig::castSpells)
    ).apply(i, BehaviorConfig::new));

    public static final StreamCodec<ByteBuf, BehaviorConfig> STREAM_CODEC = StreamCodec.composite(
            Profile.STREAM_CODEC, BehaviorConfig::profile,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), BehaviorConfig::home,
            ByteBufCodecs.VAR_INT, BehaviorConfig::radius,
            ByteBufCodecs.BOOL, BehaviorConfig::combat,
            MoveMode.STREAM_CODEC, BehaviorConfig::moveMode,
            ByteBufCodecs.BOOL, BehaviorConfig::castSpells,
            BehaviorConfig::new);

    public BehaviorConfig {
        radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
    }

    /** A profile switch also selects that profile's natural movement mode. */
    public BehaviorConfig withProfile(Profile p) {
        return new BehaviorConfig(p, home, radius, combat, p == Profile.SOLDIER ? MoveMode.FOLLOW : MoveMode.STAY, castSpells);
    }

    public BehaviorConfig withHome(Optional<BlockPos> h) {
        return new BehaviorConfig(profile, h, radius, combat, moveMode, castSpells);
    }

    public BehaviorConfig withRadius(int r) {
        return new BehaviorConfig(profile, home, r, combat, moveMode, castSpells);
    }

    public BehaviorConfig withCombat(boolean c) {
        return new BehaviorConfig(profile, home, radius, c, moveMode, castSpells);
    }

    public BehaviorConfig withMoveMode(MoveMode m) {
        return new BehaviorConfig(profile, home, radius, combat, m, castSpells);
    }

    public BehaviorConfig withCastSpells(boolean c) {
        return new BehaviorConfig(profile, home, radius, combat, moveMode, c);
    }

    /** v0.1 behaviour profiles (SDD 12.1). */
    public enum Profile implements StringRepresentable {
        GUARDIAN(0, "guardian"),
        SOLDIER(1, "soldier");

        public static final Codec<Profile> CODEC = StringRepresentable.fromEnum(Profile::values);
        private static final IntFunction<Profile> BY_ID =
                ByIdMap.continuous(p -> p.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Profile> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p -> p.id);

        private final int id;
        private final String name;

        Profile(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum MoveMode implements StringRepresentable {
        STAY(0, "stay"),
        FOLLOW(1, "follow");

        public static final Codec<MoveMode> CODEC = StringRepresentable.fromEnum(MoveMode::values);
        private static final IntFunction<MoveMode> BY_ID =
                ByIdMap.continuous(m -> m.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, MoveMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, m -> m.id);

        private final int id;
        private final String name;

        MoveMode(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
