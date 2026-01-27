package org.eclipse.emt4j.common;

import java.io.File;

public class DependencySourceDto {

    private final File file;

    private SourceInformation information;

    public DependencySourceDto(File file, SourceInformation  information) {
        this.file = file;
        this.information = information;
    }

    public String desc() {
        return getFile().getName();
    }

    public final File getFile() {
        return file;
    }

    public SourceInformation getInformation() {
        return information;
    }

    public void setInformation(SourceInformation information) {
        this.information = information;
    }
}
