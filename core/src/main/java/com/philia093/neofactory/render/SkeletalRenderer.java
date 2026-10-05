package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.chemistry.Atom;
import com.philia093.neofactory.chemistry.Bond;
import com.philia093.neofactory.chemistry.Molecule;
import com.philia093.neofactory.chemistry.SkeletalStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Paints a laid out molecule as a skeletal formula, the drawing an organic chemist reads at a glance.
 * <p>
 * The drawing is a skeleton and not a list of atoms: every vertex of a line is a carbon, a hydrogen is
 * understood and never written, and the only letters on the page are the atoms that are not carbon - an
 * oxygen, a nitrogen, a halogen - with the hydrogens they carry and the charge they bear. A bond is one
 * line, a double bond two and a triple bond three, and the ring of an aromatic molecule carries the circle
 * that says its bonds are shared and not two of them.
 * <p>
 * <b>The picture is plain pixels and nothing of a window.</b> What comes out is an array of
 * {@code RGBA8888} pixels, the very format a {@code Pixmap} holds, so the drawing of a molecule may be
 * checked by counting what was painted without opening a window or a graphics card, the way the window of
 * a fluid cell already is, see {@code CellIconFactory}. The names of the atoms are handed back beside the
 * pixels and not stamped into them, because a letter is drawn with the bitmap of the game and only a screen
 * knows which bitmap that is.
 */
public final class SkeletalRenderer {

    /** Space kept clear around the drawing, in pixels. */
    public static final int MARGIN = 4;

    /** Pixels a bond is pulled back from an atom that carries a name, so a letter is not overrun. */
    private static final int TRIM = 3;

    /** Share of a bond length the two lines of a double bond stand apart by. */
    private static final double OFFSET_SHARE = 0.12;

    /** Largest value a colour channel may hold. */
    private static final int CHANNEL_MAX = 0xFF;

    /**
     * One atom that is named on the drawing, and where its name stands.
     *
     * @param text the letters, such as {@code OH} or {@code NH2}
     * @param centerX pixel the name is centred on, across
     * @param centerY pixel the name is centred on, down
     */
    public record Label(String text, int centerX, int centerY) {
    }

    private SkeletalRenderer() {
        // Utility class: never instantiated.
    }

