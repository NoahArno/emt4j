package org.eclipse.emt4j.common.rule.impl;

import org.eclipse.emt4j.common.DependType;
import org.eclipse.emt4j.common.Dependency;
import org.eclipse.emt4j.common.RuleImpl;
import org.eclipse.emt4j.common.classanalyze.PackageUsageScanner;
import org.eclipse.emt4j.common.rule.ExecutableRule;
import org.eclipse.emt4j.common.rule.model.CheckResult;
import org.eclipse.emt4j.common.rule.model.ConfRuleItem;
import org.eclipse.emt4j.common.rule.model.ConfRules;
import org.eclipse.emt4j.common.util.FileUtil;

import java.util.HashSet;
import java.util.Set;

@RuleImpl(type = "javax-package-usage")
public class JavaxPackageUsageRule extends ExecutableRule {

    private final Set<String> packageSet = new HashSet<>();
    private String packageListFile;
    private PackageUsageScanner packageUsageScanner;

    public JavaxPackageUsageRule(ConfRuleItem confRuleItem, ConfRules confRules) {
        super(confRuleItem, confRules);
    }

    @Override
    public void init() {
        packageSet.addAll(FileUtil.readPlainTextFromResource(confRules.getRuleDataPathPrefix() + packageListFile, false));
        packageUsageScanner = new PackageUsageScanner(packageSet);
    }

    @Override
    protected CheckResult check(Dependency dependency) {
        if (packageUsageScanner == null) {
            return CheckResult.PASS;
        }
        return packageUsageScanner.containsAny(dependency.getCurrClassBytecode()) ? CheckResult.FAIL : CheckResult.PASS;
    }

    @Override
    protected boolean accept(Dependency dependency) {
        return DependType.WHOLE_CLASS == dependency.getDependType();
    }

    public void setPackageListFile(String packageListFile) {
        this.packageListFile = packageListFile;
    }
}
