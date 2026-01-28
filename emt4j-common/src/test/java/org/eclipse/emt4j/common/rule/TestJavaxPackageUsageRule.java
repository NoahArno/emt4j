package org.eclipse.emt4j.common.rule;

import org.eclipse.emt4j.common.DependTarget;
import org.eclipse.emt4j.common.DependType;
import org.eclipse.emt4j.common.Dependency;
import org.eclipse.emt4j.common.rule.impl.JavaxPackageUsageRule;
import org.eclipse.emt4j.common.rule.model.ConfRuleItem;
import org.eclipse.emt4j.common.rule.model.ConfRules;
import org.eclipse.emt4j.common.rule.model.ReportCheckResult;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.junit.Test;

public class TestJavaxPackageUsageRule {

    @Test
    public void testJavaxPackageMatch() {
        JavaxPackageUsageRule rule = new JavaxPackageUsageRule(ruleItem(), rules());
        rule.setPackageListFile("javax_packages.cfg");
        rule.init();

        byte[] bytecode = createClassWithField("t/HasJavaxServletField", "Ljavax/servlet/Servlet;", null, null);
        Dependency dependency = new Dependency(null, new DependTarget.Class("t.HasJavaxServletField", DependType.WHOLE_CLASS), null, "dummy");
        dependency.setCurrClassBytecode(bytecode);
        ReportCheckResult result = rule.execute(dependency);
        org.junit.Assert.assertFalse(result.isPass());
        org.junit.Assert.assertEquals("JAVAX_PACKAGE_USAGE", result.getResultCode());
    }

    @Test
    public void testNonJavaxPass() {
        JavaxPackageUsageRule rule = new JavaxPackageUsageRule(ruleItem(), rules());
        rule.setPackageListFile("javax_packages.cfg");
        rule.init();

        byte[] bytecode = createClassWithField("t/NoJavax", "Ljava/util/List;", null, null);
        Dependency dependency = new Dependency(null, new DependTarget.Class("t.NoJavax", DependType.WHOLE_CLASS), null, "dummy");
        dependency.setCurrClassBytecode(bytecode);
        org.junit.Assert.assertTrue(rule.execute(dependency).isPass());
    }

    @Test
    public void testDescriptorStyleMatch() {
        JavaxPackageUsageRule rule = new JavaxPackageUsageRule(ruleItem(), rules());
        rule.setPackageListFile("javax_packages.cfg");
        rule.init();

        byte[] bytecode = createClassWithField("t/HasJavaxString", "Ljava/lang/String;",
                null, "javax.servlet.Servlet");
        Dependency dependency = new Dependency(null, new DependTarget.Class("t.HasJavaxString", DependType.WHOLE_CLASS), null, "dummy");
        dependency.setCurrClassBytecode(bytecode);
        org.junit.Assert.assertFalse(rule.execute(dependency).isPass());
    }

    private ConfRuleItem ruleItem() {
        ConfRuleItem ruleItem = new ConfRuleItem();
        ruleItem.setType("javax-package-usage");
        ruleItem.setResultCode("JAVAX_PACKAGE_USAGE");
        ruleItem.setPriority(2);
        return ruleItem;
    }

    private ConfRules rules() {
        ConfRules rules = new ConfRules();
        rules.setRuleDataPathPrefix("/default/rule/8to11/data/");
        return rules;
    }

    private static byte[] createClassWithField(String internalName, String fieldDesc, String fieldSignature, String ldcString) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        cw.visitField(Opcodes.ACC_PUBLIC, "f", fieldDesc, fieldSignature, null).visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
        mv.visitCode();
        if (ldcString != null) {
            mv.visitLdcInsn(ldcString);
            mv.visitInsn(Opcodes.POP);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(ldcString != null ? 1 : 0, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
