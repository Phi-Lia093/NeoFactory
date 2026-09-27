package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.ContainerView;
import com.philia093.neofactory.gui.panel.ArrowElement;
import com.philia093.neofactory.recipe.CraftingField;

import java.util.Objects;

/**
 * What the screen of a table adds to the slots of its container.
 * <p>
 * The field of nine cells and its result are plain slots of a container, so the screen of a container
 * draws them without knowing what they are, see {@link ContainerGui.Part}. What a table needs on top of
 * that is the arithmetic: which recipe the cells hold and what taking its product gives up.
 * <p>
 * <b>The arrow stays empty.</b> A table makes its product the moment a player takes it, so there is no
 * work to show: the arrow is the very picture the field of the player uses, drawn between the field and
 * the result of a table, see {@link com.philia093.neofactory.gui.InventoryGui#drawCraftingArrow}.
 */
public final class CraftingPart implements ContainerGui.Part {

    /** Work field of the table that is shown. */
    private final CraftingField field;

    /** View of the container, handed over while the part is bound. */
    private ContainerView view;

    /**
     * Creates the part of a table.
     *
     * @param field work field of the block that was opened, never {@code null}
     */
    public CraftingPart(CraftingField field) {
        this.field = Objects.requireNonNull(field, "field");
    }

    /** Work field this part shows. */
    public CraftingField field() {
        return field;
    }

    @Override
    public void bind(ContainerMenu menu, ContainerView view) {
        this.view = view;
        // Every click that changed the field asks the recipes again, and taking the result gives up the
        // ingredients it was made of, exactly like the two by two field of the player.
        menu.setChangeListener(container -> field.refresh());
        menu.setResultFiller(slot -> field.take());
    }

    @Override
    public void draw(SpriteBatch batch, float panelX, float panelY, int panelWidth, int panelHeight) {
        if (view == null) {
            return;
        }
        ArrowElement arrow = view.arrows();
        arrow.draw(batch, panelX + CraftingLayout.ARROW_X,
                panelY + panelHeight - CraftingLayout.ARROW_Y - arrow.height(), 0.0f);
    }
}
