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
 * 奥林帕斯诸峰。环绕奥林匹斯山（10000, 10000）的一圈小型锥形山峰。
 *
 * 每座山的位置、大小、高度全部由世界种子派生，跨区块一致。
 * 数量 5~12；分布半径 140~250；山体半径 20~60；高度 地面 +50 到 地面 +93。
 *
 * 形状：锥形（无坑、无穹顶），轮廓由值噪声扰动。
 */
public class DivineRangeFeature extends Feature<NoneFeatureConfiguration> {

    private static final int CENTER_X = 10000;
    private static final int CENTER_Z = 10000;

    private static final int MOUNTAIN_COUNT_MIN = 5;
    private static final int MOUNTAIN_COUNT_MAX = 12;

    private static final double RING_MIN = 140.0;
    private static final double RING_MAX = 250.0;

    private static final double MOUNTAIN_RADIUS_MIN = 20.0;
    private static final double MOUNTAIN_RADIUS_MAX = 60.0;

    private static final double MOUNTAIN_HEIGHT_MIN = 50.0;
    private static final double MOUNTAIN_HEIGHT_MAX = 93.0;

    private static final int SEA_LEVEL = 62;
    private static final int BUILD_LIMIT = 319;

    /** 铺草比例（占山体半径的比例） */
    private static final double GRASS_RATIO = 0.9;

    public DivineRangeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        long seed = level.getSeed();

        // 从种子派生这一世界的山峰分布
        int count = MOUNTAIN_COUNT_MIN
                + (int) (hashRange(seed + 1001L, 0, MOUNTAIN_COUNT_MAX - MOUNTAIN_COUNT_MIN + 1));

        double[] cx = new double[count];
        double[] cz = new double[count];
        double[] cr = new double[count];
        double[] ch = new double[count];

        double angleBase = hashRange(seed + 2003L, 0, 1) * Math.PI * 2.0;

        for (int i = 0; i < count; i++) {
            // 角度均分 + 抖动
            double angle = angleBase
                    + (Math.PI * 2.0) * i / count
                    + (hashRange(seed + 3001L, i, 10000) / 10000.0 - 0.5)
                    * (Math.PI * 2.0 / count) * 0.8;
            double radius = RING_MIN
                    + hashRange(seed + 4001L, i, 10000) / 10000.0 * (RING_MAX - RING_MIN);
            cx[i] = CENTER_X + Math.cos(angle) * radius;
            cz[i] = CENTER_Z + Math.sin(angle) * radius;
            cr[i] = MOUNTAIN_RADIUS_MIN
                    + hashRange(seed + 5003L, i, 10000) / 10000.0
                    * (MOUNTAIN_RADIUS_MAX - MOUNTAIN_RADIUS_MIN);
            ch[i] = MOUNTAIN_HEIGHT_MIN
                    + hashRange(seed + 6007L, i, 10000) / 10000.0
                    * (MOUNTAIN_HEIGHT_MAX - MOUNTAIN_HEIGHT_MIN);
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

                for (int i = 0; i < count; i++) {
                    double ddx = wx - cx[i];
                    double ddz = wz - cz[i];
                    double d = Math.sqrt(ddx * ddx + ddz * ddz);
                    if (d > cr[i] * 1.6) continue;

                    // 方向扰动：让山的边界不规则
                    double mAngle = Math.atan2(ddz, ddx);
                    double rMod = 1.0 + (fbm1D(seed + 7001L + i, mAngle * 2.0, 3) * 2 - 1) * 0.15;
                    double effectiveR = cr[i] * rMod;
                    if (d > effectiveR) continue;

                    double t = d / effectiveR;
                    double profile = 1.0 - Math.pow(t, 2.5);
                    double h = ch[i] * profile;

                    // 起伏
                    double bigN = fbm2D(seed + 8009L + i, wx * 0.02, wz * 0.02, 3) * 2 - 1;
                    double midN = fbm2D(seed + 9001L + i, wx * 0.06, wz * 0.06, 2) * 2 - 1;
                    h += bigN * 6.0 + midN * 2.5;

                    if (h > bestH) {
                        bestH = h;
                        inside = true;
                    }
                }

                if (!inside) continue;

                int groundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, wx, wz);
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

                // 顶层装饰
                if (topY > SEA_LEVEL && topY < BUILD_LIMIT) {
                    BlockPos topPos = new BlockPos(wx, topY, wz);
                    boolean onGrass = false;
                    for (int i = 0; i < count; i++) {
                        double ddx = wx - cx[i];
                        double ddz = wz - cz[i];
                        double d = Math.sqrt(ddx * ddx + ddz * ddz);
                        if (d < cr[i] * GRASS_RATIO) {
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

    private static double hash1(long seed, int x) {
        return hash2(seed, x, 0x5F3759DF);
    }

    /** 返回 [0, range) 的整数随机。用于派生山峰参数。 */
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