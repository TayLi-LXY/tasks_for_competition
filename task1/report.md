# 任务1：一年内竞赛调研选型流程

可视化：rsi-flow-visual · 执行：rsi-flow · 数据：认定表调研 → catalog.json · LLM：WHU DeepSeek

## 任务目标

在 `rsi-flow-visual` 上设计第一条流程，调研一年内（约 2026-11 ~ 2027-11）适合计院同学参加的比赛；流程可真正跑通，输出主赛 / 备赛 / 解释。

> `rsi-flow-react` 404，落在 visual + flow 基座。

## 入口：结构化画像（非 goal 下拉）

| 字段 | 取值 |
|------|------|
| grade | 大一 / 大二 / 大三 / 大四 |
| motive | 冲推免 / 练能力 / 补简历 |
| skills | 算法、系统底层、硬件嵌入式、建模写作、项目产品（多选） |
| hours | 5以下 / 5-10 / 10-20 / 20以上 |
| note | 可选备注 |

## 流程与判断逻辑

`flowId = FLOW_COMPETITION_RESEARCH`

```
开始 → BUILD_PROFILE（画像→内部 goalType）
     → LOAD_RULES（读 catalog.json）
     → 过滤学院认可 → 一年时间窗
     → 四赛道并行评估（all / close）
     → RANK_OUTPUT（规则保底 rulePrimary / ruleBackup）
     → LLM_ADJUST（白名单内可改名单 + 解释）
结束
```

**规则保底（确定性）**

1. motive/skills 映射 `goalType`（推免 / 系统 / 算法 / 建模 / 综合）
2. catalog 分赛道候选 + 基础 score
3. 画像加权：技能匹配赛道加分；冲推免抬Ⅰ类；低投入压高成本赛；低年级略抬练手赛
4. 取主赛 2、备赛 3 → `rulePrimary` / `ruleBackup`

**LLM 调整（可选）**

- 输入：画像 JSON、goalType、规则保底名单、catalog 允许列表（name/track/level/score）
- 输出：`{"primary":[],"backup":[],"explanation":"..."}`（primary≤2，backup≤3，必须在白名单）
- 校验失败或无 Key → 保留规则名单 + 规则解释；`adjusted` 标记是否相对保底有改动

原则：**规则决定能不能选稳；LLM 在范围内微调并说明为什么。**

## 与 rsi-flow 基座的关系

| 角色 | 谁做 |
|------|------|
| 编排 / all·close / 上下文 register | rsi-flow 引擎 |
| 业务节点 `@Flow(COMP_*)` | `FlowCompetitionCode` |
| 赛历数据 | `competition/catalog.json`（调研外置，非代码常量） |
| 画布展示 | rsi-flow-visual ViewState |
| HTTP 入口 | `CompetitionApiServer` :8088 |

基座不认识「竞赛」；本任务是在其上挂一条可执行业务流。

## 实现层

| 层 | 路径 / 内容 |
|----|-------------|
| 流程 JSON | `flow-test/.../FLOW_COMPETITION_RESEARCH/flow-competition-research.json` |
| 节点代码 | `FlowCompetitionCode`（BUILD_PROFILE / RANK / LLM_ADJUST） |
| LLM 客户端 | `CompetitionLlmClient` → `maas.whu.edu.cn` · `deepseek-v4-flash` |
| UI | `rsi-flow-visual/demo/index.tsx` 右侧表单 + 保底对比 |
| 调研全文 | `recommended-competitions.md` |

## 怎么跑

```powershell
.\scripts\start-competition-api.ps1

scoop reset versions/nodejs18
cd rsi-flow-visual
npx parcel demo/index.html --out-dir deploy --port 1234
```

打开 http://localhost:1234 → 填画像 → 运行。

## 验证

- `FlowCompetitionResearchTest` 3/3 通过（结构化入参；无 LLM 时 final == rulePrimary）
- API 返回 `rulePrimary` / `primaryCompetitions` / `llmUsed` / `adjusted` / `explanation`
- 画布与引擎同一 flowId

文档：`task1/README.md`
