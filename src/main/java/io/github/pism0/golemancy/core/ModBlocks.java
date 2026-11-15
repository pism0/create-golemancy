package io.github.pism0.golemancy.core;

import io.github.pism0.golemancy.Golemancy;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golemancy blocks. v0.1 has exactly one custom block: the Binding Altar (SDD 8.2). */
public final class ModBlocks {
    private ModBlocks() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Golemancy.MOD_ID);
}
