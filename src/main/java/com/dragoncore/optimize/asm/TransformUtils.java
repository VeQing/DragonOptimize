package com.dragoncore.optimize.asm;

import org.objectweb.asm.tree.MethodNode;

/**
 * ASM transformer 的辅助工具类。
 */
public final class TransformUtils {

    private TransformUtils() {
    }

    /**
     * 判断一个方法是否是 world tick 的主循环方法。
     *
     * <p>不同版本的 Forge/MCP 名称略有差异，这里做通用匹配：</p>
     * <ul>
     * <li>{@code tick} / {@code func_72898_tickEntities}</li>
     * <li>{@code updateEntities} / {@code func_72898_tickEntities}</li>
     * </ul>
     */
    public static boolean isTickMethod(MethodNode mn) {
        if (mn == null || mn.name == null) {
            return false;
        }
        String name = mn.name;
        return name.equals("tick")
                || name.equals("updateEntities")
                || name.startsWith("func_728")
                || name.startsWith("func_729");
    }

    /**
     * 判断是否是实体 onUpdate 方法。
     */
    public static boolean isEntityUpdate(MethodNode mn) {
        if (mn == null || mn.name == null) {
            return false;
        }
        String name = mn.name;
        return name.equals("onUpdate")
                || name.equals("onEntityUpdate")
                || name.startsWith("func_70071_");
    }
}
