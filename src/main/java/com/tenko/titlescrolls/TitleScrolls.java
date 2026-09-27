package com.tenko.titlescrolls;

import com.tenko.titlescrolls.command.TitleCommand;
import com.tenko.titlescrolls.data.TitleReloadListener;
import com.tenko.titlescrolls.integration.NeoEssentialsIntegration;
import com.tenko.titlescrolls.network.ModNetworking;
import com.tenko.titlescrolls.registry.ModAttachments;
import com.tenko.titlescrolls.registry.ModDataComponents;
import com.tenko.titlescrolls.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TitleScrolls.MOD_ID)
public class TitleScrolls {

    public static final String MOD_ID = "titlescrolls";
    public static final Logger LOGGER = LoggerFactory.getLogger(TitleScrolls.class);

    public static final TitleReloadListener TITLE_REGISTRY = new TitleReloadListener();

    public TitleScrolls(IEventBus modEventBus) {
        ModItems.REGISTER.register(modEventBus);
        ModDataComponents.REGISTER.register(modEventBus);
        ModAttachments.REGISTER.register(modEventBus);

        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("neoessentials")) {
            event.enqueueWork(NeoEssentialsIntegration::register);
            LOGGER.info("[TitleScrolls] NeoEssentials detected — registering the {{active_title}} chat placeholder.");
        } else {
            LOGGER.info("[TitleScrolls] NeoEssentials not present — chat-placeholder bridge skipped. "
                    + "Title unlocks and the /title GUI are fully functional regardless.");
        }
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(TITLE_REGISTRY);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TitleCommand.register(event.getDispatcher());
    }
}