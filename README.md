# Create Aeronautics: Space

给《机械动力：航空学》(Create Aeronautics) 加一套**太空扩展**的 NeoForge 模组。

MC 1.21.1 / NeoForge 21.1.x

---

## 已实现

### 1. 高空失重（主世界）
- `y ≥ 1000`：重力随高度线性衰减
- `y ≥ 1400`：完全失重
- 物理飞行器在过渡带会越来越"轻"（通过 Sable 力组抵消重力）

### 2. 太空天空盒（真贴图）
- 越过太空线后，原版天空被**银河星空天球**盖住（等距柱状投影，贴到球面内壁）
- **太阳 / 月亮**圆盘（实拍照片裁圆 + 透明通道，朝向相机渲染）
- 雾色同步拉向深空黑

### 3. 10 颗可传送星球（独立维度）

| 星球 | 英文 | 相对重力 | 大气 |
|---|---|---|---|
| 月球 | Moon | 0.165 g | 真空 |
| 水星 | Mercury | 0.378 g | 真空 |
| 金星 | Venus | 0.904 g | 有（毒）|
| 火星 | Mars | 0.379 g | 稀薄 |
| 谷神星 | Ceres | 0.029 g | 真空 |
| 木星 | Jupiter | 2.528 g | 有 |
| 土星 | Saturn | 1.065 g | 有 |
| 天王星 | Uranus | 0.886 g | 有 |
| 海王星 | Neptune | 1.137 g | 有 |
| 冥王星 | Pluto | 0.063 g | 真空 |

- **重力**：每星球独立配置，由 Sable 的 `dimension_physics` 数据包驱动
- **天空**：无大气星球 = 星空；有大气星球 = 天顶→地平线渐变（每星球独立配色）

### 4. 氧气系统
- 不透气的星球上氧气持续消耗（满氧 15 秒）
- 氧气耗尽 → 周期性窒息伤害
- HUD 氧气条（仅在无氧环境显示）
- 创造 / 旁观模式免疫

### 5. 星际传送器（物品）
- 右键打开星球选择界面，显示每颗星球的重力与大气状态
- 点选 → 跨维度传送

---

## 构建

本机没有走 Gradle（提交内存被顶满，ModDevGradle 起不来），改用直接 `javac`：

```bash
python build.py
```

产出 `build/libs/aeronautics_space-0.1.0.jar`。

`build.py` 会从本机的 PCL 实例里读取 classpath：
- `libraries/net/minecraft/client/1.21.1/client-1.21.1-official.jar`
- `libraries/net/neoforged/neoforge/<ver>/*.jar`
- `libs/*.jar`（需自行放入 sable / aeronautics / create 等依赖）

> ⚠️ **classpath 顺序有讲究**：NeoForge 的 jar 必须排在 MC 官方 jar **之前**。
> NeoForge 的 `client.jar` 里是打过补丁的 MC 类（`Entity` 实现了 `AttachmentHolder`，
> 有 `getData/setData`）。顺序反了会报「找不到符号 getData」。

---

## 数据包结构

```
data/aeronautics_space/
├── dimension_type/<planet>.json    # 维度类型（天空效果、光照、高度）
├── dimension/<planet>.json         # 维度本体（地形生成器）
└── dimension_physics/<planet>.json # Sable 物理（重力 / 气压）
```

重力配置示例（`dimension_physics/moon.json`）：

```json
{
  "dimension": "aeronautics_space:moon",
  "priority": 100,
  "base_gravity": [0.0, -1.815, 0.0],
  "base_pressure": 0.0
}
```

> `base_gravity` 的单位基准是 Sable 的 `DEFAULT_GRAVITY = (0, -11.0, 0)`（地球）。
> 各星球按 `-11.0 × 相对重力` 计算。

---

## 素材署名

天空贴图取自 [Solar System Scope](https://www.solarsystemscope.com/textures/)，
授权 **CC BY 4.0**：

- `space_stars.png` ← `2k_stars_milky_way.jpg`
- `space_sun.png` ← `2k_sun.jpg`
- `space_moon.png` ← `2k_moon.jpg`

---

## 已知限制

- 星球地形目前用 `minecraft:flat` 生成器（平地），后续可换成噪声地形
- 星球天气（沙尘暴 / 极寒 / 风暴）尚未实现
- 飞行器**跨维度**搬运尚未打通（依赖 `SubLevelSchematicSerializationContext`）
- v0.1 未经游戏内实测（开发机提交内存不足，无法启动 MC），如崩溃请删掉 jar 并在 issue 反馈日志
