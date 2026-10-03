package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.Cables;
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
         * Empties a cell, which is what an over-voltage leaves behind.
         * <p>
         * A line of a higher tier than a machine was built for takes the machine and every cable of the line
         * with it, see {@link EnergyNet#overvolts(EnergyAcceptor)}, so the net has to be able to take a block
         * away.
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
         * <b>A line that is too much for the machine that asks destroys it and itself.</b> The tier is
         * settled before anything is carried: a machine whose own tier stands below the tier of the line, see
         * {@link EnergyNet#overvolts(EnergyAcceptor)}, is not fed at all - it and every cable of the line are
         * taken out of the world, which is what a player finds when a line of a later age is run into a
         * workshop of an earlier one. A machine that asks for nothing never meets the line, so nothing burns
         * while it is idle.
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
            if (overvolts(sink)) {
                burn(cells, List.of(sink));
                return 0;
            }
            if (wanted <= 0 || !sink.canReceive()) {
                return 0;
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

        /** {@code true} when this line is too much for the machine of one of its ends. */
        public boolean overvolts(EnergyStorage end) {
            return end instanceof EnergyAcceptor && net.overvolts((EnergyAcceptor) end);
        }

        /**
         * Takes the machine and every cable of the line out of the world.
         *
         * @param cells cells of the world
         * @param burned machines of this line the tier destroyed
         * @return amount of cells that were emptied
         */
        public int burn(Cells cells, List<EnergyStorage> burned) {
            Objects.requireNonNull(cells, "cells");
            Set<Long> taken = new HashSet<>();
            for (int[] cell : cablesAt) {
                if (taken.add(key(cell[0], cell[1], cell[2]))) {
                    cells.remove(cell[0], cell[1], cell[2]);
                }
            }
            int count = cablesAt.size();
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
