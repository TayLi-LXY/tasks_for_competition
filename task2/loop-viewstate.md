# 循环结构可视化示意（复合 loop 节点）

## while / do-while

```
┌─────────┐
│  start  │
└────┬────┘
     ▼
┌─────────────────┐
│     INIT        │  i=0, sum=0
└────────┬────────┘
         ▼
┌─────────────────────────────────────┐
│  type: loop                         │
│  loopMode: while | do-while         │
│  condition: i < times               │
│  maxIterations: 10000               │
│  reference: FLOW_LOOP_BODY ─────────┼──► BODY: i++; sum+=i
└────────┬────────────────────────────┘
         ▼
      ┌─────┐
      │ end │  输出 sum
      └─────┘
```

## for

```
┌─────────┐
│  start  │
└────┬────┘
     ▼
┌─────────────────┐
│     INIT        │  joined=""
└────────┬────────┘
         ▼
┌─────────────────────────────────────┐
│  type: loop                         │
│  loopMode: for                      │
│  iterationKey: items                │
│  maxIterations: 10000               │
│  reference: FLOW_LOOP_FOR_BODY ─────┼──► BODY: joined += item
└────────┬────────────────────────────┘
         ▼
      ┌─────┐
      │ end │  输出 joined
      └─────┘
```

状态作用域：`loop:{nodeId}`（mode / iteration / status）
