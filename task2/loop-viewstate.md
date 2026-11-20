# 循环结构可视化示意

```
┌─────────┐
│  start  │
└────┬────┘
     ▼
┌─────────────────┐
│ FLOW_LOOP_INIT  │  i=0, sum=0
└────────┬────────┘
         ▼
┌─────────────────┐ ◄──────────────┐
│ FLOW_LOOP_BODY  │  i++; sum+=i   │
└────────┬────────┘                │
         │                         │
    ┌────┴────┐                    │
    │         │                    │
 i<times   i>=times                │
    │         │                    │
    └────►────┘                    │
    continue              exit     │
    └──────────────────────────────┘
              ▼
           ┌─────┐
           │ end │  输出 sum
           └─────┘
```

对应 JSON 回边：

```json
{ "source": "FLOW_LOOP_BODY", "target": "FLOW_LOOP_BODY", "condition": "i < times" },
{ "source": "FLOW_LOOP_BODY", "target": "end", "condition": "i >= times" }
```
