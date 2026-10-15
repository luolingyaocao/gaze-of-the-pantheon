package com.onceheart.gazeofthepantheon.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onceheart.gazeofthepantheon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PrimordialChunkGenerator extends NoiseBasedChunkGenerator {

    private static final double RARIFIED_BASE = 200.0;
    private static final double TURBID_BASE = -30.0;
    private static final double BOUNDARY_AMPLITUDE = 10.0;
    private static final double BOUNDARY_FREQ = 1.0 / 64.0;
    private static final int BOUNDARY_OCTAVES = 4;

    private static final long SEED_TOP = 0x9E3779B97F4A7C15L;
    private static final long SEED_BOTTOM = 0xC2B2AE3D27D4EB4FL;

    private static final float MATTER_CHANCE = 0.3F;
    private static final int MATTER_MIN_DEPTH = 20;
    private static final int MATTER_MAX_CLUSTER = 3;

    /** 世界顶层：一律填至清之气 */
    private static final int FORCED_TOP_Y = 319;

    public static final MapCodec<PrimordialChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings)
            ).apply(instance, PrimordialChunkGenerator::new)
    );

    public PrimordialChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC.codec();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        return super.fillFromNoise(executor, blender, randomState, structureManager, chunk).thenApply(thisChunk -> {
            applyPrimordialLayers(thisChunk);
            return thisChunk;
        });
    }

    private void applyPrimordialLayers(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;

        BlockState rarified = ModBlocks.RARIFIED_ESSENCE.get().defaultBlockState();
        BlockState turbid = ModBlocks.TURBID_ESSENCE.get().defaultBlockState();

        double[] topBounds = new double[16 * 16];
        double[] bottomBounds = new double[16 * 16];

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int wx = minX + dx;
                int wz = minZ + dz;
                int idx = dx * 16 + dz;

                double topBound = RARIFIED_BASE
                        + (fbm2D(SEED_TOP, wx * BOUNDARY_FREQ, wz * BOUNDARY_FREQ, BOUNDARY_OCTAVES) * 2 - 1)
                        * BOUNDARY_AMPLITUDE;
                double bottomBound = TURBID_BASE
                        + (fbm2D(SEED_BOTTOM, wx * BOUNDARY_FREQ, wz * BOUNDARY_FREQ, BOUNDARY_OCTAVES) * 2 - 1)
                        * BOUNDARY_AMPLITUDE;

                topBounds[idx] = topBound;
                bottomBounds[idx] = bottomBound;

                int topY = (int) Math.ceil(topBound);
                int bottomY = (int) Math.floor(bottomBound);

                for (int y = topY; y <= maxY; y++) {
                    chunk.setBlockState(new BlockPos(wx, y, wz), rarified, false);
                }
                for (int y = minY; y <= bottomY; y++) {
                    chunk.setBlockState(new BlockPos(wx, y, wz), turbid, false);
                }

                if (FORCED_TOP_Y >= topY) {
                    chunk.setBlockState(new BlockPos(wx, FORCED_TOP_Y, wz), rarified, false);
                }
            }
        }

        generatePrimordialMatter(chunk, minX, minZ, minY, maxY, topBounds, bottomBounds);
    }

    private void generatePrimordialMatter(ChunkAccess chunk, int minX, int minZ, int minY, int maxY,
                                          double[] topBounds, double[] bottomBounds) {
        ChunkPos chunkPos = chunk.getPos();
        RandomSource random = RandomSource.create(chunkPos.toLong());

        if (random.nextFloat() >= MATTER_CHANCE) {
            return;
        }

        boolean upper = random.nextBoolean();

        int dx = 2 + random.nextInt(12);
        int dz = 2 + random.nextInt(12);
        int idx = dx * 16 + dz;

        int wx = minX + dx;
        int wz = minZ + dz;

        int y;
        if (upper) {
            int low = (int) Math.ceil(topBounds[idx]) + MATTER_MIN_DEPTH;
            int high = maxY - 1;
            if (low >= high) return;
            y = low + random.nextInt(high - low + 1);
        } else {
            int low = minY + 1;
            int high = (int) Math.floor(bottomBounds[idx]) - MATTER_MIN_DEPTH;
            if (low >= high) return;
            y = low + random.nextInt(high - low + 1);
        }

        int count = 1 + random.nextInt(MATTER_MAX_CLUSTER);

        for (int i = 0; i < count; i++) {
            int ox = wx + random.nextInt(3) - 1;
            int oy = y + random.nextInt(3) - 1;
            int oz = wz + random.nextInt(3) - 1;

            if (oy <= minY || oy >= maxY) continue;

            BlockPos pos = new BlockPos(ox, oy, oz);

            if (!isSurroundedByEssence(chunk, pos, upper)) {
                continue;
            }

            chunk.setBlockState(pos, ModBlocks.PRIMORDIAL_MATTER.get().defaultBlockState(), false);
        }
    }

    private boolean isSurroundedByEssence(ChunkAccess chunk, BlockPos pos, boolean upper) {
        BlockState target = upper
                ? ModBlocks.RARIFIED_ESSENCE.get().defaultBlockState()
                : ModBlocks.TURBID_ESSENCE.get().defaultBlockState();

        return chunk.getBlockState(pos.north()).equals(target)
                && chunk.getBlockState(pos.south()).equals(target)
                && chunk.getBlockState(pos.east()).equals(target)
                && chunk.getBlockState(pos.west()).equals(target)
                && chunk.getBlockState(pos.above()).equals(target)
                && chunk.getBlockState(pos.below()).equals(target);
    }

    // ============ 值噪声工具 ============

    private static double hash2(long seed, int x, int z) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 31);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double valueNoise2D(long seed, double x, double z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double xf = x - xi;
        double zf = z - zi;
        double u = smoothstep(xf);
        double v = smoothstep(zf);
        double a = hash2(seed, xi, zi);
        double b = hash2(seed, xi + 1, zi);
        double c = hash2(seed, xi, zi + 1);
        double d = hash2(seed, xi + 1, zi + 1);
        return lerp(lerp(a, b, u), lerp(c, d, u), v);
    }

    private static double fbm2D(long seed, double x, double z, int octaves) {
        double sum = 0;
        double amp = 1;
        double freq = 1;
        double norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += valueNoise2D(seed + i * 7919L, x * freq, z * freq) * amp;
            norm += amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return sum / norm;
    }
}