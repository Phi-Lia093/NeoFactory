package com.philia093.neofactory.gui.container;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * What happens behind a container screen: the slots, the stack the mouse carries and
 * the clicks that move items around.
 * <p>
 * The class is the whole logic of a container and knows nothing about drawing, which
 * keeps it testable without a window and lets every future machine screen reuse it,
 * see {@link ContainerView} for the other half.
 * <p>
 * Clicking works the way the original game does: the left button moves a whole stack
 * - taking it, dropping it, topping a stack of the same item up or swapping two
 * different ones - and the right button moves half a stack or a single item. Holding
 * shift moves the stack to the other side of the container, which is the player
 * inventory for a machine and the crafting area for the player.
 * <p>
 * Coordinates are pixels of the panel, measured from its upper left corner with the
 * Y axis pointing down, see {@link ContainerLayout}.
 */
public final class ContainerMenu {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Told about every change, used to recompute what a recipe makes. */
    public interface ChangeListener {

        /**
         * Called after a click changed the contents of the container.
         *
         * @param menu container that changed
         */
        void onChanged(ContainerMenu menu);
    }

    /**
     * Supplies the next stack of a result slot.
     * <p>
     * A result slot holds what a recipe made, so it can neither be filled by hand nor
     * refilled by the container alone: the crafting field has to give up its
     * ingredients first. The container asks this filler every time a result was taken
     * out, see {@link #setResultFiller(ResultFiller)}.
     */
    public interface ResultFiller {

        /**
         * Makes the next stack for a result slot that was just emptied.
         *
         * @param slot the result slot that is empty again
         * @return the next result, {@link ItemStack#EMPTY} when nothing can be made
         */
        ItemStack nextResult(Slot slot);
    }

    /**
     * Hands a stack to the world.
     * <p>
     * The container itself knows nothing about a world, so a screen hands it this sink.
     * Without one the items of a work field go back to the player instead of the ground.
     */
    public interface StackDropper {

        /**
         * Drops a stack into the world.
         *
         * @param stack items to drop, never empty
         */
        void drop(ItemStack stack);
    }

    private final ContainerLayout layout;

    /** Inventory the other side of the container belongs to, see {@link #touchDown}. */
    private final Inventory playerSide;

    private ItemStack cursor = ItemStack.EMPTY;
    private boolean open;
    private ChangeListener listener;
    private ResultFiller resultFiller;
    private StackDropper dropper;

    /** {@code true} when items that find no place are destroyed, see {@link #setVoidsOverflow}. */
    private boolean voidsOverflow;

    /** Slot the mouse was pressed on, {@code null} while no button is held. */
    private Slot dragStart;

    /** Button the drag uses, which decides how the rest is shared out. */
    private int dragButton = Input.Buttons.LEFT;

    /** Slots a drag handed items to, in the order the mouse reached them. */
    private final List<Slot> dragSlots = new ArrayList<>();

    /** Amount of the carried stack each slot of the drag holds. */
    private final List<Integer> dragAmounts = new ArrayList<>();

    /** Items the drag shares out: what the mouse carried when the drag began. */
    private int dragTotal;

    /** {@code true} once the mouse reached another slot while the button was held. */
    private boolean dragMoved;

