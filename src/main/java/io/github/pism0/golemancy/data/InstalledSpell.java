package io.github.pism0.golemancy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** A spell installed in a golem: a spell id from the allow-list plus its level. */
public record InstalledSpell(ResourceLocation spell, int level) {
    public static final Codec<InstalledSpell> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("spell").forGetter(InstalledSpell::spell),
            Codec.intRange(1, 100).optionalFieldOf("level", 1).forGetter(InstalledSpell::level)
    ).apply(i, InstalledSpell::new));

    public static final StreamCodec<ByteBuf, InstalledSpell> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, InstalledSpell::spell,
            ByteBufCodecs.VAR_INT, InstalledSpell::level,
            InstalledSpell::new);
}
