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
 * 奥林帕斯诸峰。
 *
 * 底部更宽，且子峰排列被拉长为横向（长边面向主山）。
 */
public class DivineRangeFeature extends Feature<NoneFeatureConfiguration> {

    private static final int CENTER_X = 10000;
    private static final int CENTER_Z = 10000;

    private static final int MOUNTAIN_COUNT_MIN = 5;
    private static final int MOUNTAIN_COUNT_MAX = 12;

    private static final double RING_MIN = 200.0;
    private static final double RING_MAX = 400.0;

    // 拓宽底部半径
    private static final double MOUNTAIN_RADIUS_MIN = 40.0;
    private static final double MOUNTAIN_RADIUS_MAX = 80.0;

    // 适当拔高，避免变宽后变成土包
    private static final double MOUNTAIN_HEIGHT_MIN = 80.0;
    private static final double MOUNTAIN_HEIGHT_MAX = 150.0;

    private static final int SEA_LEVEL = 62;
    private static final int BUILD_LIMIT = 319;

    private static final double GRASS_RATIO = 0.9;

    public DivineRangeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        long seed = level.getSeed();

        int count = MOUNTAIN_COUNT_MIN
                + (int) (hashRange(seed + 1001L, 0, MOUNTAIN_COUNT_MAX - MOUNTAIN_COUNT_MIN + 1));

        // 最大 12 群 * 4 峰 = 48 峰
        double[] px = new double[60];
        double[] pz = new double[60];
        double[] pr = new double[60];
        double[] ph = new double[60];
        int totalPeaks = 0;

        double angleBase = hashRange(seed + 2003L, 0, 1) * Math.PI * 2.0;

        for (int i = 0; i < count; i++) {
            double angle = angleBase
                    + (Math.PI * 2.0) * i / count
                    + (hashRange(seed + 3001L, i, 10000) / 10000.0 - 0.5)
                    * (Math.PI * 2.0 / count) * 0.8;

            // 计算指向主山的径向向量和切向向量
            double radialX = Math.cos(angle);
            double radialZ = Math.sin(angle);
            double tangentX = -Math.sin(angle);
            double tangentZ = Math.cos(angle);

            double radius = RING_MIN
                    + hashRange(seed + 4001L, i, 10000) / 10000.0 * (RING_MAX - RING_MIN);
            double cx = CENTER_X + radialX * radius;
            double cz = CENTER_Z + radialZ * radius;

            double cr = MOUNTAIN_RADIUS_MIN
                    + hashRange(seed + 5003L, i, 10000) / 10000.0
                    * (MOUNTAIN_RADIUS_MAX - MOUNTAIN_RADIUS_MIN);
            double ch = MOUNTAIN_HEIGHT_MIN
                    + hashRange(seed + 6007L, i, 10000) / 10000.0
                    * (MOUNTAIN_HEIGHT_MAX - MOUNTAIN_HEIGHT_MIN);

            // 拆分为 3~4 个子峰，沿切向排列
            int subPeaks = 3 + (int)(hashRange(seed + 777L, i, 2));
            for (int j = 0; j < subPeaks; j++) {
                // 将子峰在一条垂直主山的线上铺开
                double spread = (j - (subPeaks - 1) / 2.0) * (cr * 0.9);

                // 在切向大幅偏移，在径向小幅抖动
                double offsetT = spread + (hashRange(seed + 888L, i * 10 + j, 10000) / 10000.0 - 0.5) * cr * 0.4;
                double offsetR = (hashRange(seed + 999L, i * 10 + j, 10000) / 10000.0 - 0.5) * cr * 0.6;

                px[totalPeaks] = cx + tangentX * offsetT + radialX * offsetR;
                pz[totalPeaks] = cz + tangentZ * offsetT + radialZ * offsetR;

                // 每个子峰的半径稍微变化，制造连绵感
                pr[totalPeaks] = cr * (0.5 + hashRange(seed + 1111L, i * 10 + j, 10000) / 10000.0 * 0.5);
                ph[totalPeaks] = ch * (0.7 + hashRange(seed + 2222L, i * 10 + j, 10000) / 10000.0 * 0.5);
                totalPeaks++;
            }
        }

