package com.onceheart.gazeofthepantheon.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 奥林匹斯山 + 山尖。
 *
 * 山尖底部从「山脊环高度」起步，向中心逐渐抬升 20 格，与山脊环无缝衔接。
 * 穹顶形状是「从山脊环隆起的锥」，不悬空、无柱。
 * 穹顶覆盖范围内，地表上方一格放 light 方块（光照 15，防刷怪）。
 * 穹顶底部（下表面）用低频噪声点缀荧石，稀疏分布。
 */
public class OlympusMonsFeature extends Feature<NoneFeatureConfiguration> {

    private static final int CENTER_X = 10000;
    private static final int CENTER_Z = 10000;
    private static final int RADIUS = 128;
    private static final double RADIUS_SQ = (double) RADIUS * RADIUS;

    private static final double PLATEAU_RADIUS = 50.0;
    private static final double DIP_DEPTH = 20.0;
    private static final double FLAT_RATIO = 0.6;

    private static final int MAX_HEIGHT = 140;
    private static final int SEA_LEVEL = 62;
    private static final double GRASS_RADIUS = PLATEAU_RADIUS * 0.85;

    private static final int BUILD_LIMIT = 319;

    private static final double DOME_BASE_RADIUS = 50.0;
    private static final double DOME_PEAK_HEIGHT = 80.0;
    private static final double DOME_NOISE_AMP = 2.5;
    private static final int DOME_BOTTOM_CLEARANCE = 20;
    private static final double DOME_SHARPNESS = 4.0;

    /** 穹顶底部荧石阈值（fbm 结果大于此值则放荧石），越高越稀疏 */
    private static final double DOME_GLOW_THRESHOLD = 0.78;
    /** 穹顶底部荧石噪声频率，越小团越大 */
    private static final double DOME_GLOW_FREQ = 0.12;

    public OlympusMonsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        long seed = level.getSeed();

        int chunkMinX = origin.getX() & ~15;
        int chunkMinZ = origin.getZ() & ~15;

        boolean placedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int wx = chunkMinX + dx;
                int wz = chunkMinZ + dz;

                double relX = wx - CENTER_X;
                double relZ = wz - CENTER_Z;
                double distSq = relX * relX + relZ * relZ;
                if (distSq > RADIUS_SQ * 2.0) continue;

                double dist = Math.sqrt(distSq);
                double angle = Math.atan2(relZ, relX);

                double h_body = Double.NEGATIVE_INFINITY;
                double angleNoise = fbm1D(seed + 101L, angle * 2.5, 4) * 2 - 1;
                double rMod = 1.0 + angleNoise * 0.20;
                double effectiveRadius = RADIUS * rMod;

                if (dist <= effectiveRadius) {
                    double t = dist / effectiveRadius;
                    double profile = 1.0 - Math.pow(t, 3.5);
                    h_body = MAX_HEIGHT * profile;

                    double bigNoise  = fbm2D(seed + 211L, wx * 0.015, wz * 0.015, 4) * 2 - 1;
                    double midNoise  = fbm2D(seed + 307L, wx * 0.050, wz * 0.050, 3) * 2 - 1;
                    double fineNoise = fbm2D(seed + 419L, wx * 0.150, wz * 0.150, 2) * 2 - 1;
                    h_body += bigNoise  * 10.0;
                    h_body += midNoise  * 5.0;
                    h_body += fineNoise * 2.0;

                    double erosion = fbm2D(seed + 523L, wx * 0.020, wz * 0.020, 3);
                    if (erosion > 0.62
                            && dist > PLATEAU_RADIUS * 1.1
                            && dist > DOME_BASE_RADIUS) {
                        h_body -= (erosion - 0.62) * 55.0;
                    }

                    if (dist > PLATEAU_RADIUS * 1.15 && dist > DOME_BASE_RADIUS) {
                        double spike = fbm2D(seed + 631L, wx * 0.090, wz * 0.090, 2);
                        if (spike > 0.85) {
                            h_body += (spike - 0.85) * 30.0;
                        }
                    }

                    if (dist < PLATEAU_RADIUS) {
                        double dipT = dist / PLATEAU_RADIUS;
                        double dipAmount;
                        if (dipT < FLAT_RATIO) {
                            dipAmount = DIP_DEPTH;
                        } else {
                            double k = (dipT - FLAT_RATIO) / (1.0 - FLAT_RATIO);
                            dipAmount = DIP_DEPTH * (1.0 - k * k * (3.0 - 2.0 * k));
                        }
                        h_body -= dipAmount;
                        double pitNoise = fbm2D(seed + 733L, wx * 0.150, wz * 0.150, 2) * 2 - 1;
                        h_body += pitNoise * 1.2;
                    }
                }

