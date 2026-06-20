package com.dragoncore.optimize.gui;

import com.dragoncore.optimize.config.DragonOptConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DragonOptimize 主面板：支持预设切换 + 基础数值调优。
 *
 * <p>提供四档预设按钮以及几个常用参数的直接调整（粒子上限、批大小等）。</p>
 */
public class DragonOptGuiMain extends GuiScreen {

    private GuiButton btnPerformance;
    private GuiButton btnBalanced;
    private GuiButton btnQuality;
    private GuiButton btnCustom;
    private GuiButton btnSave;
    private GuiButton btnClose;

    private GuiTextField fieldParticles;
    private GuiTextField fieldBatch;
    private GuiTextField fieldThreads;

    private final List<String> messages = new ArrayList<>();

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int cx = width / 2;
        int y = 40;
        int bw = 140;

        buttonList.clear();
        buttonList.add(btnPerformance = new GuiButton(1, cx - bw - 8, y, bw, 20, "性能优先"));
        buttonList.add(btnBalanced = new GuiButton(2, cx + 8, y, bw, 20, "平衡"));
        y += 28;
        buttonList.add(btnQuality = new GuiButton(3, cx - bw - 8, y, bw, 20, "品质优先"));
        buttonList.add(btnCustom = new GuiButton(4, cx + 8, y, bw, 20, "自定义"));
        y += 36;

        fieldParticles = new GuiTextField(10, fontRenderer, cx - 160, y, 120, 18);
        fieldParticles.setText(String.valueOf(DragonOptConfig.particleLimit));
        y += 26;
        fieldBatch = new GuiTextField(11, fontRenderer, cx - 160, y, 120, 18);
        fieldBatch.setText(String.valueOf(DragonOptConfig.tickBatchSize));
        y += 26;
        fieldThreads = new GuiTextField(12, fontRenderer, cx - 160, y, 120, 18);
        fieldThreads.setText(String.valueOf(DragonOptConfig.workerThreads));

        buttonList.add(btnSave = new GuiButton(100, cx - 80, y + 40, 80, 20, I18n.format("gui.dragonoptimize.save")));
        buttonList.add(btnClose = new GuiButton(101, cx + 8, y + 40, 80, 20, I18n.format("gui.cancel")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "DragonOptimize - " + DragonOptConfig.currentPreset().display(),
                width / 2, 15, 0xFFFFFF);

        drawString(fontRenderer, "粒子上限:", width / 2 - 230, 40 + 94, 0xCCCCCC);
        drawString(fontRenderer, "每批大小:", width / 2 - 230, 40 + 120, 0xCCCCCC);
        drawString(fontRenderer, "线程数(0=自动):", width / 2 - 230, 40 + 146, 0xCCCCCC);

        fieldParticles.drawTextBox();
        fieldBatch.drawTextBox();
        fieldThreads.drawTextBox();

        int yy = 40 + 180;
        for (String msg : messages) {
            drawCenteredString(fontRenderer, msg, width / 2, yy, 0xFFDD66);
            yy += 12;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        fieldParticles.textboxKeyTyped(typedChar, keyCode);
        fieldBatch.textboxKeyTyped(typedChar, keyCode);
        fieldThreads.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        fieldParticles.mouseClicked(mouseX, mouseY, mouseButton);
        fieldBatch.mouseClicked(mouseX, mouseY, mouseButton);
        fieldThreads.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        messages.clear();
        switch (button.id) {
            case 1:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.PERFORMANCE);
                messages.add("已切换到【性能优先】");
                break;
            case 2:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.BALANCED);
                messages.add("已切换到【平衡】");
                break;
            case 3:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.QUALITY);
                messages.add("已切换到【品质优先】");
                break;
            case 4:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.CUSTOM);
                messages.add("已进入【自定义】模式，请调整下方数值。");
                break;
            case 100:
                DragonOptConfig.particleLimit = parseInt(fieldParticles.getText(), DragonOptConfig.particleLimit, 0, 65535);
                DragonOptConfig.tickBatchSize = parseInt(fieldBatch.getText(), DragonOptConfig.tickBatchSize, 8, 1024);
                DragonOptConfig.workerThreads = parseInt(fieldThreads.getText(), DragonOptConfig.workerThreads, 0, 32);
                DragonOptConfig.save();
                messages.add("已保存配置并写入配置文件。");
                break;
            case 101:
                Minecraft.getMinecraft().displayGuiScreen(null);
                return;
            default:
                break;
        }
    }

    private static int parseInt(String s, int fallback, int min, int max) {
        try {
            int v = Integer.parseInt(s.trim());
            if (v < min) return min;
            if (v > max) return max;
            return v;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}
