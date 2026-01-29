package org.eclipse.emt4j.common.rule;

import org.eclipse.emt4j.common.DependTarget;
import org.eclipse.emt4j.common.DependType;
import org.eclipse.emt4j.common.Dependency;
import org.eclipse.emt4j.common.Feature;
import org.eclipse.emt4j.common.rule.impl.ReferenceClassRule;
import org.eclipse.emt4j.common.rule.model.CheckResult;
import org.eclipse.emt4j.common.rule.model.ConfRuleItem;
import org.eclipse.emt4j.common.rule.model.ConfRules;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Optional;

public class TestRemovedCorbaRule {

    @Test
    public void testRemovedCorbaShouldMatchJavaxRmiCorbaUtil() throws Exception {
        Optional<ConfRules> opt = ConfRuleRepository.load(Feature.DEFAULT, 8, 11);
        org.junit.Assert.assertTrue(opt.isPresent());

        ConfRules confRules = opt.get();
        ConfRuleItem ruleItem = confRules.getRuleItems().stream()
                .filter(i -> "REMOVED_CORBA".equals(i.getResultCode()))
                .findFirst()
                .orElse(null);
        org.junit.Assert.assertNotNull(ruleItem);

        ReferenceClassRule rule = new ReferenceClassRule(ruleItem, confRules);
        if (ruleItem.getUserDefineAttrs() != null) {
            for (String[] nv : ruleItem.getUserDefineAttrs()) {
                Method m = rule.getClass().getMethod(toSetter(nv[0]), String.class);
                m.setAccessible(true);
                m.invoke(rule, nv[1]);
            }
        }
        rule.init();

        Dependency d = new Dependency();
        d.setTarget(new DependTarget.Class("javax.rmi.CORBA.Util", DependType.CLASS));
        CheckResult r = rule.check(d);
        org.junit.Assert.assertFalse(r.isPass());
    }

    @Test
    public void testRemovedCorbaShouldMatchOrgOmgCorba() throws Exception {
        Optional<ConfRules> opt = ConfRuleRepository.load(Feature.DEFAULT, 8, 11);
        org.junit.Assert.assertTrue(opt.isPresent());

        ConfRules confRules = opt.get();
        ConfRuleItem ruleItem = confRules.getRuleItems().stream()
                .filter(i -> "REMOVED_CORBA".equals(i.getResultCode()))
                .findFirst()
                .orElse(null);
        org.junit.Assert.assertNotNull(ruleItem);

        ReferenceClassRule rule = new ReferenceClassRule(ruleItem, confRules);
        if (ruleItem.getUserDefineAttrs() != null) {
            for (String[] nv : ruleItem.getUserDefineAttrs()) {
                Method m = rule.getClass().getMethod(toSetter(nv[0]), String.class);
                m.setAccessible(true);
                m.invoke(rule, nv[1]);
            }
        }
        rule.init();

        Dependency d = new Dependency();
        d.setTarget(new DependTarget.Class("org.omg.CORBA.ORB", DependType.CLASS));
        CheckResult r = rule.check(d);
        org.junit.Assert.assertFalse(r.isPass());
    }

    private static String toSetter(String attrName) {
        StringBuilder setMethodName = new StringBuilder(attrName.length() + "set".length());
        setMethodName.append("set");
        char[] chars = attrName.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '-') {
                continue;
            } else {
                if (i == 0 || chars[i - 1] == '-') {
                    setMethodName.append(Character.toUpperCase(chars[i]));
                } else {
                    setMethodName.append(chars[i]);
                }
            }
        }
        return setMethodName.toString();
    }
}

