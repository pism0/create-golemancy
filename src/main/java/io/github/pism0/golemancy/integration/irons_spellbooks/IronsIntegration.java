package io.github.pism0.golemancy.integration.irons_spellbooks;

import io.github.pism0.golemancy.magic.IAffinitySource;
import io.github.pism0.golemancy.magic.MagicServices;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Registers the Iron's Spells adapters behind Golemancy's magic interfaces (SDD 3.2). */
public final class IronsIntegration {
    private IronsIntegration() {
    }

    public static void init() {
        MagicServices.register(new IronsSpellCaster(), new IronsAffinitySource());
    }

    /** {@link IAffinitySource} over Iron's school registry, so datapack and addon schools work too. */
    private static final class IronsAffinitySource implements IAffinitySource {
        @Override
        public boolean exists(ResourceLocation school) {
            return SchoolRegistry.getSchool(school) != null;
        }

        @Override
        public Component displayName(ResourceLocation school) {
            SchoolType type = SchoolRegistry.getSchool(school);
            return type == null ? Component.literal(school.toString()) : type.getDisplayName();
        }
    }
}