    /**
     * Paints the bonds of a laid out molecule into a square of pixels.
     *
     * @param structure the drawing to paint
     * @param size side of the square in pixels
     * @param line colour of the bonds
     * @param ring colour of the circle of an aromatic ring
     * @return the pixels, row by row, packed as {@code RGBA8888}, empty where nothing was painted
     */
    public static int[] draw(SkeletalStructure structure, int size, Color line, Color ring) {
        Molecule molecule = structure.molecule();
        int[] pixels = new int[size * size];
        int lineColour = pack(line);
        int ringColour = pack(ring);
        View view = new View(structure, size);
        boolean[] named = new boolean[molecule.atomCount()];
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            named[atom] = isNamed(molecule, atom);
        }
        for (Bond bond : molecule.bonds()) {
            drawBond(pixels, size, view, structure, named, bond, lineColour);
        }
        for (List<Integer> ringAtoms : structure.rings()) {
            drawAromaticRing(pixels, size, view, structure, ringAtoms, ringColour);
        }
        return pixels;
    }

    /**
     * The names the drawing carries, each with the pixel its atom stands at.
     *
     * @param structure the drawing
     * @param size side of the square the drawing is painted into
     * @return one label per atom that is not a carbon, in the order the atoms were written
     */
    public static List<Label> labels(SkeletalStructure structure, int size) {
        Molecule molecule = structure.molecule();
        View view = new View(structure, size);
        List<Label> labels = new ArrayList<>();
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!isNamed(molecule, atom)) {
                continue;
            }
            labels.add(new Label(name(molecule, atom), view.across(structure.x(atom)),
                    view.down(structure.y(atom))));
        }
        return labels;
    }

    /**
     * Where a drawing stands inside a picture, and how large it is drawn.
     * <p>
     * The whole of a drawing is scaled to fit the square with a margin, so a molecule of two atoms and a
     * molecule of twenty read at the same size, and it is centred in what is left over. The vertical axis
     * is flipped here and only here: a layout counts upwards the way geometry does and a picture counts
     * down from its upper edge.
     */
    private static final class View {

        private final double scale;
        private final double left;
        private final double top;
        private final double originX;
        private final double originY;

        private View(SkeletalStructure structure, int size) {
            double width = Math.max(structure.width(), 0.0);
            double height = Math.max(structure.height(), 0.0);
            double usable = Math.max(size - 2.0 * MARGIN, 1.0);
            this.scale = Math.min(usable / Math.max(width, 1.0), usable / Math.max(height, 1.0));
            this.left = MARGIN + (usable - width * scale) / 2.0;
            this.top = MARGIN + (usable - height * scale) / 2.0;
            this.originX = structure.minX();
            this.originY = structure.maxY();
        }

        private int across(double x) {
            return (int) Math.round(left + (x - originX) * scale);
        }

        private int down(double y) {
            return (int) Math.round(top + (originY - y) * scale);
        }

        private double offset() {
            return Math.max(1.0, scale * OFFSET_SHARE);
        }
    }

    /** Paints one bond, one line for a single bond and as many as its order asks for. */
    private static void drawBond(int[] pixels, int size, View view, SkeletalStructure structure,
            boolean[] named, Bond bond, int colour) {
        int first = bond.first();
        int second = bond.second();
        int startX = view.across(structure.x(first));
        int startY = view.down(structure.y(first));
        int endX = view.across(structure.x(second));
        int endY = view.down(structure.y(second));
        double length = Math.hypot(endX - startX, endY - startY);
        if (length < 1e-9) {
            return;
        }
        double alongX = (endX - startX) / length;
        double alongY = (endY - startY) / length;
        if (named[first]) {
            startX += (int) Math.round(alongX * TRIM);
            startY += (int) Math.round(alongY * TRIM);
        }
        if (named[second]) {
            endX -= (int) Math.round(alongX * TRIM);
            endY -= (int) Math.round(alongY * TRIM);
        }
        double offset = view.offset();
        double acrossX = -alongY * offset;
        double acrossY = alongX * offset;
        if (bond.isAromatic() || bond.order() == 1) {
            drawLine(pixels, size, startX, startY, endX, endY, colour);
        } else if (bond.order() == 2) {
            drawLine(pixels, size, startX + round(acrossX), startY + round(acrossY),
                    endX + round(acrossX), endY + round(acrossY), colour);
            drawLine(pixels, size, startX - round(acrossX), startY - round(acrossY),
                    endX - round(acrossX), endY - round(acrossY), colour);
        } else {
            drawLine(pixels, size, startX, startY, endX, endY, colour);
            drawLine(pixels, size, startX + round(acrossX), startY + round(acrossY),
                    endX + round(acrossX), endY + round(acrossY), colour);
            drawLine(pixels, size, startX - round(acrossX), startY - round(acrossY),
                    endX - round(acrossX), endY - round(acrossY), colour);
        }
    }

    /**
     * Paints the circle an aromatic ring carries inside it.
     * <p>
     * The circle is drawn only for a ring whose every bond is aromatic, so a ring that merely happens to be
     * closed - a cyclohexane - keeps its plain single bonds and does not grow a circle it did not earn. The
     * circle is set inside the ring by the inradius of its polygon and pulled in a little further, so it
     * floats clear of the bonds all the way round.
     */
    private static void drawAromaticRing(int[] pixels, int size, View view, SkeletalStructure structure,
            List<Integer> ring, int colour) {
        Molecule molecule = structure.molecule();
        for (int index = 0; index < ring.size(); index++) {
            Bond bond = bondBetween(molecule, ring.get(index), ring.get((index + 1) % ring.size()));
            if (bond == null || !bond.isAromatic()) {
                return;
            }
        }
        double centerX = 0.0;
        double centerY = 0.0;
        for (int atom : ring) {
            centerX += structure.x(atom);
            centerY += structure.y(atom);
        }
        centerX /= ring.size();
        centerY /= ring.size();
        int middleX = view.across(centerX);
        int middleY = view.down(centerY);
        double radius = 0.0;
        for (int atom : ring) {
            radius += Math.hypot(view.across(structure.x(atom)) - middleX,
                    view.down(structure.y(atom)) - middleY);
        }
        double outer = radius / ring.size();
        double inner = outer * Math.cos(Math.PI / ring.size()) * 0.82;
        drawCircle(pixels, size, middleX, middleY, Math.max(inner, 1.0), colour);
    }

    /** {@code true} when an atom carries a name: anything but a carbon, and a lone atom of any kind. */
    private static boolean isNamed(Molecule molecule, int atom) {
        if (molecule.neighbours(atom).isEmpty()) {
            return true;
        }
        return !"C".equals(molecule.atom(atom).element());
    }

    /** The letters an atom is named with, its hydrogens and its charge, or its whole formula when it stands alone. */
    private static String name(Molecule molecule, int atom) {
        if (molecule.atomCount() == 1) {
            return molecule.composition().formula();
        }
        Atom element = molecule.atom(atom);
        StringBuilder text = new StringBuilder(element.element());
        if (element.hydrogens() > 0) {
            text.append('H');
            if (element.hydrogens() > 1) {
                text.append(element.hydrogens());
            }
        }
        if (element.charge() != 0) {
            text.append(element.charge() > 0 ? '+' : '-');
            if (Math.abs(element.charge()) > 1) {
                text.append(Math.abs(element.charge()));
            }
        }
        return text.toString();
    }

    /** The bond between two atoms, or {@code null} when they share none. */
    private static Bond bondBetween(Molecule molecule, int first, int second) {
        for (int bondIndex : molecule.bondsOf(first)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(first) == second) {
                return bond;
            }
        }
        return null;
    }

    /** Paints one line of pixels, walking a whole step at a time between its two ends. */
    private static void drawLine(int[] pixels, int size, int startX, int startY, int endX, int endY,
            int colour) {
        int spanX = Math.abs(endX - startX);
        int spanY = -Math.abs(endY - startY);
        int stepX = startX < endX ? 1 : -1;
        int stepY = startY < endY ? 1 : -1;
        int error = spanX + spanY;
        int x = startX;
        int y = startY;
        while (true) {
            set(pixels, size, x, y, colour);
            if (x == endX && y == endY) {
                break;
            }
            int doubled = 2 * error;
            if (doubled >= spanY) {
                error += spanY;
                x += stepX;
            }
            if (doubled <= spanX) {
                error += spanX;
                y += stepY;
            }
        }
    }

    /** Paints the outline of a circle, one pixel thick. */
    private static void drawCircle(int[] pixels, int size, int centerX, int centerY, double radius,
            int colour) {
        int fromX = (int) Math.floor(centerX - radius - 1.0);
        int toX = (int) Math.ceil(centerX + radius + 1.0);
        int fromY = (int) Math.floor(centerY - radius - 1.0);
        int toY = (int) Math.ceil(centerY + radius + 1.0);
        for (int y = fromY; y <= toY; y++) {
            for (int x = fromX; x <= toX; x++) {
                double distance = Math.hypot(x + 0.5 - centerX, y + 0.5 - centerY);
                if (Math.abs(distance - radius) < 0.6) {
                    set(pixels, size, x, y, colour);
                }
            }
        }
    }

    /** Rounds a distance travelled along a bond to whole pixels. */
    private static int round(double value) {
        return (int) Math.round(value);
    }

    /** Writes one pixel, ignoring a place that reaches outside the picture. */
    private static void set(int[] pixels, int size, int x, int y, int colour) {
        if (x < 0 || y < 0 || x >= size || y >= size) {
            return;
        }
        pixels[y * size + x] = colour;
    }

    /** Packs a colour into a pixel the way a {@code Pixmap} holds it. */
    private static int pack(Color colour) {
        return (channel(colour.r) << 24) | (channel(colour.g) << 16) | (channel(colour.b) << 8)
                | channel(colour.a);
    }

    /** Rounds a colour channel and keeps it inside the range of a byte. */
    private static int channel(float value) {
        return Math.min(Math.max(Math.round(value * CHANNEL_MAX), 0), CHANNEL_MAX);
    }
}

