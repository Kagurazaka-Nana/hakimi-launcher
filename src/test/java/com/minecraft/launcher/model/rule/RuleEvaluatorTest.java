package com.minecraft.launcher.model.rule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RuleEvaluator 的单元测试，用例来自 docs/unitTest.md。
 *
 * 关键设计：测试环境用 new RuleContext(...) 构造器手动注入假环境，
 * 绝不用 RuleContext.fromSystem() —— 后者读的是本机系统，换台机器测试结果就会变。
 */
class RuleEvaluatorTest {

    // 用假环境构造评估器：windows / 10.0.19045 / x64，不启用任何 feature
    private RuleEvaluator evaluator() {
        return new RuleEvaluator(
                new RuleContext("windows", "10.0.19045", "x64", Map.of()));
    }

    // 规则只有 action，没有 os / features 约束 → 无条件适用
    private Rule plain(Action action) {
        return new Rule(action, null, null);
    }

    // 规则带 os 约束
    private Rule osRule(Action action, OperatingSystem os) {
        return new Rule(action, null, os);
    }

    // ── 空列表语义：null 或空 → 默认允许 ──
    @Test
    void emptyRules_shouldBeAllowed() {
        assertTrue(evaluator().evaluate(null));
        assertTrue(evaluator().evaluate(List.of()));
    }

    // ── allow(osx) 在 windows 上 → 不允许 ──
    @Test
    void allowOsxRule_shouldNotMatchOnWindows() {
        OperatingSystem osx = new OperatingSystem("osx", null, null, null);
        assertFalse(evaluator().evaluate(List.of(osRule(Action.ALLOW, osx))));
    }

    // ── allow(osx) 在 osx 上 → 允许 ──
    @Test
    void allowOsxRule_shouldMatchOnMac() {
        RuleEvaluator mac = new RuleEvaluator(
                new RuleContext("osx", "14.5", "x64", Map.of()));
        OperatingSystem osx = new OperatingSystem("osx", null, null, null);
        assertTrue(mac.evaluate(List.of(osRule(Action.ALLOW, osx))));
    }

    // ── lwjgl 场景：[allow(无条件), disallow(osx)] ──
    @Test
    void lwjglRules_shouldAllowOnWindows_butDisallowOnMac() {
        List<Rule> lwjgl = List.of(plain(Action.ALLOW), osRule(Action.DISALLOW,
                new OperatingSystem("osx", null, null, null)));

        assertTrue(evaluator().evaluate(lwjgl));   // windows → true

        RuleEvaluator mac = new RuleEvaluator(
                new RuleContext("osx", "14.5", "x64", Map.of()));
        assertFalse(mac.evaluate(lwjgl));          // osx → false（后写的规则覆盖先写的）
    }

    // ── last-match-wins：[disallow(无条件), allow(osx)] 在 osx 上 → 允许 ──
    @Test
    void laterRule_shouldOverrideEarlierOne() {
        List<Rule> rules = List.of(plain(Action.DISALLOW), osRule(Action.ALLOW,
                new OperatingSystem("osx", null, null, null)));
        RuleEvaluator mac = new RuleEvaluator(
                new RuleContext("osx", "14.5", "x64", Map.of()));
        assertTrue(mac.evaluate(rules));
    }

    // ── os.version 是正则，且要求全串匹配（^10\\.）──
    @Test
    void versionRegex_shouldFullMatch() {
        OperatingSystem win10 = new OperatingSystem("windows", "^10\\.", null, null);
        assertTrue(evaluator().evaluate(List.of(osRule(Action.ALLOW, win10))));

        RuleEvaluator win7 = new RuleEvaluator(
                new RuleContext("windows", "6.1.7601", "x64", Map.of()));
        assertFalse(win7.evaluate(List.of(osRule(Action.ALLOW, win10))));
    }

    // ── os.arch 精确匹配 ──
    @Test
    void archRule_shouldNotMatchOnDifferentArch() {
        OperatingSystem x86 = new OperatingSystem(null, null, null, "x86");
        assertFalse(evaluator().evaluate(List.of(osRule(Action.ALLOW, x86))));
    }

    // ── features 匹配：声明值必须与当前环境完全相等 ──
    @Test
    void featureRule_shouldMatchOnlyWhenEnabled() {
        Features customRes = new Features(null, true, null, null, null, null);
        Rule rule = new Rule(Action.ALLOW, customRes, null);

        RuleEvaluator enabled = new RuleEvaluator(
                new RuleContext("windows", "10.0.19045", "x64",
                        Map.of("has_custom_resolution", true)));
        assertTrue(enabled.evaluate(List.of(rule)));

        assertFalse(evaluator().evaluate(List.of(rule)));   // 默认没启用 → false
    }
}
