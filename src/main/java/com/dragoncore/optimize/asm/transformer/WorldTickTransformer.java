package com.dragoncore.optimize.asm.transformer;

import com.dragoncore.optimize.asm.TransformUtils;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class WorldTickTransformer implements IClassTransformer, Opcodes {

    private static final String SCHEDULER_OWNER = "com/dragoncore/optimize/scheduler/ParallelTickScheduler";
    private static final String BEGIN_DESC = "()V";
    private static final String AWAIT_DESC = "()V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        boolean isWorld = "net.minecraft.world.World".equals(transformedName);
        boolean isWorldServer = "net.minecraft.world.WorldServer".equals(transformedName);
        if (!isWorld && !isWorldServer) return basicClass;

        try {
            ClassNode cn = new ClassNode(ASM5);
            new ClassReader(basicClass).accept(cn, 0);

            boolean modified = false;
            for (MethodNode mn : cn.methods) {
                if (TransformUtils.isTickMethod(mn)) {
                    injectBegin(mn);
                    injectAwaitBeforeReturn(mn);
                    modified = true;
                }
            }

            if (!modified) return basicClass;

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[DragonOptimize] WorldTickTransformer failed: " + t);
            return basicClass;
        }
    }

    private void injectBegin(MethodNode mn) {
        InsnList inject = new InsnList();
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SCHEDULER_OWNER, "beginTickGlobal", BEGIN_DESC, false));
        mn.instructions.insertBefore(mn.instructions.getFirst(), inject);
    }

    private void injectAwaitBeforeReturn(MethodNode mn) {
        AbstractInsnNode[] insns = mn.instructions.toArray();
        for (AbstractInsnNode node : insns) {
            int op = node.getOpcode();
            if (op == RETURN || op == IRETURN || op == ARETURN
                    || op == LRETURN || op == FRETURN || op == DRETURN) {
                InsnList inject = new InsnList();
                inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SCHEDULER_OWNER, "awaitTickGlobal", AWAIT_DESC, false));
                mn.instructions.insertBefore(node, inject);
            }
        }
    }
}
