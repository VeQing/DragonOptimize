# DragonOptimize

面向 **Minecraft 1.12.2** / **Java 8** 的客户端性能优化模组，专为 **龙核 (DragonCore)** 服务器打造。

## 特性

1. **性能 / 平衡 / 品质 / 自定义** 四档预设，玩家可在游戏内通过 `O` 键面板自由切换。
2. **并行化世界 tick**：将实体、TileEntity、区块的单 tick 循环按批次分发至多线程池，
   解决 1.12.2 原生单核处理瓶颈。
3. **异步实体 AI**：把重计算类的 AI 决策从主线程迁移到低优先级线程池，仅在主线程回写状态。
4. **光照缓存合并**：合并相邻区块重复的 `propagateSkylightOcclusion` 请求。
5. **区块懒加载**：合并、延迟提交区块加载 IO 请求，减少卡顿抖动。
6. **龙核 (DragonCore) 兼容层**：自动检测龙核并按协议完成安全协商，避免反作弊误判。
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
3. 执行 `gradlew build`，产物位于 `build/libs/dragonoptimize-1.0.0.jar`。
4. 将产物放入客户端 `.minecraft/mods/`，配合 Java 8（OpenJDK / Oracle JDK 均可）启动。

## 使用

- 默认预设为 **平衡 (Balanced)**。
- 进入游戏后按 `O` 打开 DragonOptimize 面板，在四个预设间切换并保存。
- 也可以在 Minecraft Mod 列表里点击 "Config" 进入同样的面板。
- 保存后配置同时写入 `config/dragonoptimize.cfg` 与 `config/dragonoptimize.json`。

## 与龙核 (DragonCore) 的集成

`DragonCoreCompat` 通过反射检测并读取龙核版本信息，用于与服务器完成安全协商。
在实际项目中，请按龙核官方 SDK 替换其中的反射逻辑。如果您尚未接入龙核 SDK，
默认行为为 "仅识别，不降级"，模组仍可正常工作。

## 注意事项

- 模组自带 **FMLCorePlugin**，通过 ASM 对 `WorldServer#tick` 与相关 tick 循环进行改造。
- 若与其他也做并行化的优化模组（例如 FoamFix、Phosphor）同时加载，
  建议在配置中关闭对应项以避免冲突。
- 预设为 **品质优先** 时，`parallelEntityTick / parallelTileTick / parallelChunkTick` 会自动置 `false`，
  保持原版串行行为，避免任何行为差异影响视觉表现。
