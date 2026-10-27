package ink.astrius.create_avionics.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ink.astrius.create_avionics.CreateAvionics;
import ink.astrius.create_avionics.content.instrument.Instrument;
import ink.astrius.create_avionics.content.instrument.InstrumentBlock;
import ink.astrius.create_avionics.content.instrument.InstrumentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

/**
 * Draws an instrument's moving part on top of its static block model: needles and the almanac hand
 * rotate about their pivot, the thermometer's liquid column stretches up from the bulb. Pivots are
 * in model pixels on the north-facing model; ranges are documented on {@link Instrument}.
 */
public class InstrumentRenderer implements BlockEntityRenderer<InstrumentBlockEntity> {

    public static final ModelResourceLocation THERMOMETER_COLUMN = partial("thermometer/column");
    public static final ModelResourceLocation HYGROMETER_NEEDLE = partial("hygrometer/needle");
    public static final ModelResourceLocation BAROMETER_NEEDLE = partial("barometer/needle");
    public static final ModelResourceLocation ALMANAC_HAND = partial("almanac/hand");
    public static final List<ModelResourceLocation> PARTIALS =
            List.of(THERMOMETER_COLUMN, HYGROMETER_NEEDLE, BAROMETER_NEEDLE, ALMANAC_HAND);

    private static ModelResourceLocation partial(final String path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(CreateAvionics.MOD_ID, "block/" + path));
    }

    @Override
    public void render(final InstrumentBlockEntity be, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Instrument instrument = be.instrument();
        if (instrument == Instrument.RAIN_GAUGE) {
            return;
        }
        final float dial = be.dial(partialTick);
        final Direction facing = be.getBlockState().getValue(InstrumentBlock.FACING);

        pose.pushPose();
        // Turn the north-facing model to the block's facing, like the blockstate's Y rotation
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180));
        pose.translate(-0.5, -0.5, -0.5);

        final ModelResourceLocation partial = switch (instrument) {
            case THERMOMETER -> {
                scaleY(pose, 5, Math.max(dial, 0.001f));
                yield THERMOMETER_COLUMN;
            }
            case HYGROMETER -> {
                rotateZ(pose, 8, 4, -90 + 180 * dial);
                yield HYGROMETER_NEEDLE;
            }
            case BAROMETER -> {
                rotateZ(pose, 8, 8, -135 + 270 * dial);
                yield BAROMETER_NEEDLE;
            }
            default -> {
                rotateZ(pose, 8, 8, 360 * dial);
                yield ALMANAC_HAND;
            }
        };

        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(partial);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffers.getBuffer(RenderType.cutout()), be.getBlockState(), model,
                1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    /** Clockwise as seen from the front, about the Z axis through (px, py) in model pixels. */
    private static void rotateZ(final PoseStack pose, final float px, final float py, final float degrees) {
        pose.translate(px / 16f, py / 16f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        pose.translate(-px / 16f, -py / 16f, 0);
    }

    private static void scaleY(final PoseStack pose, final float fromY, final float scale) {
        pose.translate(0, fromY / 16f, 0);
        pose.scale(1, scale, 1);
        pose.translate(0, -fromY / 16f, 0);
    }
}