                // ============ 山尖高度 ============
                double h_peak_bottom = Double.NEGATIVE_INFINITY;
                double h_peak_top = Double.NEGATIVE_INFINITY;

                if (dist < DOME_BASE_RADIUS) {
                    double k = dist / DOME_BASE_RADIUS;
                    h_peak_bottom = MAX_HEIGHT + DOME_BOTTOM_CLEARANCE * (1.0 - k);

                    double peakAdd = DOME_PEAK_HEIGHT * Math.exp(-DOME_SHARPNESS * k * k);
                    double surfNoise = fbm2D(seed + 811L, wx * 0.07, wz * 0.07, 3) * 2 - 1;
                    peakAdd += surfNoise * DOME_NOISE_AMP * k;
                    if (peakAdd < 0) peakAdd = 0;
                    h_peak_top = h_peak_bottom + peakAdd;
                }

                if (h_body == Double.NEGATIVE_INFINITY
                        && h_peak_top == Double.NEGATIVE_INFINITY) continue;

                int groundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, wx, wz);

                if (h_body != Double.NEGATIVE_INFINITY) {
                    int topY = groundY + (int) Math.round(h_body);
                    if (topY > groundY) {
                        if (topY > BUILD_LIMIT) topY = BUILD_LIMIT;
                        for (int y = groundY; y <= topY; y++) {
                            placeStone(level, wx, y, wz);
                        }
                    }
                }

                if (h_peak_top != Double.NEGATIVE_INFINITY) {
                    int peakBotY = groundY + (int) Math.round(h_peak_bottom);
                    int peakTopY = groundY + (int) Math.round(h_peak_top);
                    if (peakTopY > peakBotY) {
                        if (peakTopY > BUILD_LIMIT) peakTopY = BUILD_LIMIT;
                        if (peakBotY < groundY) peakBotY = groundY;
                        for (int y = peakBotY; y <= peakTopY; y++) {
                            placeStone(level, wx, y, wz);
                        }

                        // 穹顶底部（下表面）点缀荧石
                        double glow = fbm2D(seed + 917L,
                                wx * DOME_GLOW_FREQ, wz * DOME_GLOW_FREQ, 2);
                        if (glow > DOME_GLOW_THRESHOLD) {
                            BlockPos botPos = new BlockPos(wx, peakBotY, wz);
                            setBlock(level, botPos, Blocks.GLOWSTONE.defaultBlockState());
                        }
                    }
                }

                // ============ 顶层装饰 ============
                if (h_body != Double.NEGATIVE_INFINITY) {
                    int bodyTopY = groundY + (int) Math.round(h_body);
                    if (bodyTopY > SEA_LEVEL && bodyTopY < BUILD_LIMIT) {
                        BlockPos topPos = new BlockPos(wx, bodyTopY, wz);

                        if (dist < GRASS_RADIUS) {
                            setBlock(level, topPos, Blocks.GRASS_BLOCK.defaultBlockState());
                            for (int d = 1; d <= 4; d++) {
                                setBlock(level, topPos.below(d), Blocks.DIRT.defaultBlockState());
                            }
                        } else {
                            setBlock(level, topPos, Blocks.STONE.defaultBlockState());
                        }

                        if (dist < DOME_BASE_RADIUS) {
                            BlockPos lightPos = topPos.above();
                            BlockState existing = level.getBlockState(lightPos);
                            if (existing.isAir() || existing.canBeReplaced()) {
                                setBlock(level, lightPos, Blocks.LIGHT.defaultBlockState());
                            }
                        }
                    }
                }

                placedAny = true;
            }
        }

        return placedAny;
    }

    private void placeStone(WorldGenLevel level, int wx, int y, int wz) {
        BlockPos pos = new BlockPos(wx, y, wz);
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || existing.canBeReplaced()) {
            BlockState place = (y >= BUILD_LIMIT)
                    ? Blocks.BEDROCK.defaultBlockState()
                    : Blocks.STONE.defaultBlockState();
            setBlock(level, pos, place);
        }
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

    private static double hash1(long seed, int x) {
        return hash2(seed, x, 0x5F3759DF);
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

    private static double valueNoise1D(long seed, double x) {
        int xi = (int) Math.floor(x);
        double xf = x - xi;
        double u = smoothstep(xf);
        double a = hash1(seed, xi);
        double b = hash1(seed, xi + 1);
        return lerp(a, b, u);
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

    private static double fbm1D(long seed, double x, int octaves) {
        double sum = 0;
        double amp = 1;
        double freq = 1;
        double norm = 0;
        for (int i = 0; i < octaves; i++) {
            sum += valueNoise1D(seed + i * 7919L, x * freq) * amp;
            norm += amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return sum / norm;
    }
}