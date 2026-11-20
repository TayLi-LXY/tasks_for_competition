# 本周任务交付

依据 `requirement.md`，完成任务1（必做）与任务2（选做）。

## 任务1：竞赛调研流程（满分 5）

在 `rsi-flow-visual` 上设计「一年内适合参赛」的第一条流程，并结合认定表 + 推免计分办法完成调研。

| 内容 | 路径 |
|------|------|
| 说明与流程设计 | `task1/README.md` |
| 推荐赛清单 | `task1/recommended-competitions.md` |
| rsi-flow 流程 JSON | `task1/competition-research-flow.json` |
| 可视化 ViewState | `rsi-flow-visual/demo/test/TEST_COMPETITION_RESEARCH/` |

查看可视化：

```bash
cd rsi-flow-visual
yarn install
yarn start
```

> `rsi-flow-react` 仓库当前 404，不可用；可视化基于 `rsi-flow-visual`。

## 任务2：循环结构（设计 5 + 实现 5）

| 内容 | 路径 |
|------|------|
| 方案设计（供讨论） | `task2/LOOP_DESIGN.md` |
| 循环示意 | `task2/loop-viewstate.md` |
| 流程配置 | `rsi-flow/flow-test/src/test/resources/flow/FLOW_LOOP/flow-loop.json` |
| 节点代码 | `rsi-flow/flow-test/src/test/java/cc/eamon/open/flow/code/FlowLoopCode.java` |
| 单元测试 | `rsi-flow/flow-test/src/test/java/cc/eamon/open/flow/FLOW_LOOP/FlowLoopTest.java` |

实现方式：条件回边 While（`i < times` 回 BODY，`i >= times` 出循环）。

验证（已跑通，3/3 通过）：

```bash
cd rsi-flow
# 若缺私有依赖，先执行仓库根目录 _stub_*.py 安装本地桩
mvn -pl flow-test -am test "-Dtest=FlowLoopTest" "-Djacoco.skip=true" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 参考材料

- `附件：武汉大学本科生学科竞赛认定信息表（2026年）.xls`
- `2027届计算机学院免试攻读硕士学位研究生综合成绩计分办法（2023）-提交版(1).docx`
