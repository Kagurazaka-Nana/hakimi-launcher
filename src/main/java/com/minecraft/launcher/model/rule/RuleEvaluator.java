package com.minecraft.launcher.model.rule;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RuleEvaluator {

    private static final Map<String, Pattern> PATTERN_CACHE =
            new ConcurrentHashMap<>();
    private final RuleContext ruleContext;

    public RuleEvaluator(RuleContext ruleContext) {
        this.ruleContext = ruleContext;
    }

    public static RuleEvaluator fromSystem() {
        return new RuleEvaluator(RuleContext.fromSystem());
    }

    public boolean evaluate(List<Rule> rules) {
        return evaluate(rules, Action.ALLOW);
    }

    public boolean evaluate(List<Rule> rules, Action fallback) {
        if (rules == null || rules.isEmpty()) {
            return fallback == Action.ALLOW;
        }

        Action current = defaultAction(rules.getFirst());
        for(Rule rule : rules) {
            Action action = appliedAction(rule);
            if(action != null) {
                current = action;
            }
        }

        return current == Action.ALLOW;
    }

    public RuleContext getRuleContext() {
        return ruleContext;
    }

    private Action appliedAction(Rule rule) {

        boolean osMatches = rule.getOs() == null || matches(rule.getOs());
        boolean featureMatches = rule.getFeatures() == null || matches(rule.getFeatures());
        if(osMatches && featureMatches) {
            return rule.getAction();
        }
        return null;

    }

    private boolean matches(OperatingSystem ruleOs) {

        if(ruleOs.getName() != null && !ruleOs.getName().equalsIgnoreCase(ruleContext.getOsName())) {
            return false;
        }

        if(ruleOs.getVersion() != null && !matchesRegex(ruleOs.getVersion(), ruleContext.getOsVersion())) {
            return false;
        }

        if(ruleOs.getArch() != null && !ruleOs.getArch().equalsIgnoreCase(ruleContext.getArch())) {
            return false;
        }

        VersionRange range = ruleOs.getVersionRange();
        if(range != null) {
            if(range.getMin() != null && OSVersionComparator.compare(ruleContext.getOsVersion(), range.getMin()) < 0) {
                return false;
            }

            return range.getMax() == null || OSVersionComparator.compare(ruleContext.getOsVersion(), range.getMax()) <= 0;
        }
        return true;

    }

    private boolean matches(Features ruleFeatures) {

        for(Map.Entry<String, Boolean> entry : ruleFeatures.toMap().entrySet()) {
            if(!Objects.equals(ruleContext.getFeatures().get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;

    }

    private boolean matchesRegex(String regex, String candidate) {

        if(candidate == null) return false;

        // Mojang 的 os.version 是前缀正则（如 "^10\\." 用于 Win10 检测），
        // 必须用 find() 前缀匹配，而不能用 matches() 全串匹配——
        // 否则 "^10\\." 对 "10.0.19045" 会因无法消费整串而错误地返回 false。
        Pattern pattern = PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
        return pattern.matcher(candidate).find();
    }

    private Action defaultAction(Rule first) {
        return first.getAction() == Action.ALLOW ? Action.DISALLOW : Action.ALLOW;
    }


}
