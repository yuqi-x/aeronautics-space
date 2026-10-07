package com.yuqi.aeronauticsspace;

/**
 * 大气模型：定义"太空线"以及重力随高度的衰减曲线。
 */
public final class SpaceAtmosphere {

    /** 太空线。低于此高度 = 正常世界。 */
    public static final double SPACE_LINE = 1000.0;

    /** 过渡带厚度：从 SPACE_LINE 到 SPACE_LINE+TRANSITION 之间，重力线性衰减到 0。 */
    public static final double TRANSITION = 400.0;

    /** 完全失重的高度。 */
    public static final double FULL_SPACE_LINE = SPACE_LINE + TRANSITION;

    private SpaceAtmosphere() {
    }

    /**
     * @return 0.0 = 完全正常重力，1.0 = 完全失重。中间线性过渡。
     */
    public static double spaceFactor(double y) {
        if (y <= SPACE_LINE) {
            return 0.0;
        }
        if (y >= FULL_SPACE_LINE) {
            return 1.0;
        }
        return (y - SPACE_LINE) / TRANSITION;
    }

    /** 是否已经进入太空状态（用于客户端天空渲染判断）。 */
    public static boolean isInSpace(double y) {
        return y >= SPACE_LINE;
    }
}
