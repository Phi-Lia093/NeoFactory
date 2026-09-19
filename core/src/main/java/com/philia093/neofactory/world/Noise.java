package com.philia093.neofactory.world;

/**
 * Deterministic, allocation free value noise generator.
 * <p>
 * The implementation is intentionally simple: it hashes integer lattice points
 * with a seed and interpolates between them with a smoothstep curve. Layering
 * several octaves on top of each other (called fBm) produces terrain that looks
 * organic while staying perfectly reproducible for a given seed.
 */
public final class Noise {

    private final int seed;

    /**
     * Creates a generator.
     *
     * @param seed any value, equal seeds always produce equal noise
     */
    public Noise(int seed) {
        this.seed = seed;
    }

    /**
     * Hashes an integer lattice point into the range {@code [0, 1)}.
     * <p>
     * The point is mixed with a large odd constant per axis so that neighbouring
     * coordinates do not produce correlated values.
     */
    public static float hash(int x, int y, int seed) {
        int h = seed;
        h ^= x * 0x27D4EB2D;
        h ^= y * 0x165667B1;
        h = Integer.rotateLeft(h, 13);
        h *= 0x85EBCA6B;
        h ^= h >>> 16;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        // Map the signed hash into [0, 1) without losing precision for small values.
        return (h >>> 8) * (1.0f / (1 << 24));
    }

    /** Hash of the single argument lattice {@code (x, 0)}. */
    public float hash1(int x) {
        return hash(x, 0, seed);
    }

    /** Smoothstep interpolation curve, also known as {@code 3t^2 - 2t^3}. */
    private static float smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    /**
     * Performs 1D value noise at the given position.
     *
     * @param x position on the noise axis, values below zero are allowed
     * @return a value in the range {@code [0, 1]}
     */
    public float value1(float x) {
        return value1(x, 0);
    }

    /**
     * Performs 1D value noise with an extra row offset, useful to derive several
     * independent noise fields from one generator.
     *
     * @param x position on the noise axis
     * @param row extra hash offset, different rows give uncorrelated curves
     * @return a value in the range {@code [0, 1]}
     */
    public float value1(float x, int row) {
        int x0 = (int) Math.floor(x);
        int x1 = x0 + 1;
        float t = smooth(x - x0);
        float a = hash(x0, row, seed);
        float b = hash(x1, row, seed);
        return a + (b - a) * t;
    }

    /**
     * Performs 2D value noise at the given position.
     *
     * @param x position on the first noise axis
     * @param y position on the second noise axis
     * @return a value in the range {@code [0, 1]}
     */
    public float value2(float x, float y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        float tx = smooth(x - x0);
        float ty = smooth(y - y0);

        float c00 = hash(x0, y0, seed);
        float c10 = hash(x1, y0, seed);
        float c01 = hash(x0, y1, seed);
        float c11 = hash(x1, y1, seed);

        float bottom = c00 + (c10 - c00) * tx;
        float top = c01 + (c11 - c01) * tx;
        return bottom + (top - bottom) * ty;
    }

    /**
     * Fractal brownian motion, several octaves of 1D value noise added together.
     * <p>
     * Averaging octaves would concentrate the result around {@code 0.5}, which
     * produces flat terrain. The value is therefore stretched back to the full
     * range by applying a gain factor, see {@link #FBM_GAIN}.
     *
     * @param x position on the noise axis
     * @param octaves amount of noise layers, at least one
     * @param frequency frequency of the first octave
     * @param persistence amplitude factor between two octaves
     * @param row extra hash offset, see {@link #value1(float, int)}
     * @return a value roughly in the range {@code [0, 1]}
     */
    public float fbm1(float x, int octaves, float frequency, float persistence, int row) {
        float sum = 0.0f;
        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;
        float freq = frequency;

        for (int i = 0; i < octaves; i++) {
            sum += value1(x * freq, row + i * 3) * amplitude;
            totalAmplitude += amplitude;
            amplitude *= persistence;
            freq *= 2.0f;
        }
        float normalized = totalAmplitude == 0.0f ? 0.0f : sum / totalAmplitude;
        return clamp01(0.5f + (normalized - 0.5f) * FBM_GAIN);
    }

    /**
     * Gain applied to the fBm result to widen its narrow distribution.
     * <p>
     * Layered noise naturally clusters around {@code 0.5}; without this factor the
     * terrain height would be almost constant.
     */
    private static final float FBM_GAIN = 1.8f;

    /** Restricts a value to the range {@code [0, 1]}. */
    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        return value > 1.0f ? 1.0f : value;
    }

    /** Convenience overload of {@link #fbm1(float, int, float, float, int)} using row 0. */
    public float fbm1(float x, int octaves, float frequency, float persistence) {
        return fbm1(x, octaves, frequency, persistence, 0);
    }

    /**
     * Fractal brownian motion built from 2D value noise.
     * <p>
     * This is the two dimensional counterpart of {@link #fbm1} and is used to
     * build the biome map and the patchy ground details of a top down world. The
     * same stretching of {@link #FBM_GAIN} is applied, so the result spreads over
     * the whole range instead of clustering around {@code 0.5}.
     *
     * @param x position on the first noise axis
     * @param y position on the second noise axis
     * @param octaves amount of noise layers, at least one
     * @param frequency frequency of the first octave
     * @param persistence amplitude factor between two octaves
     * @return a value roughly in the range {@code [0, 1]}
     */
    public float fbm2(float x, float y, int octaves, float frequency, float persistence) {
        float sum = 0.0f;
        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;
        float freq = frequency;

        for (int i = 0; i < octaves; i++) {
            // The octave index is folded into the sample position, which gives
            // every octave its own uncorrelated lattice without extra state.
            sum += value2(x * freq + i * 31.7f, y * freq + i * 17.3f) * amplitude;
            totalAmplitude += amplitude;
            amplitude *= persistence;
            freq *= 2.0f;
        }
        float normalized = totalAmplitude == 0.0f ? 0.0f : sum / totalAmplitude;
        return clamp01(0.5f + (normalized - 0.5f) * FBM_GAIN);
    }
}