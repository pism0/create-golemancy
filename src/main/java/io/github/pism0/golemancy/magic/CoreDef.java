package io.github.pism0.golemancy.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * A magical core (SDD 6). Data-driven: {@code data/<namespace>/golemancy/core/<id>.json}.
 *
 * @param item                the core item the Binding Altar recognises as this core
 * @param school              Iron's school id, or empty for the neutral Arcane Core
 * @param maxEnergy           base construct energy capacity
 * @param passiveDrain        energy drained per AI decision cycle while active (not per tick)
 * @param spellCostMultiplier converts a spell's Iron's mana cost into construct energy
 * @param tier                core tier; caps the level of installed spells
 * @param maxSpellLevel       highest spell level this core can drive
 * @param spellSlots          spell slots this core supports (0 = cannot cast, e.g. Arcane)
 * @param color               RGB colour of the core glow
 * @param resists             optional damage types this core lets its golem resist
 * @param strikeSlowTicks     Slowness duration added to melee hits (Frost Core)
 * @param strikeFireTicks     burn duration added to melee hits (Fire Core)
 */
public record CoreDef(
        Holder<Item> item,
        Optional<ResourceLocation> school,
        int maxEnergy,
        float passiveDrain,
        float spellCostMultiplier,
        int tier,
        int maxSpellLevel,
        int spellSlots,
        int color,
        Optional<TagKey<DamageType>> resists,
        int strikeSlowTicks,
        int strikeFireTicks
) {
    public static final Codec<CoreDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(CoreDef::item),
            ResourceLocation.CODEC.optionalFieldOf("school").forGetter(CoreDef::school),
            Codec.intRange(1, 1_000_000).fieldOf("max_energy").forGetter(CoreDef::maxEnergy),
            Codec.FLOAT.optionalFieldOf("passive_drain", 1f).forGetter(CoreDef::passiveDrain),
            Codec.FLOAT.optionalFieldOf("spell_cost_multiplier", 1f).forGetter(CoreDef::spellCostMultiplier),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(CoreDef::tier),
            Codec.INT.optionalFieldOf("max_spell_level", 1).forGetter(CoreDef::maxSpellLevel),
            Codec.INT.optionalFieldOf("spell_slots", 0).forGetter(CoreDef::spellSlots),
            Codec.INT.optionalFieldOf("color", 0xB48CFF).forGetter(CoreDef::color),
            TagKey.hashedCodec(Registries.DAMAGE_TYPE).optionalFieldOf("resists").forGetter(CoreDef::resists),
            Codec.INT.optionalFieldOf("strike_slow_ticks", 0).forGetter(CoreDef::strikeSlowTicks),
            Codec.INT.optionalFieldOf("strike_fire_ticks", 0).forGetter(CoreDef::strikeFireTicks)
    ).apply(i, CoreDef::new));
}
