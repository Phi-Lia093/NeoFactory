package com.philia093.neofactory.energy;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.machine.EnergyStorage;
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
 * <b>What hangs on a line are the machines next to its cables.</b> A machine of the game carries a buffer of
 * energy, see {@link EnergyStorage} and {@code MachineBlockEntity}, and a machine whose buffer takes energy
 * in is a machine the line may feed. The ends are collected while the line is walked, so the net of a line
 * and the machines it reaches are one question and not two.
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
         * Buffer of energy of the machine in a cell.
         *
         * @return the storage, or {@code null} when no machine with a buffer stands there
         */
        EnergyStorage buffer(int x, int y, int z);
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
        public EnergyStorage buffer(int x, int y, int z) {
            if (!(world.blockEntity(x, y, z) instanceof MachineBlockEntity)) {
                return null;
            }
            return ((MachineBlockEntity) world.blockEntity(x, y, z)).machine().energy();
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
        private final List<EnergyStorage> ends;
        private final List<int[]> positions;
        private final EnergyNet net;

        private Line(List<Cables.Cable> cables, List<EnergyStorage> ends, List<int[]> positions) {
            this.cables = List.copyOf(cables);
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
         * Moves energy out of one end of the line into every machine of it that takes energy in.
         * <p>
         * What one tick of the line is worth, {@link EnergyNet#capacity()}, is the offer, and it is shared
         * out evenly between the machines that can take it. Every share travels through
         * {@link EnergyNet#carry}, so the loss of the run is paid by the source, a machine whose buffer is
         * full simply takes nothing, and the rest of a division nobody wanted stays in the source.
         *
         * @param source buffer that gives the energy
         * @param wanted amount the line should hand over
         * @return the amount the machines of the line received
         */
        public int push(EnergyStorage source, int wanted) {
            List<EnergyStorage> sinks = new ArrayList<>();
            for (EnergyStorage end : ends) {
                if (end != source && end.canReceive()) {
                    sinks.add(end);
                }
            }
            if (sinks.isEmpty() || wanted <= 0) {
                return 0;
            }
            int share = Math.max(1, wanted / sinks.size());
            int moved = 0;
            for (EnergyStorage sink : sinks) {
                moved += net.carry(source, sink, share);
            }
            return moved;
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
                EnergyStorage buffer = cells.buffer(nx, ny, nz);
                if (buffer != null && seenEnds.add(key(nx, ny, nz))) {
                    ends.add(buffer);
                    positions.add(new int[] {nx, ny, nz});
                }
            }
        }
        return new Line(cables, ends, positions);
    }

    /** Key of a cell, so that a set of visited cells holds no position twice. */
    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }
}
