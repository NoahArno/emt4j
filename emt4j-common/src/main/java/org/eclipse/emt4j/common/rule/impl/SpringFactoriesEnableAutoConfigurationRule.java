package org.eclipse.emt4j.common.rule.impl;

import org.eclipse.emt4j.common.DependType;
import org.eclipse.emt4j.common.Dependency;
import org.eclipse.emt4j.common.RuleImpl;
import org.eclipse.emt4j.common.rule.ExecutableRule;
import org.eclipse.emt4j.common.rule.model.CheckResult;
import org.eclipse.emt4j.common.rule.model.ConfRuleItem;
import org.eclipse.emt4j.common.rule.model.ConfRules;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RuleImpl(type = "spring-factories-enable-autoconfiguration")
public class SpringFactoriesEnableAutoConfigurationRule extends ExecutableRule {

    private static final String SPRING_FACTORIES_PATH = "META-INF/spring.factories";
    private static final String ENABLE_AUTO_CONFIGURATION_KEY = "org.springframework.boot.autoconfigure.EnableAutoConfiguration";
    private static final int MAX_NESTED_JAR_DEPTH = 5;

    public SpringFactoriesEnableAutoConfigurationRule(ConfRuleItem confRuleItem, ConfRules confRules) {
        super(confRuleItem, confRules);
    }

    @Override
    public void init() {
    }

    @Override
    protected CheckResult check(Dependency dependency) {
        File file = resolveAsFile(dependency);
        if (file == null) {
            return CheckResult.PASS;
        }
        if (file.isDirectory()) {
            return containsSpringFactoriesInDirectory(file) ? CheckResult.FAIL : CheckResult.PASS;
        }
        if (file.isFile() && file.getName().endsWith(".jar")) {
            try {
                boolean found = containsSpringFactoriesInJarFile(file);
                return found ? CheckResult.FAIL : CheckResult.PASS;
            } catch (IOException e) {
                return CheckResult.PASS;
            }
        }
        return CheckResult.PASS;
    }

    @Override
    protected boolean accept(Dependency dependency) {
        return DependType.CODE_SOURCE == dependency.getDependType();
    }

    private boolean containsSpringFactoriesInDirectory(File dir) {
        File f = new File(dir, SPRING_FACTORIES_PATH.replace('/', File.separatorChar));
        if (!f.exists() || !f.isFile()) {
            return false;
        }
        try (InputStream in = new FileInputStream(f)) {
            byte[] data = readAllBytes(in);
            return containsEnableAutoConfigurationKey(data);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean containsSpringFactoriesInJarFile(File jarFile) throws IOException {
        try (InputStream in = new FileInputStream(jarFile)) {
            byte[] jarBytes = readAllBytes(in);
            return containsSpringFactoriesInJarBytes(jarBytes, 0);
        }
    }

    private boolean containsSpringFactoriesInJarBytes(byte[] jarBytes, int depth) throws IOException {
        if (jarBytes == null || jarBytes.length == 0) {
            return false;
        }
        if (depth > MAX_NESTED_JAR_DEPTH) {
            return false;
        }
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (SPRING_FACTORIES_PATH.equals(name) || "spring.factories".equals(name)) {
                    byte[] content = readAllBytes(zis);
                    if (containsEnableAutoConfigurationKey(content)) {
                        return true;
                    }
                    continue;
                }
                if (name.endsWith(".jar")) {
                    byte[] nested = readAllBytes(zis);
                    if (containsSpringFactoriesInJarBytes(nested, depth + 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean containsEnableAutoConfigurationKey(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }
        String text = new String(content, StandardCharsets.ISO_8859_1);
        return text.contains(ENABLE_AUTO_CONFIGURATION_KEY);
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) >= 0) {
            if (n > 0) {
                baos.write(buffer, 0, n);
            }
        }
        return baos.toByteArray();
    }

    private File resolveAsFile(Dependency dependency) {
        String targetFilePath = dependency.getTargetFilePath();
        if (targetFilePath != null && !targetFilePath.isEmpty()) {
            String normalized = targetFilePath;
            if (normalized.contains("!/")) {
                normalized = normalized.substring(0, normalized.indexOf("!/"));
            }
            File f = new File(normalized);
            if (f.exists()) {
                return f;
            }
        }
        try {
            URL locationUrl = new URL(dependency.getTarget().asLocation().getLocationExternalForm());
            if ("file".equalsIgnoreCase(locationUrl.getProtocol())) {
                File f = new File(new URI(locationUrl.toString()));
                if (f.exists()) {
                    return f;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
