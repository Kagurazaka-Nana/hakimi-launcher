# Rule 模块说明

> 🔗 **权威文档见 [`docs/Rule.md`](docs/Rule.md)**（docs 子模块：`Kagurazaka-Nana/hakimi-launcher-docs`）。本文件只是仓库根的跳转摘要，避免两处内容漂移。

`com.minecraft.launcher.model.rule` 处理 Minecraft version JSON 中 `libraries[]` / `arguments.game` / `arguments.jvm` 的 `rules` 评估。

核心语义（详见 `docs/Rule.md`）：

- 空规则列表 → 默认允许（可传入 fallback）。
- 逐条评估，所有匹配规则生效，**后命中覆盖前命中**（last-match-wins）。
- 初始默认由首条规则反向决定（`defaultAction`：首条 allow → disallow，反之 allow）。
- `os.version` 是正则，用 `find()` 前缀匹配（如 `^10\.`），勿改 `matches()`。
- `features` 严格相等匹配；`versionRange` 由 `OSVersionComparator` 按 `major.minor.build` 整数比较。

维护时同步更新 `docs/Rule.md`、`docs/Evaluator.md` 与 `RuleEvaluatorTest`。