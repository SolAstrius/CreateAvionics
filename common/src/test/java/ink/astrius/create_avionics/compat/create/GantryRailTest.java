package ink.astrius.create_avionics.compat.create;

import ink.astrius.create_avionics.compat.create.peripherals.GantryRail.Rail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Rail#contains} is what ties a moving contraption to <em>this</em>
 * rail rather than a neighbouring one. Getting it wrong is not a loud failure:
 * a stacked gantry (rail, carriage, second rail) has two rails a block apart
 * whose search volumes overlap, so a sloppy membership test silently reports
 * the other rail's carriage — which is the shape of the bug this class was
 * written to kill.
 */
class GantryRailTest {

    /** An 8-long rail running east from the origin, observed from its 3rd shaft. */
    private static Rail eastward() {
        return new Rail(Direction.EAST, new BlockPos(0, 64, 0), 8, 2);
    }

    /**
     * The mirror image of {@link #eastward()} — same layout, but facing a
     * negative axis direction. {@code contains} used to compute its bound
     * check from a raw coordinate delta with no regard for which way the
     * axis runs, so on a rail like this every real position except the start
     * shaft itself produced a negative "offset" and was rejected outright.
     */
    private static Rail westward() {
        return new Rail(Direction.WEST, new BlockPos(0, 64, 0), 8, 2);
    }

    @Test
    void everyShaftOfTheRailIsContained() {
        final Rail rail = eastward();
        for (int i = 0; i < rail.length(); i++) {
            final BlockPos shaft = new BlockPos(i, 64, 0);
            assertTrue(rail.contains(shaft), shaft + " is the rail's own shaft " + i);
        }
    }

    @Test
    void positionsPastEitherEndAreNotContained() {
        final Rail rail = eastward();
        assertFalse(rail.contains(new BlockPos(-1, 64, 0)), "one before the start");
        assertFalse(rail.contains(new BlockPos(8, 64, 0)), "one past the end");
    }

    @Test
    void aParallelRailOnTheSameAxisIsNotContained() {
        final Rail rail = eastward();
        // The stacked-gantry geometry: a second rail two blocks below, with the
        // shared carriage between them. Its coordinate along the rail axis is
        // identical, so only the off-axis check rejects it.
        assertFalse(rail.contains(new BlockPos(3, 62, 0)), "rail two below");
        assertFalse(rail.contains(new BlockPos(3, 66, 0)), "rail two above");
        assertFalse(rail.contains(new BlockPos(3, 64, 2)), "rail two to the side");
    }

    @Test
    void theCarriageSlotBesideAShaftIsNotItselfAShaft() {
        final Rail rail = eastward();
        assertFalse(rail.contains(new BlockPos(3, 63, 0)), "carriage hangs below, off the rail line");
    }

    @Test
    void endIsTheLastShaftNotOnePast() {
        final Rail rail = eastward();
        assertEquals(new BlockPos(7, 64, 0), rail.end());
        assertTrue(rail.contains(rail.end()));
    }

    @Test
    void railsAreMeasuredAlongTheirOwnAxis() {
        final Rail vertical = new Rail(Direction.UP, new BlockPos(10, 0, 10), 4, 0);
        assertEquals(Direction.Axis.Y, vertical.axis());
        assertTrue(vertical.contains(new BlockPos(10, 3, 10)));
        assertFalse(vertical.contains(new BlockPos(10, 4, 10)), "one past the end");
        assertFalse(vertical.contains(new BlockPos(11, 2, 10)), "parallel column beside it");
    }

    @Test
    void everyShaftOfAWestwardRailIsContained() {
        final Rail rail = westward();
        for (int i = 0; i < rail.length(); i++) {
            final BlockPos shaft = new BlockPos(-i, 64, 0);
            assertTrue(rail.contains(shaft), shaft + " is the rail's own shaft " + i);
        }
    }

    @Test
    void westwardPositionsPastEitherEndAreNotContained() {
        final Rail rail = westward();
        assertFalse(rail.contains(new BlockPos(1, 64, 0)), "one before the start");
        assertFalse(rail.contains(new BlockPos(-8, 64, 0)), "one past the end");
    }

    @Test
    void westwardEndIsTheLastShaftNotOnePast() {
        final Rail rail = westward();
        assertEquals(new BlockPos(-7, 64, 0), rail.end());
        assertTrue(rail.contains(rail.end()));
    }

    @Test
    void negativeAxisFacingOffsetsCountUpAwayFromStart() {
        final Rail rail = westward();
        assertEquals(0, rail.coord(new BlockPos(0, 64, 0)));
        assertEquals(3, rail.coord(new BlockPos(-3, 64, 0)));
        assertEquals(7, rail.coord(rail.end()));
    }

    @Test
    void downwardRailIsAlsoSignCorrected() {
        final Rail rail = new Rail(Direction.DOWN, new BlockPos(10, 64, 10), 4, 0);
        assertTrue(rail.contains(new BlockPos(10, 62, 10)));
        assertFalse(rail.contains(new BlockPos(10, 65, 10)), "one before the start");
        assertFalse(rail.contains(new BlockPos(10, 60, 10)), "one past the end");
    }
}
