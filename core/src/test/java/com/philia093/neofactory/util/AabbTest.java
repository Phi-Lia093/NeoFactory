package com.philia093.neofactory.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the box a world of cubes is measured against.
 * <p>
 * The one rule that carries the whole collision code is the half open one: two boxes that share a
 * face do not overlap. A body that rests on the ground touches the block below it, and if touching
 * counted as overlapping, every player would be pushed out of the floor they stand on. The flat
 * engine needed a small fudge factor to stand in for that rule; this box does not, and the same rule
 * is what lets a body rest on the top of the half a slab fills, see {@code Player#collides}.
 */
class AabbTest {

    @Test
    void theBoxOfABlockCoversExactlyItsCell() {
        Aabb box = Aabb.block(3, -2, 5);

        assertEquals(3.0f, box.minX());
        assertEquals(-2.0f, box.minY());
        assertEquals(5.0f, box.minZ());
        assertEquals(4.0f, box.maxX());
        assertEquals(-1.0f, box.maxY());
        assertEquals(6.0f, box.maxZ());
        assertEquals(Constants.BLOCK_SIZE, box.sizeX(), 1.0e-6f);
        assertEquals(Constants.BLOCK_SIZE, box.sizeY(), 1.0e-6f);
        assertEquals(Constants.BLOCK_SIZE, box.sizeZ(), 1.0e-6f);
        assertEquals(3.5f, box.centerX(), 1.0e-6f);
        assertEquals(-1.5f, box.centerY(), 1.0e-6f);
        assertEquals(5.5f, box.centerZ(), 1.0e-6f);
        assertFalse(box.isEmpty());
    }

    @Test
    void aBoxThatCoversNothingIsEmpty() {
        assertTrue(new Aabb().isEmpty());
        assertTrue(Aabb.of(1.0f, 1.0f, 1.0f, 1.0f, 2.0f, 2.0f).isEmpty());
        assertTrue(Aabb.of(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f).isEmpty());
    }

    @Test
    void anEmptiedBoxCoversNothingAndSharesNothing() {
        Aabb box = Aabb.block(2, 3, 4);

        box.clear();

        assertTrue(box.isEmpty(), "a box that was emptied covers nothing");
        assertFalse(box.intersects(Aabb.block(0, 0, 0)), "and it shares space with nothing");
        assertFalse(Aabb.block(0, 0, 0).intersects(box));
        assertFalse(Aabb.block(-1, -1, -1).intersects(box), "not even at the origin of the world");
    }

    @Test
    void twoBoxesThatOnlyTouchDoNotOverlap() {
        Aabb left = Aabb.block(0, 0, 0);

        assertFalse(left.intersects(Aabb.block(1, 0, 0)), "two cells side by side");
        assertFalse(Aabb.block(1, 0, 0).intersects(left));
        assertFalse(left.intersects(Aabb.block(0, 1, 0)), "a body standing on a block");
        assertFalse(Aabb.block(0, 1, 0).intersects(left));
        assertFalse(left.intersects(Aabb.block(-1, 0, 0)), "the cell behind it");
        assertFalse(left.intersects(Aabb.block(0, 0, -1)));
    }

    @Test
    void twoBoxesThatShareSpaceOverlap() {
        Aabb acrossTheCorner = Aabb.of(0.5f, 0.5f, 0.5f, 1.5f, 1.5f, 1.5f);

        assertTrue(Aabb.block(0, 0, 0).intersects(acrossTheCorner));
        assertTrue(acrossTheCorner.intersects(Aabb.block(0, 0, 0)));
        assertTrue(Aabb.block(1, 1, 1).intersects(acrossTheCorner));
        assertTrue(Aabb.block(0, 0, 0).intersects(Aabb.block(0, 0, 0)), "a box overlaps itself");
    }

    @Test
    void aPointOnTheNearSideIsInsideAndOneOnTheFarSideIsNot() {
        Aabb box = Aabb.unit();

        assertTrue(box.contains(0.0f, 0.0f, 0.0f));
        assertTrue(box.contains(0.999f, 0.5f, 0.5f));
        assertFalse(box.contains(1.0f, 0.5f, 0.5f), "the far side");
        assertFalse(box.contains(-0.001f, 0.5f, 0.5f));
        assertFalse(box.contains(0.5f, 1.5f, 0.5f));
        assertFalse(box.contains(0.5f, 0.5f, -1.0f));
    }

    @Test
    void movingGrowingAndShrinkingKeepTheBoxUnderControl() {
        Aabb box = Aabb.unit().offset(10.0f, -1.0f, 0.5f);

        assertEquals(10.0f, box.minX());
        assertEquals(-1.0f, box.minY());
        assertEquals(0.5f, box.minZ());
        assertEquals(11.0f, box.maxX());
        assertEquals(1.5f, box.maxZ());

        box.grow(0.25f);
        assertEquals(1.5f, box.sizeX(), 1.0e-6f);
        assertEquals(1.5f, box.sizeY(), 1.0e-6f);
        assertEquals(1.5f, box.sizeZ(), 1.0e-6f);

        assertTrue(Aabb.unit().grow(-0.6f).isEmpty(), "shrinking past the middle turns it out");
    }

    @Test
    void aBoxWrittenTheWrongWayRoundIsTurnedAround() {
        Aabb box = Aabb.of(5.0f, 5.0f, 5.0f, 1.0f, 1.0f, 1.0f);

        assertEquals(1.0f, box.minX());
        assertEquals(1.0f, box.minY());
        assertEquals(1.0f, box.minZ());
        assertEquals(5.0f, box.maxX());
        assertEquals(4.0f, box.sizeX(), 1.0e-6f);
    }

    @Test
    void aBoxIsMovedAndCopiedWithoutAllocating() {
        Aabb box = new Aabb();

        assertSame(box, box.setBlock(7, 8, 9));
        assertEquals(7.0f, box.minX());
        assertEquals(8.0f, box.minY());
        assertEquals(9.0f, box.minZ());

        assertSame(box, box.set(Aabb.unit()));
        assertEquals(Aabb.unit(), box);

        Aabb copy = box.copy().offset(1.0f, 0.0f, 0.0f);
        assertNotEquals(box, copy, "the copy moved, the box did not");
        assertEquals(0.0f, box.minX(), 1.0e-6f);
        assertEquals(1.0f, copy.minX(), 1.0e-6f);
        assertEquals(box, box.copy());
        assertEquals(box.hashCode(), box.copy().hashCode());
    }

    @Test
    void aBoxDescribesItself() {
        String text = Aabb.block(1, 2, 3).toString();

        assertTrue(text.contains("(1.0, 2.0, 3.0)"), text);
        assertTrue(text.contains("(2.0, 3.0, 4.0)"), text);
    }
}
