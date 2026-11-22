# 任务2：rsi-flow 循环结构

设计：复合节点 `loop` · 实现：3 处改动 · 测试：全部通过

## 设计要点

- **结构**：新增节点类型 `loop`（复合节点），`reference` 指向循环体
- **配置**：`loopMode` / `maxIterations`（必须 > 0）/ `condition` / `onError` / 迭代键
- **执行**：`bindParams → execute → bindResults`；状态记在 `loop:{nodeId}`
- **终止**：强制 `maxIterations` + 可选墙钟超时；缺省直接报错

## 实现层

| 实现层 | 改动 |
|--------|------|
| 配置 | `FlowNodeConfig` 增加 6 个字段 |
| 注册 | `ExecutableEnums` 增加 `loop` |
| 执行 | 新增 `LoopExecutable` |
| 测试 | 5 个示例流程 + 3 个用例 |

## 流程 JSON 示例（while）

路径：`rsi-flow/flow-test/.../FLOW_LOOP_WHILE/flow-loop-while.json`  
外层路线：`start → INIT → loop → end`（无 route 自环；循环在 loop 节点内部）

```json
{
  "id": "FLOW_LOOP_WHILE_NODE",
  "type": "loop",
  "reference": "FLOW_LOOP_BODY",
  "loopMode": "while",
  "condition": "i < times",
  "maxIterations": 10000,
  "onError": "throw"
}
```

循环体：`FLOW_LOOP_BODY`（每轮 `i++`，`sum+=i`）  
同类示例：`FLOW_LOOP_DO_WHILE`、`FLOW_LOOP_FOR`（`iterationKey: "items"`）

## 验证

BUILD SUCCESS · `FlowLoopTest` 3/3 通过  
（for → `joined=abc` / while → `sum=6` / do-while → `sum=1`）

文档：`task2/LOOP_DESIGN.md`
