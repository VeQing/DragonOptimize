# DragonOptimize

面向 **Minecraft 1.12.2** / **Java 8** 的客户端性能优化模组，专为 **龙核 (DragonCore)** 服务器打造。

## 特性

1. **性能 / 平衡 / 品质 / 自定义** 四档预设，玩家可在游戏内通过 `O` 键面板自由切换。
2. **主城渲染优化**：按 FPS、距离、同屏玩家数量裁剪远处玩家/时装渲染。
3. **低 FPS 保护**：主城掉帧时自动降低粒子和视距，减少活动委托/交易时的波动卡顿。
4. **并行化世界 tick 框架**：提供实体、TileEntity、区块辅助计算的分片调度。
5. **异步实体 AI 框架**：为后续路径/邻居搜索预处理提供低优先级线程池。
6. **光照缓存合并 + 区块懒加载**：减少重复请求和瞬时 IO 抖动。
7. **配置文件 + JSON 双输出**：`.cfg` 供 Forge 使用，`dragonoptimize.json` 供外部工具/脚本读取。

## 目录结构

```
DragonOptimize/
├── build.gradle                                 # ForgeGradle 构建脚本（请配套 1.12.2 工作区）
└── src/main/
    ├── java/com/dragoncore/optimize/
    │   ├── DragonOptimize.java                  # 模组主类
    │   ├── core/
    │   │   ├── DragonOptimizeCore.java          # 核心模块初始化
    │   │   └── CoreTickListener.java            # 桥接 Forge tick 事件
    │   ├── scheduler/
    │   │   ├── ParallelTickScheduler.java       # 并行 tick 调度器（关键）
    │   │   ├── AsyncAIScheduler.java            # 异步 AI 任务调度
    │   │   └── DragonOptExceptionHandler.java   # 异常频率抑制
    │   ├── config/
    │   │   ├── DragonOptConfig.java             # 全局配置 + 预设
    │   │   └── JsonConfigExporter.java          # 导出 JSON 快照
    │   ├── render/
    │   │   ├── LightingCache.java               # 光照缓存合并
    │   │   └── LazyChunkLoader.java             # 区块懒加载
    │   ├── dragoncore/
    │   │   └── DragonCoreCompat.java            # 龙核协议检测与协商
    │   ├── gui/
    │   │   ├── GuiEventHandler.java             # 热键注册与响应
    │   │   ├── DragonOptGuiMain.java            # 主面板（预设/调优）
    │   │   └── ModGuiFactory.java               # Forge Mod 列表配置入口
    │   └── asm/
    │       ├── DragonOptimizeLoadingPlugin.java # Core Mod 加载插件
    │       ├── TransformUtils.java              # ASM 工具方法
    │       └── transformer/
    │           ├── WorldTickTransformer.java    # WorldServer 改造
    │           ├── TileEntityTickTransformer.java
    │           └── EntityAITransformer.java
    └── resources/
        ├── mcmod.info
        └── META-INF/dragonoptimize_at.cfg       # access transformer
```

## 构建

1. 准备一个标准的 **Forge 1.12.2-14.23.5.2860** MDK 工作区。
2. 把本目录作为一个子项目放入工作区（或直接替换 `src`）。
3. 执行 `gradlew build`，产物位于 `build/libs/dragonoptimize-1.0.1.jar`。
4. 将产物放入客户端 `.minecraft/mods/`，配合 Java 8（OpenJDK / Oracle JDK 均可）启动。

## 使用

- 默认预设为 **平衡 (Balanced)**。
- 进入游戏后按 `O` 打开 DragonOptimize 面板，在四个预设间切换，也可以调整主城同屏玩家上限、玩家渲染距离和低 FPS 保护阈值。
- 也可以在 Minecraft Mod 列表里点击 "Config" 进入同样的面板。
- 保存后配置同时写入 `config/dragonoptimize.cfg` 与 `config/dragonoptimize.json`。

## 与龙核 (DragonCore) 的集成

`DragonCoreCompat` 通过 Forge 加载列表检测龙核客户端，默认行为为 "仅识别，不降级"。

## 注意事项

- 模组自带 **FMLCorePlugin**，目前只做浅注入，不强行替换原版实体 tick 循环。
- 若与其他也做并行化的优化模组（例如 FoamFix、Phosphor）同时加载，
  建议在配置中关闭对应项以避免冲突。
- 预设为 **品质优先** 时，`parallelEntityTick / parallelTileTick / parallelChunkTick` 会自动置 `false`，
  保持原版串行行为，避免任何行为差异影响视觉表现。
