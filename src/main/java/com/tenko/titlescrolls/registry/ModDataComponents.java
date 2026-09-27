package com.tenko.titlescrolls.registry;

import com.mojang.serialization.Codec;
import com.tenko.titlescrolls.TitleScrolls;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The single data component that turns a plain Title Scroll item stack into a
 * scroll for one SPECIFIC title. Storing the title's id here (rather than
 * registering one Item per title) is what lets new titles be added purely
 * through datapack JSON — see data/TitleReloadListener.java.
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTER =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TitleScrolls.MOD_ID);

    // Value is the title's plain string id (matches the JSON file name under
    // data/<namespace>/titles/, e.g. "kitsune"), never a display name.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> GRANTS_TITLE =
            REGISTER.register("grants_title", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ModDataComponents() {}
}
