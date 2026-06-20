package com.dragoncore.optimize.gui;

import com.dragoncore.optimize.config.DragonOptConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DragonOptGuiMain extends GuiScreen {

    private static final int BTN_PERF = 1;
    private static final int BTN_BAL = 2;
    private static final int BTN_QUALITY = 3;
    private static final int BTN_CUSTOM = 4;
    private static final int BTN_CITY = 5;
    private static final int BTN_SAVE = 100;
    private static final int BTN_CANCEL = 101;

    private GuiTextField fieldParticles;
    private GuiTextField fieldBatch;
    private GuiTextField fieldThreads;
    private GuiTextField fieldCityPlayers;
    private GuiTextField fieldCityDistance;
    private GuiTextField fieldCityLowFps;
    private final List<String> messages = new ArrayList<>();

    @Override
    public void initGui() {
        buttonList.clear();
        int cx = width / 2;
        int y = 40;
        int bw = 140;

        buttonList.add(new GuiButton(BTN_PERF, cx - bw - 8, y, bw, 20, "性能优先"));
        buttonList.add(new GuiButton(BTN_BAL, cx + 8, y, bw, 20, "平衡"));
        y += 28;
        buttonList.add(new GuiButton(BTN_QUALITY, cx - bw - 8, y, bw, 20, "品质优先"));
        buttonList.add(new GuiButton(BTN_CUSTOM, cx + 8, y, bw, 20, "自定义"));
        y += 28;
        buttonList.add(new GuiButton(BTN_CITY, cx - bw - 8, y, bw * 2 + 16, 20,
                DragonOptConfig.cityRenderOptimizer ? "主城优化：开启" : "主城优化：关闭"));

        y += 40;
        fieldParticles = new GuiTextField(10, fontRenderer, cx - 120, y, 120, 18);
        fieldParticles.setText(String.valueOf(DragonOptConfig.particleLimit));
        fieldParticles.setFocused(true);

        fieldBatch = new GuiTextField(11, fontRenderer, cx - 120, y + 26, 120, 18);
        fieldBatch.setText(String.valueOf(DragonOptConfig.tickBatchSize));

        fieldThreads = new GuiTextField(12, fontRenderer, cx - 120, y + 52, 120, 18);
        fieldThreads.setText(String.valueOf(DragonOptConfig.workerThreads));

        fieldCityPlayers = new GuiTextField(13, fontRenderer, cx - 120, y + 78, 120, 18);
        fieldCityPlayers.setText(String.valueOf(DragonOptConfig.cityMaxRenderedPlayers));

        fieldCityDistance = new GuiTextField(14, fontRenderer, cx - 120, y + 104, 120, 18);
        fieldCityDistance.setText(String.valueOf(DragonOptConfig.cityPlayerFarDistance));

        fieldCityLowFps = new GuiTextField(15, fontRenderer, cx - 120, y + 130, 120, 18);
        fieldCityLowFps.setText(String.valueOf(DragonOptConfig.cityLowFpsThreshold));

        buttonList.add(new GuiButton(BTN_SAVE, cx - 90, y + 166, 80, 20, "保存并关闭"));
        buttonList.add(new GuiButton(BTN_CANCEL, cx + 10, y + 166, 80, 20, "取消"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "DragonOptimize - " + DragonOptConfig.currentPreset().display(),
                width / 2, 12, 0xFFFFFF);

        int cx = width / 2;
        int y = 80;
        drawString(fontRenderer, "粒子上限 (0-65535):", cx - 290, y + 2, 0xCCCCCC);
        drawString(fontRenderer, "每批大小 (8-1024):", cx - 290, y + 28, 0xCCCCCC);
        drawString(fontRenderer, "工作线程数 (0=auto, max 32):", cx - 290, y + 54, 0xCCCCCC);
        drawString(fontRenderer, "主城同屏玩家上限:", cx - 290, y + 80, 0xCCCCCC);
        drawString(fontRenderer, "主城玩家渲染距离:", cx - 290, y + 106, 0xCCCCCC);
        drawString(fontRenderer, "低 FPS 保护阈值:", cx - 290, y + 132, 0xCCCCCC);

        fieldParticles.drawTextBox();
        fieldBatch.drawTextBox();
        fieldThreads.drawTextBox();
        fieldCityPlayers.drawTextBox();
        fieldCityDistance.drawTextBox();
        fieldCityLowFps.drawTextBox();

        int yy = y + 195;
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
        fieldCityPlayers.textboxKeyTyped(typedChar, keyCode);
        fieldCityDistance.textboxKeyTyped(typedChar, keyCode);
        fieldCityLowFps.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        fieldParticles.mouseClicked(mouseX, mouseY, mouseButton);
        fieldBatch.mouseClicked(mouseX, mouseY, mouseButton);
        fieldThreads.mouseClicked(mouseX, mouseY, mouseButton);
        fieldCityPlayers.mouseClicked(mouseX, mouseY, mouseButton);
        fieldCityDistance.mouseClicked(mouseX, mouseY, mouseButton);
        fieldCityLowFps.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        messages.clear();
        switch (button.id) {
            case BTN_PERF:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.PERFORMANCE);
                messages.add("已切换到性能优先。记得点击保存并关闭写入配置。");
                syncFieldsFromConfig();
                break;
            case BTN_BAL:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.BALANCED);
                messages.add("已切换到平衡。");
                syncFieldsFromConfig();
                break;
            case BTN_QUALITY:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.QUALITY);
                messages.add("已切换到品质优先。");
                syncFieldsFromConfig();
                break;
            case BTN_CUSTOM:
                DragonOptConfig.applyPreset(DragonOptConfig.Preset.CUSTOM);
                messages.add("已进入自定义，请修改下方数值后保存。");
                break;
            case BTN_CITY:
                DragonOptConfig.cityRenderOptimizer = !DragonOptConfig.cityRenderOptimizer;
                button.displayString = DragonOptConfig.cityRenderOptimizer ? "主城优化：开启" : "主城优化：关闭";
                messages.add(DragonOptConfig.cityRenderOptimizer ? "主城优化已开启。" : "主城优化已关闭。");
                break;
            case BTN_SAVE:
                DragonOptConfig.particleLimit = parseInt(fieldParticles.getText(), DragonOptConfig.particleLimit, 0, 65535);
                DragonOptConfig.tickBatchSize = parseInt(fieldBatch.getText(), DragonOptConfig.tickBatchSize, 8, 1024);
                DragonOptConfig.workerThreads = parseInt(fieldThreads.getText(), DragonOptConfig.workerThreads, 0, 32);
                DragonOptConfig.cityMaxRenderedPlayers = parseInt(fieldCityPlayers.getText(), DragonOptConfig.cityMaxRenderedPlayers, 1, 256);
                DragonOptConfig.cityPlayerFarDistance = parseInt(fieldCityDistance.getText(), DragonOptConfig.cityPlayerFarDistance, 8, 256);
                DragonOptConfig.cityLowFpsThreshold = parseInt(fieldCityLowFps.getText(), DragonOptConfig.cityLowFpsThreshold, 10, 240);
                DragonOptConfig.save();
                messages.add("配置已保存。下次启动仍会生效。");
            case BTN_CANCEL:
                if (button.id == BTN_CANCEL) {
                    messages.add("已取消更改。");
                }
                closeScreen();
                break;
        }
    }

    private void syncFieldsFromConfig() {
        fieldParticles.setText(String.valueOf(DragonOptConfig.particleLimit));
        fieldBatch.setText(String.valueOf(DragonOptConfig.tickBatchSize));
        fieldThreads.setText(String.valueOf(DragonOptConfig.workerThreads));
        fieldCityPlayers.setText(String.valueOf(DragonOptConfig.cityMaxRenderedPlayers));
        fieldCityDistance.setText(String.valueOf(DragonOptConfig.cityPlayerFarDistance));
        fieldCityLowFps.setText(String.valueOf(DragonOptConfig.cityLowFpsThreshold));
    }

    private static int parseInt(String s, int fallback, int min, int max) {
        try {
            int v = Integer.parseInt(s.trim());
            return Math.min(max, Math.max(min, v));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void closeScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
