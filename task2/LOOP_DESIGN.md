# 任务2：rsi-flow 循环结构方案

## 1. 现状

`rsi-flow` 已支持：

| 结构 | 实现方式 |
|------|----------|
| 顺序 | `routes` 单后继 |
| 分支 | `routes` + `condition`（Jexl） |
| 嵌套 | `type: "flow"` + `reference` 子流程 |
| 并行 | `strategies: all / any / close / async` |

缺少：**循环**（同一子图按条件/集合反复执行）。

## 2. 方案对比

### 方案 A：条件回边（While 语义）

用现有 `condition` 构造自环，零侵入引擎。  
优点：实现快。缺点：语义散落、无统一 `maxIterations`。

### 方案 B：复合 loop 节点（本次落地）

新增节点类型 `type: "loop"`，`reference` 指向循环体子流程，配置：

| 字段 | 说明 |
|------|------|
| `loopMode` | `for` / `while` / `do-while` |
| `maxIterations` | **必须 > 0**，缺省或非法立即报错 |
| `condition` | while / do-while 的 Jexl 条件 |
| `onError` | `throw` / `break` / `continue` |
| `timeoutMs` | 可选墙钟超时（毫秒） |
| `iterationKey` | for 模式：上下文中的集合变量名 |

执行生命周期：`bindParams → execute(body)×N → bindResults`。  
状态写入作用域 `loop:{nodeId}`（mode / iteration / status）。

### 方案 C：外层 Java while 调子流程

循环不在流程 JSON 内，可视化丢失，不采用。

## 3. 本次实现：方案 B

改动分层：

| 层 | 改动 |
|----|------|
| 配置 | `FlowNodeConfig` 增加 6 个 loop 字段 |
| 注册 | `ExecutableEnums.LOOP("loop", LoopExecutable.class)` |
| 执行 | 新建 `LoopExecutable` |
| 测试 | 5 个示例流程 + 3 个用例 |

示例流程：

```
FLOW_LOOP_WHILE     INIT → loop(while)  → end     reference: FLOW_LOOP_BODY
FLOW_LOOP_DO_WHILE  INIT → loop(do-while) → end   reference: FLOW_LOOP_BODY
FLOW_LOOP_FOR       INIT → loop(for)    → end     reference: FLOW_LOOP_FOR_BODY
FLOW_LOOP_BODY      累加体 i++; sum+=i
FLOW_LOOP_FOR_BODY  拼接体 joined += item
```

约束：

1. `maxIterations` 强制必填且 > 0。
2. while / do-while 必须提供可终止的 `condition`。
3. for 必须提供 `iterationKey`，支持 List / 数组 / 字符串字符迭代。
4. 循环变量经节点 `results` 写回，才能进入下一轮绑定与条件求值。

## 4. 文件清单

| 路径 | 说明 |
|------|------|
| `flow-config/.../FlowNodeConfig.java` | loop 配置字段 |
| `flow-core/.../LoopExecutable.java` | 循环执行器 |
| `flow-core/.../ExecutableEnums.java` | 注册 `loop` |
| `flow-core/.../FlowLoopException.java` | 循环异常 |
| `flow-test/.../FLOW_LOOP_*/` | 5 个示例流程 JSON |
| `flow-test/.../FlowLoopCode.java` | INIT / BODY 节点代码 |
| `flow-test/.../FlowLoopTest.java` | 单元测试 |
| `task2/loop-viewstate.md` | 结构示意 |

## 5. 验证

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
- caseWhileSumToThree  → sum=6
- caseDoWhileOnce      → sum=1
- caseForJoinItems     → joined=abc
BUILD SUCCESS
```

## 6. 讨论点

1. 可视化是否为 loop 单独提供节点样式（vs 普通 flow 嵌套）。
2. for-each 当前元素固定写入 `item` / `index`，是否需可配置。
3. 嵌套 loop：复合节点天然支持，状态域按 `loop:{nodeId}` 隔离。
