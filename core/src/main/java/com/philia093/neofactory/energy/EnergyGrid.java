package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.EnergyStorage;
import com.philia093.neofactory.machine.FaceConfig;
import com.philia093.neofactory.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The net of cables a world holds: which cables are joined to which, and which machines hang on them.
 * <p>
 * The line of a net is found by walking the cables that are joined to one another, the way a player reads a
 * line off the ground: <b>a cable joins the side its state says it joins</b>, see {@link Cables}, so a walk
 * from one cable to the next only ever goes through a joined pair. Two lines that meet at a wall, and two
 * that cross without a join, stay apart - a line is a thing a player builds and not something the game
 * guesses at.
 * <p>
 * <b>What hangs on a line are the machines next to its cables, through the side each of them was given for
 * the power.</b> A machine of the game carries a buffer of energy and two plugs - the one a line feeds it
 * through and the one it feeds a line through, see {@link EnergyStorage} and {@code FaceConfig} - and the walk
 * counts a machine as an end of the line only while the cable that reaches it stands at one of those two
 * sides. The ends are collected while the line is walked, so the net of a line and the machines it reaches are
 * one question and not two.
 * <p>
 * <b>Nothing here knows the world directly.</b> Every cell is asked through {@link Cells}, which a world
 * answers through {@link #of(World)} and which a test answers with a map, see {@code EnergyGridTest}: the
 * arithmetic of a line is checkable without a world, the same way the transport of the fluid system is.
 */
public final class EnergyGrid {

    /**
     * What a net asks about a cell of the world.
     */
    public interface Cells {

        /** Block that stands in a cell. */
        Block block(int x, int y, int z);

        /** State the cell carries, which is the mask of a cable. */
        int state(int x, int y, int z);

        /**
         * Buffer of energy of the machine in a cell, reached from one side of it.
         * <p>
         * <b>A line hangs on the side a machine was given for the power.</b> The energy of a machine is
         * reached through one side of its block and through no other - the plug a player set with the wrench,
         * see {@code FaceConfig} - so a cable that stands at a side the machine carries no plug on is a cable
         * the machine has nothing to do with. The side is handed in as the side of the machine, which is the
         * opposite of the side of the cable that reaches it.
         *
         * @param x x of the cell
         * @param y y of the cell
         * @param z z of the cell
         * @param from side of that cell the line reaches it through
         * @return the storage, or {@code null} when no machine with a plug on that side stands there
         */
        EnergyStorage buffer(int x, int y, int z, BlockFace from);

        /**
         * Empties a cell, which is what a line that burns leaves behind.
         * <p>
         * A machine that was built for a worse line than the one it reaches for, and a piece of a run that
         * cannot take what its line is fed with, are taken out of the world, see
         * {@link EnergyNet#overvolts(Voltage, EnergyStorage)}, so the net has to be able to take a block away.
         *
         * @param x x of the cell
         * @param y y of the cell
         * @param z z of the cell
         */
        void remove(int x, int y, int z);
    }

    /** The cells of a world: its blocks, its states and the buffers of its machines. */
    private static final class WorldCells implements Cells {

        private final World world;

        private WorldCells(World world) {
            this.world = world;
        }

        @Override
        public Block block(int x, int y, int z) {
            return world.getBlock(x, y, z);
        }

        @Override
        public int state(int x, int y, int z) {
            return world.getState(x, y, z);
        }

        @Override
        public EnergyStorage buffer(int x, int y, int z, BlockFace from) {
            if (!(world.blockEntity(x, y, z) instanceof MachineBlockEntity machine)) {
                return null;
            }
            FaceConfig faces = machine.machine().faces();
            if (faces.energyIn() != from && faces.energyOut() != from) {
                // The machine carries no plug on that side, so the line does not hang on it: a side may carry
                // one job at a time and the front carries none at all, see FaceConfig.
                return null;
            }
            return machine.machine().energy();
        }

        @Override
        public void remove(int x, int y, int z) {
            world.setBlock(x, y, z, Blocks.AIR);
        }

        @Override
        public String toString() {
            return "WorldCells";
        }
    }

    /**
     * The cells of a world.
     *
     * @param world world a line is walked in
     * @return the cells
     */
    public static Cells of(World world) {
        return new WorldCells(Objects.requireNonNull(world, "world"));
    }

    /**
     * One line of the power as a world holds it: its cables, the machines at its ends and what it carries.
     */
    public static final class Line {

        private final List<Cables.Cable> cables;
        private final List<int[]> cablesAt;
        private final List<EnergyStorage> ends;
        private final List<int[]> positions;
        private final EnergyNet net;

        private Line(List<Cables.Cable> cables, List<int[]> cablesAt, List<EnergyStorage> ends,
                List<int[]> positions) {
            this.cables = List.copyOf(cables);
            this.cablesAt = List.copyOf(cablesAt);
            this.ends = List.copyOf(ends);
            this.positions = List.copyOf(positions);
            this.net = EnergyNet.of(cables);
        }

        /** The cables of this line, the cell the walk started at first. */
        public List<Cables.Cable> cables() {
            return cables;
        }

        /** The buffers of the machines next to the cables of this line. */
        public List<EnergyStorage> ends() {
            return ends;
        }

        /** Position of every end, in the order of {@link #ends()}. */
        public List<int[]> endPositions() {
            return positions;
        }

        /** What this line carries a tick, see {@link EnergyNet}. */
        public EnergyNet net() {
            return net;
        }

        /** Amount of cable blocks of this line. */
        public int length() {
            return cables.size();
        }

        /** {@code true} when a buffer is one of the ends of this line. */
        public boolean reaches(EnergyStorage storage) {
            for (EnergyStorage end : ends) {
                if (end == storage) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Brings the energy a machine of this line asks for out of the ends that can give it.
         * <p>
         * <b>A cable carries nothing of its own, so the machine that works is the one that moves the
         * energy.</b> The machine at the end of the line is the one that asks, and the line answers with
         * what its ends can give: every end that is a source - a buffer that may be emptied, which is the
         * buffer of a machine that makes power and of nothing that only works - hands over an even share of
         * what was asked for. Every share travels through {@link EnergyNet#carry}, so the loss of the run is
         * paid by the source, a line that nobody feeds leaves the machine waiting, and the rest of a division
         * nobody wanted stays where it was.
         * <p>
         * <b>What the line is fed with decides what it burns, and the two halves of that are weighed
         * apart.</b> The tier of a line is the one the machines that give it their power are built for,
         * {@link #liveVoltage(EnergyStorage)}: a machine that asks a line of a higher tier than its own is
         * not fed at all and is taken out of the world, which is what a player finds when a line of a later
         * age is run into a workshop of an earlier one, while a piece of the run that cannot take that much
         * melts where the power enters the line, see {@link #melt(Cells, EnergyStorage)}. A machine of the right
         * tier therefore outlives a run that was too weak for it, and a machine that was too small for its
         * line does not take the cables of that line away with it. A machine that asks for nothing never
         * meets the line, so nothing burns while it is idle.
         *
         * @param cells cells of the world, so that a line that burns can be taken away
         * @param sink buffer that wants the energy
         * @param wanted amount the sink asks for
         * @return the amount the sink received, {@code 0} when the line burned or nothing was given
         */
        public int pull(Cells cells, EnergyStorage sink, int wanted) {
            Objects.requireNonNull(cells, "cells");
            Objects.requireNonNull(sink, "sink");
            if (!reaches(sink)) {
                // A machine that is no end of this line is not fed by it, however it was asked to.
                return 0;
            }
            if (wanted <= 0 || !sink.canReceive()) {
                return 0;
            }
            // What the machines that give this line their power are built for is settled before anything is
            // carried, and it is what the machine that asks and the run itself have to take, see the note on
            // this method.
            Voltage live = liveVoltage(sink);
            if (live != null) {
                boolean mine = EnergyNet.overvolts(live, sink);
                int melted = melt(cells, sink);
                if (mine) {
                    burn(cells, List.of(sink));
                }
                if (mine || melted > 0) {
                    return 0;
                }
            }
            List<EnergyStorage> sources = new ArrayList<>();
            for (EnergyStorage end : ends) {
                if (end != sink && end.canExtract()) {
                    sources.add(end);
                }
            }
            if (sources.isEmpty()) {
                return 0;
            }
            int share = Math.max(1, wanted / sources.size());
            int moved = 0;
            for (EnergyStorage source : sources) {
                moved += net.carry(source, sink, share);
            }
            return moved;
        }

        /**
         * The tier the machines that give this line their power are built for, the <b>highest</b> of them.
         * <p>
         * <b>A line is fed by the machines at its ends and not by its cables.</b> What stands on a run is
         * what the machines that push into it are built for - a box of the high voltage feeds a line of the
         * high voltage whatever the material of that line is - and a run of two of them is a run of the
         * higher of the two, because that is the power everything on it has to take. The machine that asks
         * for the power is not counted: what is weighed is what the line is given, not what the machine
         * asking for it holds.
         * <p>
         * An end that gives nothing away names no tier: a machine that only works spends out of its own
         * pocket and is never a source of the line, and a box with no cell in it holds nothing to give, see
         * {@link EnergyStorage#canExtract()}. A line that stands along no machine that gives is fed with
         * nothing at all and burns nothing, however strong its cables are.
         *
         * @param asking machine that asks this line for power, which is not one of the machines that give it
         * @return the tier, or {@code null} when no machine feeds this line
         */
        public Voltage liveVoltage(EnergyStorage asking) {
            Objects.requireNonNull(asking, "asking");
            Voltage live = null;
            for (EnergyStorage end : ends) {
                if (end == asking || !end.canExtract() || !(end instanceof EnergyAcceptor giving)) {
                    continue;
                }
                if (live == null || giving.accepted().isAtLeast(live)) {
                    live = giving.accepted();
                }
            }
            return live;
        }

        /** {@code true} when this line is too much for the machine of one of its ends. */
        public boolean overvolts(EnergyStorage end) {
            Voltage live = liveVoltage(end);
            return live != null && EnergyNet.overvolts(live, end);
        }

        /**
         * Melts one piece of this line: the piece that stands nearest the machines that give it its power.
         * <p>
         * <b>A run of a cable that cannot take what its line is fed with melts where the power enters
         * it.</b> Every cable of the run is weighed against the tier of the machines that give the line their
         * power, see {@link #liveVoltage(EnergyStorage)}, and the first piece that cannot take that much and
         * stands right at one of those machines goes - <b>one piece</b> of the run, which is what a player
         * finds when the line of a workshop of a later age is run through the cable of an earlier one. A line
         * of one material melts at the end it is fed from, and a run that stands on no machine that gives
         * melts nothing at all.
         *
         * @param cells cells of the world
         * @param asking machine that asks this line for power, which is not one of the machines that give it
         * @return amount of cells that were emptied, one or none
         */
        public int melt(Cells cells, EnergyStorage asking) {
            Objects.requireNonNull(cells, "cells");
            Voltage live = liveVoltage(Objects.requireNonNull(asking, "asking"));
            if (live == null) {
                return 0;
            }
            for (int end = 0; end < ends.size(); end++) {
                if (ends.get(end) == asking || !ends.get(end).canExtract()) {
                    continue;
                }
                int[] at = positions.get(end);
                for (int index = 0; index < cablesAt.size(); index++) {
                    int[] cell = cablesAt.get(index);
                    if (touches(at, cell) && !cables.get(index).voltage().isAtLeast(live)) {
                        return emptyOut(cells, cell);
                    }
                }
            }
            for (int index = 0; index < cablesAt.size(); index++) {
                if (!cables.get(index).voltage().isAtLeast(live)) {
                    return emptyOut(cells, cablesAt.get(index));
                }
            }
            return 0;
        }

        /** Empties one cell of the run, which is one piece of the line gone. */
        private static int emptyOut(Cells cells, int[] cell) {
            cells.remove(cell[0], cell[1], cell[2]);
            return 1;
        }

        /** {@code true} when two cells of the world stand next to each other. */
        private static boolean touches(int[] one, int[] other) {
            return Math.abs(one[0] - other[0]) + Math.abs(one[1] - other[1]) + Math.abs(one[2] - other[2]) == 1;
        }

        /**
         * Takes the machines of a line out of the world.
         *
         * @param cells cells of the world
         * @param burned machines of this line the tier destroyed
         * @return amount of cells that were emptied
         */
        public int burn(Cells cells, List<EnergyStorage> burned) {
            Objects.requireNonNull(cells, "cells");
            Set<Long> taken = new HashSet<>();
            int count = 0;
            for (int index = 0; index < ends.size(); index++) {
                if (!burned.contains(ends.get(index))) {
                    continue;
                }
                int[] cell = positions.get(index);
                if (taken.add(key(cell[0], cell[1], cell[2]))) {
                    cells.remove(cell[0], cell[1], cell[2]);
                    count++;
                }
            }
            return count;
        }

        @Override
        public String toString() {
            return "Line(" + cables.size() + " cables, " + ends.size() + " ends, " + net + ")";
        }
    }

    /** Step of a direction, one cell along one of its three axes. */
    private static int step(BlockFace face, int axis) {
        int sign = face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.TOP ? 1 : -1;
        boolean horizontal = face == BlockFace.NORTH || face == BlockFace.SOUTH;
        int mine = face == BlockFace.EAST || face == BlockFace.WEST ? 0 : horizontal ? 2 : 1;
        return mine == axis ? sign : 0;
    }

    /**
     * Walks the line the cable of a cell belongs to.
     * <p>
     * The walk starts at that cell and follows every side the cable joins into another cable, so the whole
     * line is collected once. Every other neighbour that carries a buffer of energy is an end of the line,
     * and a machine that stands next to two cables of the same line is written down once.
     *
     * @param cells cells of the world
     * @param x x of the cable the walk starts at
     * @param y y of the cable the walk starts at
     * @param z z of the cable the walk starts at
     * @return the line, or {@code null} when no cable stands in that cell
     */
    public static Line line(Cells cells, int x, int y, int z) {
        Objects.requireNonNull(cells, "cells");
        if (Cables.of(cells.block(x, y, z)) == null) {
            return null;
        }
        List<Cables.Cable> cables = new ArrayList<>();
        List<int[]> cablesAt = new ArrayList<>();
        List<EnergyStorage> ends = new ArrayList<>();
        List<int[]> positions = new ArrayList<>();
        Set<Long> seenCables = new HashSet<>();
        Set<Long> seenEnds = new HashSet<>();
        List<int[]> open = new ArrayList<>();
        open.add(new int[] {x, y, z});
        seenCables.add(key(x, y, z));
        while (!open.isEmpty()) {
            int[] cell = open.remove(open.size() - 1);
            cables.add(Cables.of(cells.block(cell[0], cell[1], cell[2])));
            cablesAt.add(new int[] {cell[0], cell[1], cell[2]});
            int state = cells.state(cell[0], cell[1], cell[2]);
            for (BlockFace face : Cables.DIRECTIONS) {
                if (!Cables.isConnected(state, face)) {
                    continue;
                }
                int nx = cell[0] + step(face, 0);
                int ny = cell[1] + step(face, 1);
                int nz = cell[2] + step(face, 2);
                if (Cables.of(cells.block(nx, ny, nz)) != null) {
                    if (seenCables.add(key(nx, ny, nz))) {
                        open.add(new int[] {nx, ny, nz});
                    }
                    continue;
                }
                EnergyStorage buffer = cells.buffer(nx, ny, nz, face.opposite());
                if (buffer != null && seenEnds.add(key(nx, ny, nz))) {
                    ends.add(buffer);
                    positions.add(new int[] {nx, ny, nz});
                }
            }
        }
        return new Line(cables, cablesAt, ends, positions);
    }

    /** Key of a cell, so that a set of visited cells holds no position twice. */
    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }
}
