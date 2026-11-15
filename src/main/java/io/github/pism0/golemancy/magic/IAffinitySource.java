package io.github.pism0.golemancy.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolves an affinity (a school id from core or material JSON) to something displayable
 * (SDD 3.2). The default implementation reads Iron's school registry.
 */
public interface IAffinitySource {
    /** Whether the school id exists. */
    boolean exists(ResourceLocation school);

    /** Display name of the school. */
    Component displayName(ResourceLocation school);

    IAffinitySource NONE = new IAffinitySource() {
        @Override
        public boolean exists(ResourceLocation school) {
            return false;
        }

        @Override
        public Component displayName(ResourceLocation school) {
            return Component.literal(school.toString());
        }
    };
}
