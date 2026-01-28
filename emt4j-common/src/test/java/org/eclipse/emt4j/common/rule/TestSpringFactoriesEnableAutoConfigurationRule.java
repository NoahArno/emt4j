package org.eclipse.emt4j.common.rule;

import org.eclipse.emt4j.common.DependTarget;
import org.eclipse.emt4j.common.Dependency;
import org.eclipse.emt4j.common.rule.impl.SpringFactoriesEnableAutoConfigurationRule;
import org.eclipse.emt4j.common.rule.model.ConfRuleItem;
import org.eclipse.emt4j.common.rule.model.ConfRules;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class TestSpringFactoriesEnableAutoConfigurationRule {

    @Test
    public void testJarWithEnableAutoConfiguration() throws Exception {
        File jarFile = Files.createTempFile("spring-factories", ".jar").toFile();
        writeJar(jarFile, "META-INF/spring.factories",
                "org.springframework.boot.autoconfigure.EnableAutoConfiguration=foo.Bar\n");
        SpringFactoriesEnableAutoConfigurationRule rule = new SpringFactoriesEnableAutoConfigurationRule(ruleItem(), rules());
        rule.init();

        Dependency dependency = new Dependency(null, new DependTarget.Location(jarFile.toURI().toURL()), null, jarFile.getAbsolutePath());
        org.junit.Assert.assertFalse(rule.execute(dependency).isPass());
    }

    @Test
    public void testJarWithoutEnableAutoConfiguration() throws Exception {
        File jarFile = Files.createTempFile("spring-factories", ".jar").toFile();
        writeJar(jarFile, "META-INF/spring.factories", "a=b\n");
        SpringFactoriesEnableAutoConfigurationRule rule = new SpringFactoriesEnableAutoConfigurationRule(ruleItem(), rules());
        rule.init();

        Dependency dependency = new Dependency(null, new DependTarget.Location(jarFile.toURI().toURL()), null, jarFile.getAbsolutePath());
        org.junit.Assert.assertTrue(rule.execute(dependency).isPass());
    }

    @Test
    public void testDirectoryWithEnableAutoConfiguration() throws Exception {
        File dir = Files.createTempDirectory("spring-factories-dir").toFile();
        File metaInf = new File(dir, "META-INF");
        org.junit.Assert.assertTrue(metaInf.mkdirs());
        File factories = new File(metaInf, "spring.factories");
        Files.write(factories.toPath(),
                "org.springframework.boot.autoconfigure.EnableAutoConfiguration=foo.Bar\n".getBytes(StandardCharsets.ISO_8859_1));

        SpringFactoriesEnableAutoConfigurationRule rule = new SpringFactoriesEnableAutoConfigurationRule(ruleItem(), rules());
        rule.init();

        Dependency dependency = new Dependency(null, new DependTarget.Location(dir.toURI().toURL()), null, dir.getAbsolutePath());
        org.junit.Assert.assertFalse(rule.execute(dependency).isPass());
    }

    @Test
    public void testNestedJarWithEnableAutoConfiguration() throws Exception {
        byte[] nestedJarBytes = buildJarBytes("META-INF/spring.factories",
                "org.springframework.boot.autoconfigure.EnableAutoConfiguration=foo.Bar\n");
        File jarFile = Files.createTempFile("outer", ".jar").toFile();
        try (OutputStream out = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(out)) {
            JarEntry entry = new JarEntry("lib/inner.jar");
            jos.putNextEntry(entry);
            jos.write(nestedJarBytes);
            jos.closeEntry();
        }

        SpringFactoriesEnableAutoConfigurationRule rule = new SpringFactoriesEnableAutoConfigurationRule(ruleItem(), rules());
        rule.init();

        Dependency dependency = new Dependency(null, new DependTarget.Location(jarFile.toURI().toURL()), null, jarFile.getAbsolutePath());
        org.junit.Assert.assertFalse(rule.execute(dependency).isPass());
    }

    private void writeJar(File jarFile, String entryName, String entryContent) throws Exception {
        try (OutputStream out = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(out)) {
            JarEntry entry = new JarEntry(entryName);
            jos.putNextEntry(entry);
            jos.write(entryContent.getBytes(StandardCharsets.ISO_8859_1));
            jos.closeEntry();
        }
    }

    private byte[] buildJarBytes(String entryName, String entryContent) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            JarEntry entry = new JarEntry(entryName);
            jos.putNextEntry(entry);
            jos.write(entryContent.getBytes(StandardCharsets.ISO_8859_1));
            jos.closeEntry();
        }
        return baos.toByteArray();
    }

    private ConfRuleItem ruleItem() {
        ConfRuleItem ruleItem = new ConfRuleItem();
        ruleItem.setType("spring-factories-enable-autoconfiguration");
        ruleItem.setResultCode("SPRING_FACTORIES_ENABLE_AUTOCONFIGURATION");
        ruleItem.setPriority(1);
        return ruleItem;
    }

    private ConfRules rules() {
        return new ConfRules();
    }
}

