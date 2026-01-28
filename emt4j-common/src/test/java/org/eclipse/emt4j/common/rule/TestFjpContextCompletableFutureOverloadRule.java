package org.eclipse.emt4j.common.rule;

import org.eclipse.emt4j.common.DependTarget;
import org.eclipse.emt4j.common.DependType;
import org.eclipse.emt4j.common.Dependency;
import org.eclipse.emt4j.common.rule.impl.TouchedMethodRule;
import org.eclipse.emt4j.common.rule.model.ConfRuleItem;
import org.eclipse.emt4j.common.rule.model.ConfRules;
import org.junit.Test;

public class TestFjpContextCompletableFutureOverloadRule {

    @Test
    public void testRunAsyncWithoutExecutorShouldFail() {
        TouchedMethodRule rule = new TouchedMethodRule(ruleItem(), rules());
        rule.setMethodListFile("fjp_context_implicit.cfg");
        rule.init();

        DependTarget.Method method = new DependTarget.Method("java.util.concurrent.CompletableFuture", "runAsync",
                "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", DependType.METHOD);
        Dependency dependency = new Dependency(null, method, null, "dummy");
        org.junit.Assert.assertFalse(rule.execute(dependency).isPass());
    }

    @Test
    public void testRunAsyncWithExecutorShouldPass() {
        TouchedMethodRule rule = new TouchedMethodRule(ruleItem(), rules());
        rule.setMethodListFile("fjp_context_implicit.cfg");
        rule.init();

        DependTarget.Method method = new DependTarget.Method("java.util.concurrent.CompletableFuture", "runAsync",
                "(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", DependType.METHOD);
        Dependency dependency = new Dependency(null, method, null, "dummy");
        org.junit.Assert.assertTrue(rule.execute(dependency).isPass());
    }

    @Test
    public void testSupplyAsyncWithoutExecutorShouldFail() {
        TouchedMethodRule rule = new TouchedMethodRule(ruleItem(), rules());
        rule.setMethodListFile("fjp_context_implicit.cfg");
        rule.init();

        DependTarget.Method method = new DependTarget.Method("java.util.concurrent.CompletableFuture", "supplyAsync",
                "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;", DependType.METHOD);
        Dependency dependency = new Dependency(null, method, null, "dummy");
        org.junit.Assert.assertFalse(rule.execute(dependency).isPass());
    }

    @Test
    public void testSupplyAsyncWithExecutorShouldPass() {
        TouchedMethodRule rule = new TouchedMethodRule(ruleItem(), rules());
        rule.setMethodListFile("fjp_context_implicit.cfg");
        rule.init();

        DependTarget.Method method = new DependTarget.Method("java.util.concurrent.CompletableFuture", "supplyAsync",
                "(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", DependType.METHOD);
        Dependency dependency = new Dependency(null, method, null, "dummy");
        org.junit.Assert.assertTrue(rule.execute(dependency).isPass());
    }

    private ConfRuleItem ruleItem() {
        ConfRuleItem ruleItem = new ConfRuleItem();
        ruleItem.setType("touched-method");
        ruleItem.setResultCode("FJP_CONTEXT");
        ruleItem.setPriority(2);
        return ruleItem;
    }

    private ConfRules rules() {
        ConfRules rules = new ConfRules();
        rules.setRuleDataPathPrefix("/default/rule/8to11/data/");
        return rules;
    }
}
