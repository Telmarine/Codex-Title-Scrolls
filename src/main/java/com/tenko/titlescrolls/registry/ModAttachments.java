package com.tenko.titlescrolls.registry;

import com.tenko.titlescrolls.TitleScrolls;
import com.tenko.titlescrolls.data.PlayerTitleData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Per-player persistent storage: which titles a player has unlocked, and which
 * one (if any) is currently active. Survives logout/relogin via NeoForge's
 * Data Attachment API — this is the ONLY place that state lives.
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TitleScrolls.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerTitleData>> PLAYER_TITLES =
            REGISTER.register("player_titles", () -> AttachmentType.builder(PlayerTitleData::empty)
                    .serialize(PlayerTitleData.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
