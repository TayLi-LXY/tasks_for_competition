# 任务1：一年内适合参赛的学科竞赛调研流程

基于 [rsi-flow-visual](https://github.com/ymxiong/rsi-flow-visual) 的可视化能力，设计「竞赛选型」第一条流程；数据依据：

- 《武汉大学本科生学科竞赛认定信息表（2026年）》
- 《2027届计算机学院免试攻读硕士学位研究生综合成绩计分办法》

> 说明：`rsi-flow-react` 仓库当前 404，不可用；本任务以 `rsi-flow-visual` 的 `ViewState` + rsi-flow JSON 流程描述交付。

## 如何查看流程

### 方式 A（推荐，立刻能看）

直接用浏览器打开：

`task1/competition-flow-preview.html`

（双击文件，或在资源管理器中右键用浏览器打开）

### 方式 B（官方 rsi-flow-visual demo）

必须用 **Node 18**（本机可用 `scoop reset versions/nodejs18`）。Parcel 1 依赖 `deasync`，在 Windows 上需先编译原生模块：

```bash
# 1) 切到 Node 18
scoop reset versions/nodejs18
node -v   # 应显示 v18.x

# 2) 安装依赖（跳过会失败的 postinstall）
cd rsi-flow-visual
npm install --ignore-scripts

# 3) 编译 deasync（只需一次）
cd node_modules/deasync
node ../../node_modules/npm/node_modules/node-gyp/bin/node-gyp.js rebuild
# 若上面路径不对，用 scoop 的 node-gyp：
# node "$env:USERPROFILE\scoop\apps\nodejs18\current\node_modules\npm\node_modules\node-gyp\bin\node-gyp.js" rebuild
mkdir bin\win32-x64-node-18
copy build\Release\deasync.node bin\win32-x64-node-18\
cd ../..

# 4) 启动
npx parcel demo/index.html --out-dir deploy --port 1234
```

浏览器打开：http://localhost:1234

相关文件：

| 文件 | 作用 |
|------|------|
| `CompetitionResearchViewState.ts` | 可视化流程 ViewState |
| `competition-research-flow.json` | 与 rsi-flow 对齐的流程定义 |
| `recommended-competitions.md` | 一年内推荐赛清单（2026-11 ~ 2027-11） |

## 流程设计（第一个流程）

目标：从「推免加分 / 能力训练 / 简历」出发，筛出一年内可报名且学院认可的赛事。

```
开始
  → 明确参赛目标（推免 / 能力 / 简历）
  → 读取认定表 + 推免计分办法
  → 过滤学院认可赛事（Ⅰ类 / Ⅱ类 A–D）
  → 按一年时间窗口过滤
  → 分支评估赛道投入
       ├─ Ⅰ类高价值（挑战杯 / 电设 / 数模 / 国创）
       ├─ 系统能力Ⅱ类A（系统能力大赛 / ASC / ICPC WF）
       ├─ 程序设计（ICPC/CCPC / 蓝桥杯）
       └─ 建模与创新（美赛 / 服务外包 / 计算机设计）
  → 综合成本与获奖预期
  → 输出主赛 + 备赛清单
结束
```

决策原则（来自计分办法）：

1. **Ⅰ类**国一及以上可直接学术原始分满分，优先评估能否冲击。
2. Ⅱ类按 A>B>C>D 分值递减；限三项代表奖；队长满分、队员 80%。
3. 同年同赛只取最高；不同周期次高 80%、第三 60%。
4. 蓝桥杯、司南杯、服务外包等有「国奖按省奖计」特例，选型时必须核对年级条款。

## 结论摘要

面向计算机学院、面向推免，**一年内优先冲击**：

1. **「挑战杯」大挑**（2027 奇数年）——Ⅰ类，校内申报窗口约在 12 月
2. **全国大学生电子设计竞赛**（2027 奇数年）——Ⅰ类
3. **全国大学生数学建模竞赛**（约 2027-09）——Ⅰ类
4. **全国大学生计算机系统能力大赛**（约 2027 春夏）——Ⅱ类 A/B/C
5. **ICPC / CCPC**——程序设计主赛道
6. **美赛 MCM/ICM**（约 2027-01/02）——认定表国家级，可作为建模备赛

详见 `recommended-competitions.md`。
