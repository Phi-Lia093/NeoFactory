package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.world.GameMode;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Mining rule that answers differently in each game mode.
 * <p>
 * A creative player owns every item of the game already, so a block is broken the moment it is hit and
 * nothing is left behind: collecting what a block hands over is what survival is for, and a creative
 * player who takes from a block would be given what they already have. Everything else is asked of the
 * survival rule, so hardness, mining level and the speed of the tool stay in one place, see
 * {@link HardnessMining}.
 * <p>
 * <b>An unbreakable block stays unbreakable.</b> A block with a negative hardness - bedrock, a fluid - is
 * reported as unbreakable in both modes, which is decided here so that no mode can dig through it.
 * <p>
 * The mode is read while a break runs and not while this rule is built, so switching with
 * {@code /gamemode} takes effect on the next hit.
 */
public final class GameModeMining implements MiningRule {

    /** Value reported for a block that cannot be broken, the one every rule of the game uses. */
    private static final float UNBREAKABLE = -1.0f;

    /** Value reported for a block that breaks right away, which is every block in creative mode. */
    private static final float INSTANT = 0.0f;

    /** Mode the world is played in right now. */
    private final Supplier<GameMode> mode;

    /** Rule that answers for a world played in survival mode. */
    private final MiningRule survival;

    /**
     * Creates a rule that switches with the mode of a world.
     *
     * @param mode mode the world is played in, asked on every break
     * @param survival rule that decides a survival break
     */
    public GameModeMining(Supplier<GameMode> mode, MiningRule survival) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.survival = Objects.requireNonNull(survival, "survival");
    }

    @Override
    public float breakSeconds(Block block, ItemStack tool) {
        if (block.hardness() < 0.0f) {
            return UNBREAKABLE;
        }
        return isCreative() ? INSTANT : survival.breakSeconds(block, tool);
    }

    @Override
    public boolean canHarvest(Block block, ItemStack tool) {
        // Nothing is collected in creative mode, which also means that a tool the player holds neither
        // helps nor is needed: the block goes and nothing comes back, see the class comment.
        return !isCreative() && survival.canHarvest(block, tool);
    }

    /** {@code true} while the world is played in creative mode. */
    private boolean isCreative() {
        return mode.get() == GameMode.CREATIVE;
    }

    @Override
    public String toString() {
        return "GameModeMining(" + (isCreative() ? "creative" : "survival") + ")";
    }
}
