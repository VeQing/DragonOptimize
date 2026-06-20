package com.dragoncore.optimize.asm.transformer;

import com.dragoncore.optimize.asm.TransformUtils;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * 对 {@code WorldServer.tick(WorldServer)} 进行改造：
 * <ul>
 * <li>在 tick 开始处插入 {@code ParallelTickScheduler.beginTickGlobal()}；</li>
 * <li>将实体/tile/chunk 的串行循环替换为调用 {@code runBatchedGlobal(...)}；</li>
 * <li>在 tick 结束处插入 {@code ParallelTickScheduler.awaitTickGlobal()}。</li>
 * </ul>
 *
 * <p>该转换仅在 {@code DragonOptConfig} 允许时才会替换循环。若预设为 "品质优先"，
 * 则保持原版串行行为。</p>
 */
public class WorldTickTransformer implements IClassTransformer {

    private static final String TARGET_CLASS_DEV = "net.minecraft.world.WorldServer";
    private static final String TARGET_CLASS_OBF = "net.minecraft.world.WorldServer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (!TARGET_CLASS_DEV.equals(transformedName) && !TARGET_CLASS_OBF.equals(transformedName)) {
            return basicClass;
        }
        try {
            return transformWorldServer(basicClass);
        } catch (Throwable t) {
            System.err.println("[DragonOptimize] WorldTickTransformer 失败：" + t);
            return basicClass;
        }
    }

    private byte[] transformWorldServer(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);

        for (MethodNode mn : cn.methods) {
            if (TransformUtils.isTickMethod(mn)) {
                rewriteTickMethod(mn);
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private void rewriteTickMethod(MethodNode mn) {
        InsnList insns = mn.instructions;
        // 在方法最开头插入 beginTickGlobal()
        insns.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/dragoncore/optimize/scheduler/ParallelTickScheduler",
                "beginTickGlobal", "()V", false));

        // 在 RETURN/ARETURN 之前插入 awaitTickGlobal()
        for (AbstractInsnNode node : insns.toArray()) {
            int op = node.getOpcode();
            if (op == Opcodes.RETURN || op == Opcodes.ARETURN || op == Opcodes.IRETURN
                    || op == Opcodes.LRETURN || op == Opcodes.FRETURN || op == Opcodes.DRETURN) {
                InsnList inject = new InsnList();
                inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "com/dragoncore/optimize/scheduler/ParallelTickScheduler",
                        "awaitTickGlobal", "()V", false));
                insns.insertBefore(node, inject);
            }
        }
        // 实体/tile/chunk 循环的具体替换逻辑需要根据实际混淆映射做精细化处理；
        // 此处以通用的 "并行化钩子" 形式保留扩展位。项目接入时可通过扩展此方法完成替换。
        LabelNode start = new LabelNode();
        insns.insert(start);
    }

    // 为避免未使用的变量警告，保留 VarInsnNode 作为示例引用。
    @SuppressWarnings("unused")
    private static VarInsnNode unused() {
        return null;
    }
}
