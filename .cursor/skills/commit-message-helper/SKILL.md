---
name: commit-message-helper
description: Generate clear git commit messages from code changes and repository history. Use when the user asks to write a commit message, summarize staged/unstaged diffs, or improve commit text quality in Chinese or English.
---

# Commit Message Helper

## 目标

为当前改动生成清晰、可追溯的提交信息，优先贴合仓库既有风格，而不是强制套用固定规范。

## 何时使用

- 用户提到“写 commit message / 提交信息 / 提交说明”
- 用户希望“根据 diff 总结这次改动”
- 用户已有草稿，想优化可读性或语义准确性

## 快速流程

1. 先看改动范围：关注 staged 与 unstaged 的核心变化。
2. 再看历史风格：读取最近提交，识别团队常用语气和粒度。
3. 归纳“为什么改”：优先写动机和结果，不只罗列文件变更。
4. 产出 2-3 个候选提交信息，默认给一个“最稳妥版本”。

## 写作原则

- 与仓库风格一致：如果历史提交多为中文短句，优先中文短句。
- 语义准确：`增加` 用于新增能力，`修复` 用于问题修正，`优化` 用于改进但不改变核心行为。
- 一次提交一个主语义：避免把无关改动混在同一句里。
- 先主后次：第一句讲主改动，必要时第二段补充影响范围或兼容性。

## 输出格式（高自由）

按改动复杂度自行选择：

- **简单改动**：单行提交信息
- **中等改动**：标题 + 1-2 行补充
- **复杂改动**：标题 + 要点列表（最多 3 条）

在无法确定真实意图时，先给“保守中性”的描述，不臆测业务背景。

## 质量自检

- 是否能看出“这次改动的核心目的”？
- 是否避免了“update/fix stuff”这类空泛措辞？
- 是否与当前仓库历史风格一致？
- 是否包含不应承诺的内容（如未验证的性能提升）？

## 额外资源

- 参考语气与措辞：[reference.md](reference.md)
- 示例输入输出：[examples.md](examples.md)
