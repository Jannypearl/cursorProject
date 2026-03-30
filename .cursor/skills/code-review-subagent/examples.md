# Review Examples

## Example 1: 发现关键问题

```markdown
## Findings
- [Critical] src/main/java/com/example/user/UserService.java: `updateEmail()` 未校验用户归属
  - 影响：可被越权修改他人邮箱
  - 建议：在写入前校验 `currentUserId == targetUserId` 或增加管理员权限校验

- [Major] src/main/java/com/example/user/UserController.java: 入参缺少 `@Valid`
  - 影响：非法请求可能进入业务层并触发运行时异常
  - 建议：在接口入参加 `@Valid` 并补充字段约束

## Brief Summary
- 发现 1 个高危权限问题与 1 个参数校验缺失问题，建议合并前修复。
```

## Example 2: 无问题但有测试缺口

```markdown
## Findings
- No findings.

## Brief Summary
- 当前改动未发现明显逻辑缺陷。
- 仍建议补充失败路径测试：数据库写失败时的回滚行为。
```