        int chunkMinX = origin.getX() & ~15;
        int chunkMinZ = origin.getZ() & ~15;

        boolean placedAny = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int wx = chunkMinX + dx;
                int wz = chunkMinZ + dz;

                double bestH = 0;
                boolean inside = false;

                // 域扭曲：继续保持边缘的崎岖破碎
                double warpX = (fbm2D(seed + 71L, wx * 0.015, wz * 0.015, 3) * 2 - 1) * 50.0;
                double warpZ = (fbm2D(seed + 73L, wx * 0.015, wz * 0.015, 3) * 2 - 1) * 50.0;
                double swx = wx + warpX;
                double swz = wz + warpZ;

                for (int i = 0; i < totalPeaks; i++) {
                    double ddx = swx - px[i];
                    double ddz = swz - pz[i];
                    double d = Math.sqrt(ddx * ddx + ddz * ddz);
                    if (d > pr[i] * 1.6) continue;

                    double t = d / pr[i];
                    double profile = Math.pow(1.0 - t, 1.5);
                    if (profile <= 0) continue;

                    double edgeFade = Math.max(0.0, 1.0 - t * t);

                    double ridgeN = 1.0 - Math.abs(fbm2D(seed + 3333L + i, wx * 0.025, wz * 0.025, 3) * 2 - 1);
                    double midN = fbm2D(seed + 9001L + i, wx * 0.05, wz * 0.05, 2) * 2 - 1;

                    double h = ph[i] * profile;
                    // 脊状噪声制造崖壁
                    h += (ridgeN * 25.0 - 6.0 + midN * 10.0) * edgeFade;

                    if (h < 0) h = 0;

                    if (h > bestH) {
                        bestH = h;
                        inside = true;
                    }
                }

                if (!inside) continue;

                int groundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, wx, wz);
                int topY = groundY + (int) Math.round(bestH);
                if (topY <= groundY) continue;
                if (topY > BUILD_LIMIT) topY = BUILD_LIMIT;

                for (int y = groundY; y <= topY; y++) {
                    BlockPos pos = new BlockPos(wx, y, wz);
                    BlockState existing = level.getBlockState(pos);
                    if (existing.isAir() || existing.canBeReplaced()) {
                        BlockState place = (y >= BUILD_LIMIT)
                                ? Blocks.BEDROCK.defaultBlockState()
                                : Blocks.STONE.defaultBlockState();
                        setBlock(level, pos, place);
                    }
                }

                // 顶层装饰：根据原生距离决定草方块
                if (topY > SEA_LEVEL && topY < BUILD_LIMIT) {
                    BlockPos topPos = new BlockPos(wx, topY, wz);
                    boolean onGrass = false;
                    for (int i = 0; i < totalPeaks; i++) {
                        double ddx = wx - px[i];
                        double ddz = wz - pz[i];
                        double d = Math.sqrt(ddx * ddx + ddz * ddz);
                        if (d < pr[i] * GRASS_RATIO) {
                            onGrass = true;
                            break;
                        }
                    }
                    if (onGrass) {
                        setBlock(level, topPos, Blocks.GRASS_BLOCK.defaultBlockState());
                        for (int d = 1; d <= 4; d++) {
                            setBlock(level, topPos.below(d), Blocks.DIRT.defaultBlockState());
                        }
                    } else {
                        setBlock(level, topPos, Blocks.STONE.defaultBlockState());
                    }
                }

                placedAny = true;
            }
        }

        return placedAny;
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

    private static int hashRange(long seed, int index, int range) {
        return (int) (hash2(seed, index, index * 31 + 7) * range);
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