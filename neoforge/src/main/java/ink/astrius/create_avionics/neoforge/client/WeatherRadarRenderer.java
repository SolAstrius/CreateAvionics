package ink.astrius.create_avionics.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.content.radar.WeatherRadarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Turns the weather radar's dish: about the block's vertical centre line by the azimuth, and
 * tilted up about its trunnion at (8, 11, 8) model px by the elevation of the current turn. Both
 * follow from the game time, the same way the server scans.
 */
public class WeatherRadarRenderer implements BlockEntityRenderer<WeatherRadarBlockEntity> {

    public static final ModelResourceLocation DISH = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(CreateAvionics.MOD_ID, "block/weather_radar/dish"));
    private static final float PIVOT_Y = 11 / 16f;

    @Override
    public void render(final WeatherRadarBlockEntity be, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Level level = be.getLevel();
        if (level == null) {
            return;
        }
        final double time = level.getGameTime() + partialTick;
        final float azimuth = (float) (360 * be.turnFraction(time));
        final float elevation = (float) be.elevationAt(time);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        // The north-facing model to the block's facing, then clockwise from above by the azimuth
        pose.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot() + 180 - azimuth));
        pose.translate(0, PIVOT_Y, 0);
        pose.mulPose(Axis.XP.rotationDegrees(elevation));
        pose.translate(-0.5, -PIVOT_Y, -0.5);
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(DISH);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffers.getBuffer(RenderType.cutout()), be.getBlockState(), model,
                1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }
}
