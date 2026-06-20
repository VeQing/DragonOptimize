package com.dragoncore.optimize.asm;

import org.objectweb.asm.tree.MethodNode;

public final class TransformUtils {

    private TransformUtils() {
    }

    public static boolean isTickMethod(MethodNode mn) {
        if (mn == null || mn.name == null) return false;
        String name = mn.name;
        return name.equals("tick") || name.equals("func_72939_s")
                || name.equals("updateEntities") || name.equals("func_72866_a");
    }

    public static boolean looksLikeAITick(MethodNode mn) {
        if (mn == null || mn.name == null) return false;
        String name = mn.name;
        return name.equals("onUpdateTasks") || name.equals("func_75774_a")
                || name.equals("updateTasks") || name.equals("tick") || name.equals("func_75770_a");
    }
}
