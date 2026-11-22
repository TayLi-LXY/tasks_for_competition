# 本周任务交付

依据 `requirement.md`，完成任务1（必做）与任务2（选做）。

## 任务1：竞赛调研流程（满分 5）

在 `rsi-flow-visual` 上设计「一年内适合参赛」的第一条流程，并结合认定表 + 推免计分办法完成调研；流程已接到 `rsi-flow` 可执行。

| 内容 | 路径 |
|------|------|
| 说明与流程设计 | `task1/README.md` |
| 推荐赛清单 | `task1/recommended-competitions.md` |
| 流程 JSON | `task1/competition-research-flow.json` |
| 可视化 ViewState | `rsi-flow-visual/demo/test/TEST_COMPETITION_RESEARCH/` |
| 可执行节点 | `rsi-flow/.../code/FlowCompetitionCode.java` |
| 引擎单测 | `rsi-flow/.../FLOW_COMPETITION_RESEARCH/FlowCompetitionResearchTest.java` |

官方画布（需 Node 18）：

```bash
scoop reset versions/nodejs18
cd rsi-flow-visual
npm install --ignore-scripts
npx parcel demo/index.html --out-dir deploy --port 1234
# 打开 http://localhost:1234
```

真正跑出主赛/备赛（推荐整条链路）：

```powershell
# 终端1：引擎 API
.\scripts\start-competition-api.ps1

# 终端2：官方画布 + 右侧结构化画像表单（Node 18）
scoop reset versions/nodejs18
cd rsi-flow-visual
npx parcel demo/index.html --out-dir deploy --port 1234
# 打开 http://localhost:1234 填年级/动机/技能/投入后运行
```

CLI / 单测：

```powershell
.\scripts\run-competition-cli.ps1
cd rsi-flow
mvn -pl flow-test -am test "-Dtest=FlowCompetitionResearchTest" "-Djacoco.skip=true" "-Dsurefire.failIfNoSpecifiedTests=false"
```

赛历：`competition/catalog.json`。链路：规则保底 + LLM 可调整（WHU DeepSeek）。

> `rsi-flow-react` 仓库当前 404，不可用；可视化基于 `rsi-flow-visual`。

## 任务2：循环结构（设计 5 + 实现 5）

| 内容 | 路径 |
|------|------|
| 方案设计（供讨论） | `task2/LOOP_DESIGN.md` |
| 循环示意 | `task2/loop-viewstate.md` |
| 执行器 | `rsi-flow/flow-core/.../LoopExecutable.java` |
| 示例流程 | `FLOW_LOOP_WHILE` / `FLOW_LOOP_DO_WHILE` / `FLOW_LOOP_FOR` + BODY |
| 节点代码 | `rsi-flow/flow-test/src/test/java/cc/eamon/open/flow/code/FlowLoopCode.java` |
| 单元测试 | `rsi-flow/flow-test/src/test/java/cc/eamon/open/flow/FLOW_LOOP/FlowLoopTest.java` |

实现方式：复合节点 `type: "loop"`（for / while / do-while），强制 `maxIterations`，状态写入 `loop:{nodeId}`。

验证：

```bash
cd rsi-flow
# 若缺私有依赖，先执行仓库根目录 _stub_*.py 安装本地桩
mvn -pl flow-test -am test "-Dtest=FlowLoopTest" "-Djacoco.skip=true" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 参考材料

- `附件：武汉大学本科生学科竞赛认定信息表（2026年）.xls`
- `2027届计算机学院免试攻读硕士学位研究生综合成绩计分办法（2023）-提交版(1).docx`
