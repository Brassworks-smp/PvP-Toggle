package org.opnsoc.dererneuerer.pvptoggle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import org.opnsoc.dererneuerer.pvptoggle.Pvptoggle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PvpIconRenderer {
    private static final ResourceLocation PVP_ON =
            ResourceLocation.fromNamespaceAndPath(
                    Pvptoggle.MODID,
                    "textures/gui/pvp_on.png"
            );

    private static final ResourceLocation PVP_OFF =
            ResourceLocation.fromNamespaceAndPath(
                    Pvptoggle.MODID,
                    "textures/gui/pvp_off.png"
            );

    private static final String SIMPLE_VOICE_CHAT_MOD_ID = "voicechat";

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer self = mc.player;

        if (self == null) return;
        if (self.getUUID().equals(target.getUUID())) return;
        if (mc.options.hideGui) return;

        boolean canAttack = ClientPvpIcons.canAttack(target.getUUID());
        ResourceLocation icon = canAttack ? PVP_ON : PVP_OFF;

        PoseStack pose = event.getPoseStack();

        pose.pushPose();
        pose.translate(0.0D, target.getBbHeight() + 0.5D, 0.0D);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025F, -0.025F, 0.025F);

        VertexConsumer consumer = event.getMultiBufferSource()
                .getBuffer(RenderType.text(icon));

        PoseStack.Pose lastPose = pose.last();

        Component nameTag = event.getContent();

        float size = 10F;
        float x = mc.font.width(nameTag) / 2F + 2F;
        float y = -1F;

        if (isSimpleVoiceChatNametagIconVisible()) {
            x += 10F;
        }

        int light = LightTexture.FULL_BRIGHT;

        vertex(consumer, lastPose, x, y + size, 0F, 0F, 1F, light);
        vertex(consumer, lastPose, x + size, y + size, 0F, 1F, 1F, light);
        vertex(consumer, lastPose, x + size, y, 0F, 1F, 0F, light);
        vertex(consumer, lastPose, x, y, 0F, 0F, 0F, light);

        pose.popPose();
    }

    private static boolean isSimpleVoiceChatNametagIconVisible() {
        if (!ModList.get().isLoaded(SIMPLE_VOICE_CHAT_MOD_ID)) {
            return false;
        }

        try {
            Class<?> voicechatClientClass = Class.forName("de.maxhenkel.voicechat.VoicechatClient");

            Field clientConfigField = voicechatClientClass.getField("CLIENT_CONFIG");
            Object clientConfig = clientConfigField.get(null);

            boolean hideIcons = getBooleanConfigValue(clientConfig, "hideIcons", false);
            boolean showNametagIcons = getBooleanConfigValue(clientConfig, "showNametagIcons", true);

            return !hideIcons && showNametagIcons;
        } catch (ReflectiveOperationException exception) {
            return true;
        }
    }

    private static boolean getBooleanConfigValue(Object config, String fieldName, boolean fallback) throws ReflectiveOperationException {
        Field field = config.getClass().getField(fieldName);
        Object configValue = field.get(config);

        Method getMethod = configValue.getClass().getMethod("get");
        Object value = getMethod.invoke(configValue);

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        return fallback;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(pose.pose(), x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0F, 0F, -1F);
    }
}