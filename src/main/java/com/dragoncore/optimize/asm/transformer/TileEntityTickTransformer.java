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

public class TileEntityTickTransformer implements IClassTransformer, Opcodes {

    private static final String SCHEDULER_OWNER = "com/dragoncore/optimize/scheduler/ParallelTickScheduler";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (!"net.minecraft.world.World".equals(transformedName)
                && !"net.minecraft.world.WorldServer".equals(transformedName)) {
            return basicClass;
        }
        try {
            ClassNode cn = new ClassNode(ASM5);
            new ClassReader(basicClass).accept(cn, 0);
            boolean modified = false;
            for (MethodNode mn : cn.methods) {
                if (TransformUtils.isTickMethod(mn)) {
                    AbstractInsnNode first = mn.instructions.getFirst();
                    InsnList begin = new InsnList();
                    begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SCHEDULER_OWNER, "beginTickGlobal", "()V", false));
                    mn.instructions.insertBefore(first, begin);

                    AbstractInsnNode[] arr = mn.instructions.toArray();
                    for (AbstractInsnNode node : arr) {
                        int op = node.getOpcode();
                        if (op == RETURN || op == IRETURN || op == ARETURN
                                || op == LRETURN || op == FRETURN || op == DRETURN) {
                            InsnList inject = new InsnList();
                            inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SCHEDULER_OWNER, "awaitTickGlobal", "()V", false));
                            mn.instructions.insertBefore(node, inject);
                        }
                    }
                    modified = true;
                }
            }
            if (!modified) return basicClass;
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[DragonOptimize] TileEntityTickTransformer failed: " + t);
            return basicClass;
        }
    }
}
