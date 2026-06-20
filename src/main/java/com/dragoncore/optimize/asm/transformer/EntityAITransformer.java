package com.dragoncore.optimize.asm.transformer;

import com.dragoncore.optimize.asm.TransformUtils;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class EntityAITransformer implements IClassTransformer, Opcodes {

    private static final String ASYNC_OWNER = "com/dragoncore/optimize/scheduler/AsyncAIScheduler";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (!"net.minecraft.entity.ai.EntityAITasks".equals(transformedName)) {
            return basicClass;
        }
        try {
            ClassNode cn = new ClassNode(ASM5);
            new ClassReader(basicClass).accept(cn, 0);
            boolean modified = false;
            for (MethodNode mn : cn.methods) {
                if (TransformUtils.looksLikeAITick(mn)) {
                    InsnList inject = new InsnList();
                    inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASYNC_OWNER, "onAIMethodEntered", "()V", false));
                    mn.instructions.insertBefore(mn.instructions.getFirst(), inject);
                    modified = true;
                }
            }
            if (!modified) return basicClass;
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[DragonOptimize] EntityAITransformer failed: " + t);
            return basicClass;
        }
    }
}
