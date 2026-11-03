package com.tac5studios.codextitlescrolls.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac5studios.codextitlescrolls.TitleScrolls;
import com.tac5studios.codextitlescrolls.compat.OverlapGuard;
import com.tac5studios.codextitlescrolls.config.ClientSettings;
import com.tac5studios.codextitlescrolls.config.Feature;
import com.tac5studios.codextitlescrolls.network.NametagStylePayload;
import com.tac5studios.codextitlescrolls.network.PlayerTitlePayload;
import com.tac5studios.codextitlescrolls.util.ColorCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws other players' active titles on them: as its own line above the name tag, or after the name.
 * The name tag itself is left alone, so rank prefixes from other mods still show.
 * Hides whenever the name tag hides (sneaking, invisible, spectator, too far away).
 */
@EventBusSubscriber(modid = TitleScrolls.MOD_ID, value = Dist.CLIENT)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class ClientNametags {

    /** One line of name tag text, in world units (same as vanilla's below-name line). */
    private static final float LINE = 9.0F * 1.15F * 0.025F;

    /** Title text as sent by the server, and the same text formatted with the server's format. */
    private static final Map<UUID, String> RAW = new ConcurrentHashMap<>();
    private static final Map<UUID, Component> TITLES = new ConcurrentHashMap<>();
    private static volatile NametagStylePayload style = NametagStylePayload.OFF;
    private static Method shouldShowName;
    private static boolean reflectionFailed;

    private ClientNametags() {}

    public static void onStyle(NametagStylePayload payload) {
        style = payload;
        if (!payload.enabled()) {
            RAW.clear();
            TITLES.clear();
            return;
        }
        RAW.forEach((id, display) -> TITLES.put(id, format(display)));
    }

    public static void onTitle(PlayerTitlePayload payload) {
        if (payload.display().isEmpty()) {
            RAW.remove(payload.player());
            TITLES.remove(payload.player());
        } else {
            RAW.put(payload.player(), payload.display());
            TITLES.put(payload.player(), format(payload.display()));
        }
    }

    /** The server's format with the title in place of {title}. The title keeps its own colors. */
    private static Component format(String display) {
        String fmt = style.format();
        int at = fmt.indexOf("{title}");
        if (at < 0) return ColorCodes.translate(display);
        MutableComponent out = Component.empty();
        out.append(ColorCodes.translate(fmt.substring(0, at)));
        out.append(ColorCodes.translate(display));
        out.append(ColorCodes.translate(fmt.substring(at + "{title}".length())));
        return out;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        RAW.clear();
        TITLES.clear();
        style = NametagStylePayload.OFF;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onNameTag(RenderNameTagEvent e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer player)) return;
        if (!style.enabled() || !ClientSettings.showTitles() || OverlapGuard.blocks(Feature.NAMETAG_TITLES)) return;
        if (player == Minecraft.getInstance().player) return;
        Component title = TITLES.get(player.getUUID());
        if (title == null) return;

        if ("beside".equals(style.position())) {
            e.setContent(Component.empty().append(e.getContent()).append(" ").append(title));
            return;
        }
        if (willShow(e, player)) drawAbove(e, player, title);
    }

    /** Same decision vanilla makes about drawing the name tag. */
    private static boolean willShow(RenderNameTagEvent e, Entity entity) {
        if (e.canRender().isFalse()) return false;
        if (e.canRender().isTrue()) return true;
        if (!reflectionFailed) {
            try {
                if (shouldShowName == null) {
                    shouldShowName = EntityRenderer.class.getDeclaredMethod("shouldShowName", Entity.class);
                    shouldShowName.setAccessible(true);
                }
                return (boolean) shouldShowName.invoke(e.getEntityRenderer(), entity);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                reflectionFailed = true;
                TitleScrolls.LOGGER.warn("[TitleScrolls] Name tag visibility check unavailable, using a simple check: {}", ex.toString());
            }
        }
        Minecraft mc = Minecraft.getInstance();
        return Minecraft.renderNames() && mc.player != null && !entity.isInvisibleTo(mc.player) && !entity.isVehicle();
    }

    /** One line over the name tag, drawn the way vanilla draws the name. */
    private static void drawAbove(RenderNameTagEvent e, AbstractClientPlayer player, Component title) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        double distSq = dispatcher.distanceToSqr(player);
        if (!ClientHooks.isNameplateInRenderDistance(player, distSq)) return;
        Vec3 anchor = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getViewYRot(e.getPartialTick()));
        if (anchor == null) return;

        float scale = style.scale();
        double lift = 0.5 + LINE * scale;
        if (distSq < 100.0 && player.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
            lift += LINE; // vanilla puts the below-name score under the name and moves the name up
        }

        PoseStack pose = e.getPoseStack();
        pose.pushPose();
        pose.translate(anchor.x, anchor.y + lift, anchor.z);
        pose.mulPose(dispatcher.cameraOrientation());
        float s = 0.025F * scale;
        pose.scale(s, -s, s);
        Matrix4f matrix = pose.last().pose();
        boolean seeThrough = !player.isDiscrete();
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        Font font = e.getEntityRenderer().getFont();
        float x = -font.width(title) / 2.0F;
        font.drawInBatch(title, x, 0, 553648127, false, matrix, e.getMultiBufferSource(),
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, e.getPackedLight());
        if (seeThrough) {
            font.drawInBatch(title, x, 0, -1, false, matrix, e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());
        }
        pose.popPose();
    }
}
