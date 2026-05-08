package com.happysg.radar.item.binos;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.item.binos.Binoculars;
import com.happysg.radar.registry.ModGuiTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpyglassItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT, modid = CreateRadar.MODID)
public class BinocularOverlay {

    private static final ResourceLocation OVERLAY = ModGuiTextures.BINOCULAR_OVERLAY.location;

    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        if (!player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof Binoculars)) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();


        final int TEX_W = 512;
        final int TEX_H = 256;


        float baseScale = (float) screenH / (float) TEX_H;
        float scale = baseScale * 0.87f;

        int drawW = (int) (TEX_W * scale);
        int drawH = (int) (TEX_H * scale);

        int x0 = (screenW - drawW) / 2;
        int y0 = (screenH - drawH) / 2;


        if (x0 > 0) {
            event.getGuiGraphics().fill(0, 0, x0, screenH, 0xFF000000);
            event.getGuiGraphics().fill(x0 + drawW, 0, screenW, screenH, 0xFF000000);
        }
        if (y0 > 0) {
            event.getGuiGraphics().fill(0, 0, screenW, y0, 0xFF000000);
            event.getGuiGraphics().fill(0, y0 + drawH, screenW, screenH, 0xFF000000);
        }

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();


        var pose = event.getGuiGraphics().pose();
        pose.pushPose();

        pose.translate(x0, y0, 0);
        pose.scale(scale, scale, 1f);

        event.getGuiGraphics().blit(
                OVERLAY,
                0, 0,
                0, 0,
                TEX_W, TEX_H,
                TEX_W, TEX_H
        );

        pose.popPose();
        RenderSystem.disableBlend();
    }





}
