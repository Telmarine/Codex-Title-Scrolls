package com.tac5studios.codextitlescrolls;

import com.tac5studios.codextitlescrolls.command.TitleCommand;
import com.tac5studios.codextitlescrolls.command.GiveCommand;
import com.tac5studios.codextitlescrolls.compat.OverlapGuard;
import com.tac5studios.codextitlescrolls.config.ClientSettings;
import com.tac5studios.codextitlescrolls.config.Feature;
import com.tac5studios.codextitlescrolls.config.Features;
import com.tac5studios.codextitlescrolls.config.Settings;
import com.tac5studios.codextitlescrolls.data.DatapackImport;
import com.tac5studios.codextitlescrolls.data.TitleStore;
import com.tac5studios.codextitlescrolls.integration.NeoEssentialsIntegration;
import com.tac5studios.codextitlescrolls.migrate.Migration;
import com.tac5studios.codextitlescrolls.network.ModNetworking;
import com.tac5studios.codextitlescrolls.registry.ModAttachments;
import com.tac5studios.codextitlescrolls.registry.ModDataComponents;
import com.tac5studios.codextitlescrolls.registry.ModItems;
import com.tac5studios.codextitlescrolls.registry.ModLoot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TitleScrolls.MOD_ID)
public class TitleScrolls {

    public static final String MOD_ID = "titlescrolls";
    public static final Logger LOGGER = LoggerFactory.getLogger(TitleScrolls.class);

    /** Every title, from config/titlescrolls/titles/. Chat mods read it by name (get(id) and getAll()). */
    public static final TitleStore TITLE_REGISTRY = new TitleStore();

    public TitleScrolls(IEventBus modEventBus, ModContainer container) {
        // config/titlescrolls/features.toml, settings.toml and titlescrolls-client.toml
        Features.register(container);
        Settings.register(container);
        ClientSettings.register(container);

        ModItems.REGISTER.register(modEventBus);
        ModDataComponents.REGISTER.register(modEventBus);
        ModAttachments.REGISTER.register(modEventBus);
        ModLoot.REGISTER.register(modEventBus);

        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        OverlapGuard.check();
        if (!Features.on(Feature.CHAT_BRIDGE)) return;
        if (ModList.get().isLoaded("neoessentials")) {
            event.enqueueWork(NeoEssentialsIntegration::register);
            LOGGER.info("[TitleScrolls] NeoEssentials detected — registering the {{active_title}} chat placeholder.");
        } else {
            LOGGER.info("[TitleScrolls] NeoEssentials not present — chat-placeholder bridge skipped. "
                    + "Title unlocks and the /title GUI are fully functional regardless.");
        }
    }

    /** Titles load before anyone can join. Datapack titles from 1.0.2 are copied over the first time. */
    private void onServerStarting(ServerStartingEvent event) {
        Migration.createTemplate();
        DatapackImport.runOnce(event.getServer());
        TITLE_REGISTRY.load(event.getServer());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        if (Features.on(Feature.TITLE_MENU)) TitleCommand.register(event.getDispatcher());
        GiveCommand.register(event.getDispatcher());
    }
}