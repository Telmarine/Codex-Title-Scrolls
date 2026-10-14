package com.tac5studios.codextitlescrolls.integration;

import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.data.TitleDefinition;
import com.tac5studios.codextitlescrolls.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Optional bridge to NeoEssentials' placeholder API. Only called when NeoEssentials is
 * installed (see TitleScrolls#commonSetup). Registers {active_title}: the player's active
 * title, or "" if none.
 *
 * <p>Done through reflection so Title Scrolls builds without the NeoEssentials jar.
 */
public class NeoEssentialsIntegration {

    public static void register() {
        try {
            Class<?> api = Class.forName("com.zerog.neoessentials.api.PlaceholderAPI");
            for (Method m : api.getMethods()) {
                if (!m.getName().equals("registerPlaceholder") || m.getParameterCount() != 2
                        || m.getParameterTypes()[0] != String.class || !m.getParameterTypes()[1].isInterface()) {
                    continue;
                }
                Class<?> handlerType = m.getParameterTypes()[1];
                Object handler = Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[]{handlerType},
                        (proxy, method, args) -> {
                            if (method.getDeclaringClass() == Object.class) {
                                return switch (method.getName()) {
                                    case "hashCode" -> System.identityHashCode(proxy);
                                    case "equals" -> proxy == args[0];
                                    default -> "TitleScrollsPlaceholder";
                                };
                            }
                            return args != null && args.length > 0 && args[0] instanceof ServerPlayer p ? resolve(p) : "";
                        });
                m.invoke(null, "active_title", handler);
                TitleScrolls.LOGGER.info("[TitleScrolls] Registered {{active_title}} placeholder with NeoEssentials.");
                return;
            }
            TitleScrolls.LOGGER.warn("[TitleScrolls] NeoEssentials found, but its placeholder API was not. {active_title} is not available.");
        } catch (Throwable t) {
            TitleScrolls.LOGGER.warn("[TitleScrolls] Could not register {active_title} with NeoEssentials: {}", t.toString());
        }
    }

    private static String resolve(ServerPlayer player) {
        var data = player.getData(ModAttachments.PLAYER_TITLES.get());
        return data.activeTitle()
                .flatMap(TitleScrolls.TITLE_REGISTRY::get)
                .map(TitleDefinition::display)
                .orElse("");
    }

    private NeoEssentialsIntegration() {}
}