    /**
     * Creates a container.
     *
     * @param layout slots of the container and the size of its panel
     * @param playerSide inventory a stack is moved into when shift is held, normally
     *                  the inventory of the player
     */
    public ContainerMenu(ContainerLayout layout, Inventory playerSide) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.playerSide = Objects.requireNonNull(playerSide, "playerSide");
    }

    /** Slots of this container and the size of its panel. */
    public ContainerLayout layout() {
        return layout;
    }

    /** Inventory the other side of the container belongs to. */
    public Inventory playerSide() {
        return playerSide;
    }

    /** {@code true} while the container is shown. */
    public boolean isOpen() {
        return open;
    }

    /** Opens the container. */
    public void open() {
        open = true;
    }

    /** Opens the container when it is closed and closes it otherwise. */
    public void toggle() {
        if (open) {
            close();
        } else {
            open = true;
        }
    }

    /**
     * Closes the container.
     * <p>
     * A stack the mouse carries is put back into the inventory of the player, so a
     * player never loses items by closing a screen. Should that inventory be full, the
     * container stays open and the stack stays on the mouse until there is room.
     */
    public void close() {
        if (!open) {
            return;
        }
        carryBack();
        if (!cursor.isEmpty()) {
            LOGGER.warn("The inventory is full, {} x {} stay on the mouse",
                    cursor.count(), cursor.item().name());
            return;
        }
        open = false;
        dropWorkFields();
        changed();
    }

    /**
     * Hands whatever the work fields hold to the world.
     * <p>
     * A crafting field is no storage: leaving its items in a screen the player cannot see
     * would hide them, so they are dropped where the player stands, see
     * {@link Slot.Rule#WORK}.
     */
    private void dropWorkFields() {
        for (Slot slot : layout.slots()) {
            if (slot.rule() != Slot.Rule.WORK) {
                continue;
            }
            ItemStack stack = slot.remove();
            if (stack.isEmpty()) {
                continue;
            }
            if (dropper == null) {
                // Without a world to drop into the items go back to the player.
                playerSide.add(stack);
            } else {
                dropper.drop(stack);
            }
        }
    }

    /**
     * Drops the stack of one slot into the world, the action of the drop key.
     *
     * @param slot slot to empty, may be {@code null}
     * @param wholeStack {@code true} to drop the whole stack, {@code false} for one item
     * @return {@code true} when something was dropped
     */
    public boolean dropFrom(Slot slot, boolean wholeStack) {
        if (slot == null || slot.isOutput()) {
            // A result belongs to its recipe, it is not thrown away.
            return false;
        }
        ItemStack stored = slot.stack();
        if (stored.isEmpty()) {
            return false;
        }
        ItemStack dropped;
        if (wholeStack) {
            dropped = slot.remove();
        } else {
            dropped = ItemStack.of(stored.item(), 1);
            stored.setCount(stored.count() - 1);
            if (stored.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
        }
        if (dropper == null) {
            // Without a world the item goes back to the player instead of nowhere.
            playerSide.add(dropped);
        } else {
            dropper.drop(dropped);
        }
        changed();
        return true;
    }

    /**
     * Throws the stack the mouse carries into the world.
     * <p>
     * This is the action of a click beside the panel: a player gets rid of a stack without
     * closing the screen first. Without a world to drop into, the items go back to the
     * inventory of the player, so nothing is ever lost.
     *
     * @return {@code true} when a stack was thrown away
     */
    public boolean dropCursor() {
        if (cursor.isEmpty()) {
            return false;
        }
        ItemStack dropped = cursor;
        cursor = ItemStack.EMPTY;
        if (dropper == null) {
            int leftover = playerSide.add(dropped);
            cursor = leftover > 0 ? ItemStack.of(dropped.item(), leftover) : ItemStack.EMPTY;
        } else {
            dropper.drop(dropped);
        }
        changed();
        return true;
    }

    /**
     * Ends a drag and throws what is left on the mouse into the world.
     * <p>
     * This is what a button that is released beside the panel does: the slots the mouse
     * reached keep the items they already hold, and instead of being shared out over them
     * the rest is dropped, see {@link #dropCursor()}.
     *
     * @return {@code true} when something was dropped
     */
    public boolean dropDragRemainder() {
        dragStart = null;
        dragMoved = false;
        dragSlots.clear();
        dragAmounts.clear();
        return dropCursor();
    }

    /** Stack the mouse currently carries. */
    public ItemStack cursorStack() {
        return cursor;
    }

    /**
     * Sets the listener told about every change.
     *
     * @param listener listener to use, {@code null} to stop listening
     */
    public void setChangeListener(ChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the filler of the result slots.
     * <p>
     * A container without a recipe leaves this unset, and a result slot then simply
     * stays empty after it was emptied.
     *
     * @param resultFiller filler to use, {@code null} to stop filling
     */
    public void setResultFiller(ResultFiller resultFiller) {
        this.resultFiller = resultFiller;
    }

    /**
     * Sets the sink a closing container drops its work fields into.
     *
     * @param dropper sink to use, {@code null} to hand the items back to the player
     */
    public void setDropper(StackDropper dropper) {
        this.dropper = dropper;
    }

    /**
     * Sets whether items that find no place are destroyed instead of staying where they are.
     * <p>
     * Every ordinary container never loses an item: what does not fit stays in the slot it
     * came from, see {@link #quickMove(Slot)}. The creative inventory is the other way
     * round - an emptied grid slot fills itself again, see
     * {@link #setResultFiller(ResultFiller)} - so putting something into that grid can only
     * mean the player wants to get rid of it.
     *
     * @param voidsOverflow {@code true} to destroy what cannot be placed
     */
    public void setVoidsOverflow(boolean voidsOverflow) {
        this.voidsOverflow = voidsOverflow;
    }

    /**
     * Slot under a point of the panel.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return the slot, or {@code null} when the point is beside every slot
     */
    public Slot slotAt(int localX, int localY) {
        return layout.slotAt(localX, localY);
    }

    /**
     * Handles a mouse button press on the panel.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @param button mouse button that was pressed, see {@link Input.Buttons}
     * @param shift {@code true} while shift is held, which moves the whole stack to
     *              the other side of the container
     * @return {@code true} always, a container swallows every click while it is open
     */
    public boolean touchDown(int localX, int localY, int button, boolean shift) {
        Slot slot = layout.slotAt(localX, localY);
        if (slot == null) {
            // A click beside every slot puts the carried stack back.
            carryBack();
            changed();
            return true;
        }
        if (shift) {
            quickMove(slot);
            changed();
            return true;
        }
        // Nothing happens yet: a button that is released again on the same slot moves a
        // whole stack, a button that is dragged over other slots shares the stack out,
        // see {@link #touchDragged(int, int)}.
        dragStart = slot;
        dragButton = button;
        dragMoved = false;
        dragSlots.clear();
        dragAmounts.clear();
        return true;
    }

    /**
     * Handles the mouse moving while a button is held.
     * <p>
     * The stack the mouse carried when the drag began is shared out again every time the
     * mouse reaches a slot, so the amounts on screen stay even while the drag goes on:
     * dragging four items across two slots shows two each, across three slots one, one
     * and two, and so on. What does not fit into a full slot stays on the mouse.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     */
    public void touchDragged(int localX, int localY) {
        if (dragStart == null) {
            return;
        }
        if (cursor.isEmpty() && !dragMoved) {
            // Nothing to share out: the press happened on an empty mouse.
            return;
        }
        Slot slot = layout.slotAt(localX, localY);
        if (slot == null || slot == dragStart || slot.isOutput() || dragSlots.contains(slot)) {
            return;
        }
        if (!dragMoved) {
            // The slot the press started on takes part in the drag as well, so a stack that is
            // dragged across four slots is shared out over all four of them. The amount that
            // is shared out is the one the mouse carried when the drag began.
            dragMoved = true;
            dragTotal = cursor.count();
            dragSlots.add(dragStart);
            dragAmounts.add(0);
        }
        dragSlots.add(slot);
        dragAmounts.add(0);
        redistribute();
        changed();
    }

    /**
     * Shares the items of the drag out over the slots it reached.
     * <p>
     * Every slot gets its even share, rounded up for the first ones. A slot that cannot
     * take its share - because it is full - keeps what it has, and the rest stays on the
     * mouse, see {@link #shareOut()}.
     */
    private void redistribute() {
        int slots = dragSlots.size();
        if (slots == 0) {
            return;
        }
        int perSlot = dragTotal / slots;
        int extra = dragTotal % slots;
        for (int index = 0; index < slots; index++) {
            adjust(index, perSlot + (index < extra ? 1 : 0));
        }
    }

    /**
     * Makes one slot of a drag hold the wanted amount.
     *
     * @param index index of the slot inside the drag
     * @param wanted amount the slot should hold
     */
    private void adjust(int index, int wanted) {
        Slot slot = dragSlots.get(index);
        int held = dragAmounts.get(index);
        int difference = wanted - held;
        if (difference > 0) {
            dragAmounts.set(index, held + put(slot, difference));
        } else if (difference < 0) {
            dragAmounts.set(index, held - takeBack(slot, -difference));
        }
    }

    /**
     * Puts up to a given amount of the carried stack into a slot.
     *
     * @param slot slot that receives items
     * @param amount largest amount to place
     * @return the amount that fit, {@code 0} when the slot cannot take any
     */
    private int put(Slot slot, int amount) {
        if (cursor.isEmpty() || amount <= 0 || slot.isOutput()) {
            return 0;
        }
        ItemStack stored = slot.stack();
        if (stored.isEmpty()) {
            int placing = Math.min(amount, cursor.count());
            slot.set(cursor.split(placing));
            if (cursor.isEmpty()) {
                cursor = ItemStack.EMPTY;
            }
            return placing;
        }
        if (!stored.isStackableWith(cursor)) {
            return 0;
        }
        int placing = Math.min(amount, Math.min(stored.room(), cursor.count()));
        stored.grow(placing);
        cursor.setCount(cursor.count() - placing);
        if (cursor.isEmpty()) {
            cursor = ItemStack.EMPTY;
        }
        return placing;
    }

    /**
     * Takes up to a given amount back out of a slot onto the mouse.
     *
     * @param slot slot the items come from
     * @param amount largest amount to take back
     * @return the amount that came back
     */
    private int takeBack(Slot slot, int amount) {
        ItemStack stored = slot.stack();
        if (stored.isEmpty() || amount <= 0) {
            return 0;
        }
        int taking = Math.min(amount, stored.count());
        if (cursor.isEmpty()) {
            cursor = stored.split(taking);
        } else {
            stored.setCount(stored.count() - taking);
            cursor.grow(taking);
        }
        return taking;
    }

    /**
     * Hands out what is left of the carried stack after a drag.
     * <p>
     * The even share of {@link #redistribute()} leaves a remainder on the mouse whenever
     * a slot could not take its part. This walks the slots of the drag until nothing
     * fits any more, so a stack ends up as even as the slots allow.
     */
    private void shareOut() {
        boolean handedOut;
        do {
            handedOut = false;
            for (int index = 0; index < dragSlots.size() && !cursor.isEmpty(); index++) {
                int placed = put(dragSlots.get(index), 1);
                if (placed > 0) {
                    dragAmounts.set(index, dragAmounts.get(index) + placed);
                    handedOut = true;
                }
            }
        } while (handedOut && !cursor.isEmpty());
    }

    /**
     * Handles the release of a button.
     * <p>
     * A button that never left its slot behaves like a plain click: the left one moves a
     * whole stack, the right one takes half a stack or drops a single item. A drag shares
     * out what is left on the mouse, see {@link #touchDragged(int, int)}.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @param button mouse button that was released
     * @param shift {@code true} while shift is held
     * @return {@code true} when the release belonged to this container
     */
    public boolean touchUp(int localX, int localY, int button, boolean shift) {
        if (dragStart == null) {
            return false;
        }
        dragStart = null;
        if (dragMoved) {
            if (dragButton != Input.Buttons.RIGHT) {
                // The left button shares out what is left, the right one drops a single
                // item per slot and leaves the rest on the mouse.
                shareOut();
            }
            dragSlots.clear();
            dragAmounts.clear();
            dragMoved = false;
            changed();
            return true;
        }
        dragSlots.clear();
        dragAmounts.clear();
        Slot slot = layout.slotAt(localX, localY);
        if (slot == null) {
            carryBack();
        } else if (button == Input.Buttons.RIGHT) {
            moveOneItem(slot);
        } else {
            moveWholeStack(slot);
        }
        changed();
        return true;
    }

    /**
     * Puts the carried stack back into the inventory of the player.
     * <p>
     * Whatever does not fit stays on the mouse, so nothing is ever dropped into a
     * world that has no place for it yet.
     */
    public void carryBack() {
        if (cursor.isEmpty()) {
            return;
        }
        int leftover = playerSide.add(cursor);
        cursor = leftover > 0 ? ItemStack.of(cursor.item(), leftover) : ItemStack.EMPTY;
    }

    /**
     * Moves a whole stack, which is what the left button does.
     * <p>
     * An empty mouse takes the stack out of the slot, a full mouse drops it into an
     * empty slot, tops up a stack of the same item or swaps two different items. A
     * result slot only hands out: what the container made there is taken, never
     * filled by hand.
     *
     * @param slot slot that was clicked
     */
    private void moveWholeStack(Slot slot) {
        ItemStack stored = slot.stack();
        if (cursor.isEmpty()) {
            cursor = slot.remove();
            // A result slot fills itself again while the ingredients are still there.
            if (slot.isOutput()) {
                refillResult(slot);
            }
            return;
        }
        if (slot.isOutput()) {
            if (voidsOverflow) {
                // Dropping a stack on a slot that only hands out means throwing it away
                // in a screen that owns every item of the game anyway.
                cursor = ItemStack.EMPTY;
            }
            return;
        }
        if (stored.isEmpty()) {
            slot.set(cursor);
            cursor = ItemStack.EMPTY;
            return;
        }
        if (stored.isStackableWith(cursor)) {
            int leftover = stored.grow(cursor.count());
            cursor = ItemStack.of(cursor.item(), leftover);
            return;
        }
        // Two different items change places.
        slot.set(cursor);
        cursor = stored;
    }

    /**
     * Moves half a stack or a single item, which is what the right button does.
     *
     * @param slot slot that was clicked
     */
    private void moveOneItem(Slot slot) {
        if (slot.isOutput()) {
            if (voidsOverflow && !cursor.isEmpty()) {
                // The right button throws one item away on a slot that only hands out,
                // see setVoidsOverflow.
                cursor.setCount(cursor.count() - 1);
                return;
            }
            // A result is always taken as a whole, splitting it makes no sense.
            moveWholeStack(slot);
            return;
        }
        ItemStack stored = slot.stack();
        if (cursor.isEmpty()) {
            if (stored.isEmpty()) {
                return;
            }
            // The right button takes half of the stack, rounded up.
            cursor = stored.split((stored.count() + 1) / 2);
            return;
        }
        if (stored.isEmpty()) {
            ItemStack single = cursor.split(1);
            if (cursor.isEmpty()) {
                cursor = ItemStack.EMPTY;
            }
            slot.set(single);
            return;
        }
        if (stored.isStackableWith(cursor) && !stored.isFull() && stored.grow(1) == 0) {
            cursor.setCount(cursor.count() - 1);
        }
    }

    /**
     * Moves a stack to the other side of the container, which is what a click with
     * shift held does. On a result slot this is a whole crafting session, see
     * {@link #quickMoveResult(Slot)}.
     *
     * @param slot slot that was clicked
     */
    private void quickMove(Slot slot) {
        if (slot.isOutput()) {
            // The result goes to the player and the container makes the next one while
            // the ingredients are still there.
            quickMoveResult(slot);
            return;
        }

        ItemStack moving = slot.remove();
        if (moving.isEmpty()) {
            return;
        }
        boolean toPlayer = slot.inventory() != playerSide;
        int leftover = moveInto(moving, candidate -> toPlayer
                ? candidate.inventory() == playerSide
                : candidate.inventory() != playerSide && !candidate.isOutput());
        if (leftover > 0) {
            if (voidsOverflow) {
                // The other side owns every item anyway, see setVoidsOverflow.
                return;
            }
            // No room on the other side, the stack stays where it came from.
            slot.set(ItemStack.of(moving.item(), leftover));
        }
    }

    /**
     * Hands out as many results of a result slot as the recipe and the room allow.
     * <p>
     * A click with shift held on a result is a whole crafting session: the container makes
     * one result after the other and moves every one of them to the player, which goes on
     * until the ingredients are used up or the inventory has no room left. One result is
     * taken at a time, so the field only gives up its ingredients for a result the player
     * really receives - a full inventory leaves them where they are instead of eating them
     * for nothing. What does not fit of a single result waits in the result slot, because
     * the field has not been touched for it yet.
     *
     * @param slot result slot that was clicked
     */
    private void quickMoveResult(Slot slot) {
        if (slot.stack().isEmpty()) {
            return;
        }
        // A filler that never runs out of ingredients would loop forever, and a player can
        // never receive more than the inventory holds.
        int guard = playerSide.size() * Math.max(1, slot.stack().item().maxStackSize());
        for (int taken = 0; taken < guard && !slot.stack().isEmpty(); taken++) {
            ItemStack made = slot.stack();
            int leftover = playerSide.add(made);
            if (leftover >= made.count()) {
                // No room at all: the ingredients are left alone.
                return;
            }
            if (leftover > 0) {
                // Only part of it fit, the rest waits in the result slot.
                made.setCount(leftover);
                return;
            }
            slot.remove();
            refillResult(slot);
        }
    }

    /**
     * Distributes a stack over the slots that accept it.
     * <p>
     * The slots on the other side are filled the way an inventory does it: stacks of
     * the same item are topped up first, so a container does not waste a slot on a
     * single item while a half filled stack of it exists.
     *
     * @param moving stack to place, its amount is left untouched
     * @param accept decides which slots may hold the stack
     * @return the amount that did not fit, {@code 0} when everything was placed
     */
    private int moveInto(ItemStack moving, Predicate<Slot> accept) {
        int remaining = moving.count();
        if (moving.item().isStackable()) {
            for (Slot candidate : layout.slots()) {
                if (remaining == 0) {
                    break;
                }
                if (accept.test(candidate) && candidate.stack().isStackableWith(moving)) {
                    remaining = candidate.stack().grow(remaining);
                }
            }
        }
        for (Slot candidate : layout.slots()) {
            if (remaining == 0) {
                break;
            }
            if (!accept.test(candidate) || !candidate.stack().isEmpty()) {
                continue;
            }
            int fitting = Math.min(remaining, moving.item().maxStackSize());
            candidate.set(ItemStack.of(moving.item(), fitting));
            remaining -= fitting;
        }
        return remaining;
    }

    /** Tells the listener that the contents changed. */
    private void changed() {
        if (listener != null) {
            listener.onChanged(this);
        }
    }

    /** Asks the filler for the next result of a result slot that was emptied. */
    private void refillResult(Slot slot) {
        if (resultFiller != null) {
            slot.set(resultFiller.nextResult(slot));
        }
    }

    @Override
    public String toString() {
        return "ContainerMenu(" + layout.size() + " slots, cursor " + cursor + ")";
    }
}
