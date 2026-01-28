package org.eclipse.emt4j.common.rule;

import org.junit.Test;

public class TestRuleResultCodeFilter {

    @Test
    public void testDisableRules() {
        RuleResultCodeFilter f = new RuleResultCodeFilter(null, "A,B");
        org.junit.Assert.assertFalse(f.accept("A"));
        org.junit.Assert.assertTrue(f.accept("C"));
    }

    @Test
    public void testEnableRules() {
        RuleResultCodeFilter f = new RuleResultCodeFilter("A,B", null);
        org.junit.Assert.assertTrue(f.accept("A"));
        org.junit.Assert.assertFalse(f.accept("C"));
    }
}

