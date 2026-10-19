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
 * 底部宽大且通过二维域扭曲打破标准圆形，山体挺拔。
 * 内部包含平整战斗平台，顶部穹顶崎岖。
 */
public class OlympusMonsFeature extends Feature<NoneFeatureConfiguration> {

    private static final int CENTER_X = 10000;
    private static final int CENTER_Z = 10000;

    // 恢复宽大的底部半径，甚至更宽
    private static final int RADIUS = 150;
    private static final double RADIUS_SQ = (double) RADIUS * RADIUS;

    private static final double PLATEAU_RADIUS = 50.0;
    private static final double DIP_DEPTH = 20.0;
    private static final double FLAT_RATIO = 0.6;

    // 保持较高高度，维持挺拔感
    private static final int MAX_HEIGHT = 160;

    private static final int SEA_LEVEL = 62;
    private static final double GRASS_RADIUS = PLATEAU_RADIUS * 0.85;

    private static final int BUILD_LIMIT = 319;

    private static final double DOME_BASE_RADIUS = 50.0;
    private static final double DOME_PEAK_HEIGHT = 80.0;
    private static final double DOME_NOISE_AMP = 6.0;
    private static final int DOME_BOTTOM_CLEARANCE = 20;
    private static final double DOME_SHARPNESS = 4.0;

    private static final double DOME_GLOW_THRESHOLD = 0.78;
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

                // 使用域扭曲，把坐标在一个大范围内扯歪
                double warpX = (fbm2D(seed + 71L, wx * 0.012, wz * 0.012, 3) * 2 - 1) * 30.0;
                double warpZ = (fbm2D(seed + 73L, wx * 0.012, wz * 0.012, 3) * 2 - 1) * 30.0;
                double swx = wx + warpX;
                double swz = wz + warpZ;

                double relX = swx - CENTER_X;
                double relZ = swz - CENTER_Z;
                double distSq = relX * relX + relZ * relZ;
                // 判定范围稍微放大以容纳扭曲
                if (distSq > RADIUS_SQ * 1.5) continue;

                double dist = Math.sqrt(distSq);

                double h_body = Double.NEGATIVE_INFINITY;

                // 辅助边缘噪声，让边界更毛糙
                double edgeNoise = fbm2D(seed + 101L, wx * 0.025, wz * 0.025, 3) * 2 - 1;
                double rMod = 1.0 + edgeNoise * 0.1;
                double effectiveRadius = RADIUS * rMod;

                if (dist <= effectiveRadius) {
                    double t = dist / effectiveRadius;
                    // 剖面曲线 2.5 次方，保证中下部有一定体积感，但也向中心收拢
                    double profile = 1.0 - Math.pow(t, 2.5);
                    h_body = MAX_HEIGHT * profile;

                    double bigNoise  = fbm2D(seed + 211L, wx * 0.015, wz * 0.015, 4) * 2 - 1;
                    double midNoise  = fbm2D(seed + 307L, wx * 0.045, wz * 0.045, 3) * 2 - 1;
                    double fineNoise = fbm2D(seed + 419L, wx * 0.150, wz * 0.150, 2) * 2 - 1;
                    h_body += bigNoise  * 15.0;
                    h_body += midNoise  * 7.0;
                    h_body += fineNoise * 2.5;

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

                        // 平台表面相对平整
                        double pitNoise = fbm2D(seed + 733L, wx * 0.100, wz * 0.100, 2) * 2 - 1;
                        h_body += pitNoise * 0.8;
                    }
                }

                // ============ 山尖高度 ============
                double h_peak_bottom = Double.NEGATIVE_INFINITY;
                double h_peak_top = Double.NEGATIVE_INFINITY;

                // 穹顶本身不进行坐标扭曲，确保中心战斗场地的平整
                double realRelX = wx - CENTER_X;
                double realRelZ = wz - CENTER_Z;
                double realDist = Math.sqrt(realRelX * realRelX + realRelZ * realRelZ);

                if (realDist < DOME_BASE_RADIUS) {
                    double k = realDist / DOME_BASE_RADIUS;
                    h_peak_bottom = MAX_HEIGHT + DOME_BOTTOM_CLEARANCE * (1.0 - k);

                    double peakAdd = DOME_PEAK_HEIGHT * Math.exp(-DOME_SHARPNESS * k * k);

                    double ridgeNoise = 1.0 - Math.abs(fbm2D(seed + 811L, wx * 0.04, wz * 0.04, 3) * 2 - 1);
                    double noiseFade = Math.sin(k * Math.PI);

                    peakAdd += (ridgeNoise - 0.5) * DOME_NOISE_AMP * noiseFade;

                    double fineNoise = fbm2D(seed + 919L, wx * 0.15, wz * 0.15, 2) * 2 - 1;
                    peakAdd += fineNoise * 1.5 * noiseFade;

                    if (peakAdd < 0) peakAdd = 0;
                    h_peak_top = h_peak_bottom + peakAdd;
                }

                if (h_body == Double.NEGATIVE_INFINITY
                        && h_peak_top == Double.NEGATIVE_INFINITY) continue;

                int groundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, wx, wz);

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

                        if (realDist < GRASS_RADIUS) {
                            setBlock(level, topPos, Blocks.GRASS_BLOCK.defaultBlockState());
                            for (int d = 1; d <= 4; d++) {
                                setBlock(level, topPos.below(d), Blocks.DIRT.defaultBlockState());
                            }
                        } else {
                            setBlock(level, topPos, Blocks.STONE.defaultBlockState());
                        }

                        if (realDist < DOME_BASE_RADIUS) {
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