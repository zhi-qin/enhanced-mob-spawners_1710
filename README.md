# Enhanced Mob Spawners

**当前版本**: 1.0.0 for Minecraft 1.7.10 (Forge)

为 Minecraft 刷怪笼增加丰富的可配置功能，支持通过 GUI 调整刷怪参数、红石控制、刷怪次数限制等。

---

## 功能特性

- **Spawner Key** — 手持右键刷怪笼打开 GUI 配置界面
- **刷怪蛋提取** — 空手或非方块物品右键刷怪笼提取其中的刷怪蛋
- **刷怪蛋设置** — 手持刷怪蛋右键刷怪笼，设置其生成的实体类型
- **精准采集掉落** — 精准采集镐挖掘刷怪笼时掉落刷怪蛋（而非刷怪笼本身）
- **怪物掉落刷怪蛋** — 怪物被击杀时有概率掉落自身刷怪蛋（可配置概率）
- **红石控制** — 红石信号输入时禁用刷怪笼，信号移除后恢复
- **刷怪次数限制** — 限制每个刷怪笼的总生成次数，达到上限后永久关闭
- **自定义默认范围** — 为新生成的刷怪笼设置自定义玩家检测范围
- **自定义硬度** — 修改刷怪笼的挖掘硬度
- **GUI 配置** — 直观的图形界面调整刷怪数量、速度、开关等参数
- **`/ems` 命令** — 运行时查看/修改配置项，检查刷怪笼状态
- **调试伤害药水** — 测试用物品，投掷后对 80 格范围内造成最高 300 点魔法伤害

---

## 配置文件

路径：`config/spawnermod.json`

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `monster_egg_drop_chance` | int | 4 | 怪物掉落刷怪蛋的概率 (0~100%) |
| `disable_silk_touch` | int | 0 | 1=禁用精准采集掉落刷怪蛋 |
| `disable_spawner_config` | int | 0 | 1=禁用 Spawner Key 配置功能 |
| `disable_count` | int | 0 | 1=禁用 GUI 中 Count 按钮 |
| `disable_range` | int | 0 | 1=禁用 GUI 中 Range 按钮 |
| `disable_speed` | int | 0 | 1=禁用 GUI 中 Speed 按钮 |
| `limited_spawns_enabled` | int | 0 | 1=启用刷怪次数限制 |
| `limited_spawns_amount` | int | 32 | 刷怪次数上限 |
| `disable_egg_removal_from_spawner` | int | 0 | 1=禁止从刷怪笼提取刷怪蛋 |
| `monster_egg_only_drop_when_killed_by_player` | int | 0 | 1=仅玩家击杀才掉落刷怪蛋 |
| `default_spawner_range_enabled` | int | 0 | 1=启用自定义默认范围 |
| `default_spawner_range` | int | 52 | 刷怪笼默认玩家检测范围 |
| `spawner_hardness` | int | 5 | 刷怪笼挖掘硬度 |
| `display_item_id_from_right_click_in_log` | int | 0 | 1=右键时在日志输出物品 ID |

**特殊结构**:
- `item_id_blacklist` — 字符串数组，列出右键不会提取刷怪蛋的物品 ID
- `disable_specific_egg_drops` — 对象，key=实体名，value=0/1，禁用特定实体的蛋掉落

---

## 命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/ems check` | 检查玩家视线中的刷怪笼 NBT 数据 | OP 2 |
| `/ems reset` | 重置配置文件为默认值 | OP 2 |
| `/ems <key>` | 查看指定配置项的当前值 | OP 2 |
| `/ems <key> <value>` | 修改指定配置项的值 | OP 2 |

---

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/`。

---

## 致谢

- **Branders** — 原始作者
- **GTNH 社区** — Forge 1.7.10 构建工具链

原项目地址：https://github.com/andersblomqvist/enhanced-mob-spawners
