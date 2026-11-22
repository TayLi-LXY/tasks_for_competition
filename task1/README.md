# 任务1：一年内适合参赛的学科竞赛调研流程

基于 [rsi-flow-visual](https://github.com/ymxiong/rsi-flow-visual) + [rsi-flow](https://github.com/ymxiong/rsi-flow)：

- **画布**展示 `FLOW_COMPETITION_RESEARCH`
- **右侧**填结构化画像（年级/动机/技能/投入）
- **引擎**：规则保底 → LLM 可调整名单并解释

数据依据：认定信息表（2026）+ 推免计分办法。`rsi-flow-react` 404。

## 启动

```powershell
# API（含 LLM 环境变量）
.\scripts\start-competition-api.ps1

# 画布（Node 18）
scoop reset versions/nodejs18
cd rsi-flow-visual
npx parcel demo/index.html --out-dir deploy --port 1234
```

- API：`http://localhost:8088`  
  `POST /api/competition/run`  
  `{"grade":"大三","motive":"冲推免","skills":["算法"],"hours":"10-20","note":""}`
- UI：http://localhost:1234

CLI：

```powershell
.\scripts\run-competition-cli.ps1
# 或
cd rsi-flow
mvn -pl flow-test -am install "-DskipTests" "-Djacoco.skip=true"
mvn -pl flow-test exec:java "-Djacoco.skip=true" "-Dexec.classpathScope=test" "-Dexec.mainClass=cc.eamon.open.flow.competition.CompetitionCli" "-Dexec.args=--grade 大三 --motive 冲推免 --skills 算法 --hours 10-20"
```

单测：

```powershell
cd rsi-flow
mvn -pl flow-test -am test "-Dtest=FlowCompetitionResearchTest" "-Djacoco.skip=true" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## LLM

| 环境变量 | 默认 |
|----------|------|
| `COMPETITION_LLM_API_KEY` | 必填，本地环境变量（勿提交） |
| `COMPETITION_LLM_BASE_URL` | `https://maas.whu.edu.cn/v1` |
| `COMPETITION_LLM_MODEL` | `deepseek-v4-flash` |

`COMP_LLM_ADJUST`：在 catalog 白名单内微调主赛/备赛并解释；失败则保留规则保底。

## 关键文件

| 文件 | 作用 |
|------|------|
| `task1/catalog.json` | 赛历配置副本 |
| `competition-research-flow.json` | 流程定义 |
| `FlowCompetitionCode.java` | 节点实现 |
| `CompetitionApiServer.java` | HTTP |
| `rsi-flow-visual/demo/index.tsx` | 画像表单 + 结果 |

## 流程

```
构建画像 → 读 catalog → 过滤 → 四赛道并行 → 规则保底 → LLM 调整 → 结束
```
