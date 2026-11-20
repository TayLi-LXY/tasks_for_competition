# 任务2：rsi-flow 循环结构方案

## 1. 现状

`rsi-flow` 已支持：

| 结构 | 实现方式 |
|------|----------|
| 顺序 | `routes` 单后继 |
| 分支 | `routes` + `condition`（Jexl） |
| 嵌套 | `type: "flow"` + `reference` 子流程 |
| 并行 | `strategies: all / any / close / async` |

缺少：**循环**（同一子图按条件反复执行）。

核心执行器 `Executor` 用队列驱动节点，**没有“已访问节点不可再入”限制**，因此回边在运行时是可行的。

## 2. 方案对比

### 方案 A：条件回边（While 语义，推荐先落地）

用现有 `condition` 构造回边，零侵入引擎：

```
start → INIT → BODY ──[i < times]──► BODY
                 └──[i >= times]──► end
```

优点：与条件分支一致、实现快、可测。  
缺点：循环语义散落在多条 route；缺统一 max-iteration 保护。

### 方案 B：专用 loop 策略（后续增强）

新增 `strategies: [{ "source":"LOOP_HEAD", "strategy":"loop" }]`，配置：

```json
{
  "source": "LOOP_HEAD",
  "strategy": "loop",
  "condition": "i < times",
  "body": "LOOP_BODY",
  "maxIterations": 10000
}
```

引擎侧增加 `LoopSourceStrategy` + `LoopExecutor`：在策略内反复调度 body，超限抛错。  
优点：语义清晰、易做可视化。  
缺点：改动面大，需讨论后合入。

### 方案 C：嵌套子流程 + 外部 while

把 body 做成子 flow，外层 Java/`@Flow` 里 while 调用 `FlowEngine.run(subFlow)`。  
优点：不改路由。  
缺点：循环不在流程 JSON 内，可视化丢失。

## 3. 本次实现：方案 A

落地示例流程 `FLOW_LOOP`：计算 `sum = 1 + 2 + … + times`。

```
start → FLOW_LOOP_INIT → FLOW_LOOP_BODY ──[i < times]──► FLOW_LOOP_BODY
                              └──[i >= times]──► end
```

| 节点 | 作用 |
|------|------|
| `FLOW_LOOP_INIT` | `i=0, sum=0` |
| `FLOW_LOOP_BODY` | `i++, sum+=i` |

约束（讨论结论）：

1. 继续/退出条件必须互斥且可终止。
2. 循环变量必须作为节点 `results` 写出，才能进入路由变量表。
3. 建议业务层保证 `times` 有上界；方案 B 再补引擎级 `maxIterations`。

## 4. 与可视化的对应

在 `rsi-flow-visual` 中，循环回边用带条件名的 `branch` 链接表示（`continue` / `exit`），与分支共用连线类型，靠条件文案区分。

## 5. 讨论点（待课上确认）

1. 是否需要方案 B 的一等公民 `loop` 策略？
2. 是否支持 for-each（对 List 迭代）——可在方案 A 上用索引模拟，或方案 B 扩展 `iterator`。
3. 嵌套循环：方案 A 自然支持多层回边；方案 B 需处理策略嵌套。

## 6. 文件清单

| 路径 | 说明 |
|------|------|
| `rsi-flow/.../FLOW_LOOP/flow-loop.json` | 循环流程配置 |
| `rsi-flow/.../code/FlowLoopCode.java` | INIT / BODY 节点代码 |
| `rsi-flow/.../FLOW_LOOP/FlowLoopTest.java` | 单元测试 |
| `task2/loop-viewstate.md` | 循环结构示意 |

## 7. 验证结果

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
- caseLoopSumToThree  → sum=6
- caseLoopSumToTen    → sum=55
- caseLoopOnce        → sum=1
BUILD SUCCESS
```

日志可见 `FLOW_LOOP_BODY` 按条件反复执行（times=10 时 body 执行 10 次），回边生效。
