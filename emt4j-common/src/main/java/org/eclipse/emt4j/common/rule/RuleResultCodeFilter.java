package org.eclipse.emt4j.common.rule;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class RuleResultCodeFilter {
    private final Set<String> enable;
    private final Set<String> disable;

    public RuleResultCodeFilter(String enableCsv, String disableCsv) {
        this.enable = parseCsv(enableCsv);
        this.disable = parseCsv(disableCsv);
    }

    public boolean accept(String resultCode) {
        if (resultCode == null) {
            return false;
        }
        if (!enable.isEmpty() && !enable.contains(resultCode)) {
            return false;
        }
        return !disable.contains(resultCode);
    }

    public boolean isDisabledByConfig(String resultCode) {
        if (resultCode == null) {
            return false;
        }
        return disable.contains(resultCode) || (!enable.isEmpty() && !enable.contains(resultCode));
    }

    public Set<String> getEnable() {
        return Collections.unmodifiableSet(enable);
    }

    public Set<String> getDisable() {
        return Collections.unmodifiableSet(disable);
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null) {
            return Collections.emptySet();
        }
        String trimmed = csv.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String s : trimmed.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) {
                out.add(v);
            }
        }
        return out;
    }
}

