package com.dragoncore.optimize.asm.transformer;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * EntityAI 相关方法的异步化改造。
 *
 * <p>目标是把 {@code EntityAITasks#onUpdateTasks} 等较重的重计算方法调度到
 * {@code AsyncAIScheduler} 线程池上执行。该 transformer 保留框架，实际替换需按具体混淆映射定制。</p>
 */
public class EntityAITransformer implements IClassTransformer, Opcodes {

    private static final String TARGET_CLASS_DEV = "net.minecraft.entity.ai.EntityAITasks";
    private static final String TARGET_CLASS_OBF = "net.minecraft.entity.ai.EntityAITasks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (!TARGET_CLASS_DEV.equals(transformedName) && !TARGET_CLASS_OBF.equals(transformedName)) {
            return basicClass;
        }
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            new ClassReader(basicClass).accept(new ClassVisitor(ASM5, cw) {
                // 此处可在 visitMethod 中匹配 AI tick 逻辑并替换为异步提交。
            }, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[DragonOptimize] EntityAITransformer 失败：" + t);
            return basicClass;
        }
    }
}
