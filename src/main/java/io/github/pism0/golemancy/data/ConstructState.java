package io.github.pism0.golemancy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Runtime state that travels with a packed-up golem but is not part of its design: stored energy,
 * health as a fraction of maximum, and the custom name. The {@code golemancy:construct_state} data
 * component on Dormant Golem items (SDD 11.2: "the spec, energy and installed spells are kept").
 */
public record ConstructState(float energy, float healthFraction, Optional<String> name) {
    /** Energy value meaning "full": resolved against the real maximum when the golem is deployed. */
    public static final float FULL = -1f;
    public static final ConstructState FRESH = new ConstructState(FULL, 1f, Optional.empty());

    public static final Codec<ConstructState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("energy", FULL).forGetter(ConstructState::energy),
            Codec.FLOAT.optionalFieldOf("health_fraction", 1f).forGetter(ConstructState::healthFraction),
            Codec.STRING.optionalFieldOf("name").forGetter(ConstructState::name)
    ).apply(i, ConstructState::new));

    public static final StreamCodec<ByteBuf, ConstructState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ConstructState::energy,
            ByteBufCodecs.FLOAT, ConstructState::healthFraction,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), ConstructState::name,
            ConstructState::new);
}
