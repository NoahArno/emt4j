package org.eclipse.emt4j.common.classanalyze;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PackageUsageScanner {

    private final List<String> internalPackagePrefixes;
    private final List<String> dottedPackagePrefixes;

    public PackageUsageScanner(Collection<String> dottedPackagePrefixes) {
        this.dottedPackagePrefixes = new ArrayList<>();
        this.internalPackagePrefixes = new ArrayList<>();
        if (dottedPackagePrefixes != null) {
            for (String p : dottedPackagePrefixes) {
                if (p == null) {
                    continue;
                }
                String trimmed = p.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                this.dottedPackagePrefixes.add(trimmed);
                this.internalPackagePrefixes.add(trimmed.replace('.', '/'));
            }
        }
    }

    public boolean containsAny(byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return false;
        }
        if (internalPackagePrefixes.isEmpty()) {
            return false;
        }
        FoundFlag foundFlag = new FoundFlag();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(new ScanClassVisitor(foundFlag), 0);
        return foundFlag.found;
    }

    private boolean matchInternalName(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return false;
        }
        for (String prefix : internalPackagePrefixes) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchDescriptor(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return false;
        }
        for (String prefix : internalPackagePrefixes) {
            if (descriptor.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String prefix : internalPackagePrefixes) {
            if (text.contains(prefix)) {
                return true;
            }
        }
        for (String prefix : dottedPackagePrefixes) {
            if (text.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void scanSignatureIfPresent(String signature, FoundFlag foundFlag) {
        if (foundFlag.found) {
            return;
        }
        if (signature == null || signature.isEmpty()) {
            return;
        }
        try {
            SignatureReader signatureReader = new SignatureReader(signature);
            signatureReader.accept(new ScanSignatureVisitor(foundFlag));
        } catch (Throwable ignored) {
        }
    }

    private static final class FoundFlag {
        private boolean found;
    }

    private final class ScanSignatureVisitor extends SignatureVisitor {
        private final FoundFlag foundFlag;

        private ScanSignatureVisitor(FoundFlag foundFlag) {
            super(Opcodes.ASM9);
            this.foundFlag = foundFlag;
        }

        @Override
        public void visitClassType(String name) {
            if (!foundFlag.found && matchInternalName(name)) {
                foundFlag.found = true;
            }
            super.visitClassType(name);
        }
    }

    private final class ScanClassVisitor extends ClassVisitor {
        private final FoundFlag foundFlag;

        private ScanClassVisitor(FoundFlag foundFlag) {
            super(Opcodes.ASM9);
            this.foundFlag = foundFlag;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (!foundFlag.found) {
                if (matchInternalName(name) || matchInternalName(superName)) {
                    foundFlag.found = true;
                }
            }
            if (!foundFlag.found && interfaces != null) {
                for (String itf : interfaces) {
                    if (matchInternalName(itf)) {
                        foundFlag.found = true;
                        break;
                    }
                }
            }
            scanSignatureIfPresent(signature, foundFlag);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (!foundFlag.found) {
                if (matchDescriptor(descriptor)) {
                    foundFlag.found = true;
                } else if (value instanceof String && matchText((String) value)) {
                    foundFlag.found = true;
                } else if (value instanceof Type && matchDescriptor(((Type) value).getDescriptor())) {
                    foundFlag.found = true;
                }
            }
            scanSignatureIfPresent(signature, foundFlag);
            FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
            return new ScanFieldVisitor(fv, foundFlag);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (!foundFlag.found) {
                if (matchDescriptor(descriptor)) {
                    foundFlag.found = true;
                }
            }
            scanSignatureIfPresent(signature, foundFlag);
            if (!foundFlag.found && exceptions != null) {
                for (String ex : exceptions) {
                    if (matchInternalName(ex)) {
                        foundFlag.found = true;
                        break;
                    }
                }
            }
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new ScanMethodVisitor(mv, foundFlag);
        }

        @Override
        public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            scanSignatureIfPresent(signature, foundFlag);
            RecordComponentVisitor rcv = super.visitRecordComponent(name, descriptor, signature);
            return new ScanRecordComponentVisitor(rcv, foundFlag);
        }
    }

    private final class ScanRecordComponentVisitor extends RecordComponentVisitor {
        private final FoundFlag foundFlag;

        private ScanRecordComponentVisitor(RecordComponentVisitor recordComponentVisitor, FoundFlag foundFlag) {
            super(Opcodes.ASM9, recordComponentVisitor);
            this.foundFlag = foundFlag;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
    }

    private final class ScanFieldVisitor extends FieldVisitor {
        private final FoundFlag foundFlag;

        private ScanFieldVisitor(FieldVisitor fv, FoundFlag foundFlag) {
            super(Opcodes.ASM9, fv);
            this.foundFlag = foundFlag;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
    }

    private final class ScanMethodVisitor extends MethodVisitor {
        private final FoundFlag foundFlag;

        private ScanMethodVisitor(MethodVisitor mv, FoundFlag foundFlag) {
            super(Opcodes.ASM9, mv);
            this.foundFlag = foundFlag;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            return super.visitParameterAnnotation(parameter, descriptor, visible);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (!foundFlag.found && matchInternalName(type)) {
                foundFlag.found = true;
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (!foundFlag.found) {
                if (matchInternalName(owner) || matchDescriptor(descriptor)) {
                    foundFlag.found = true;
                }
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (!foundFlag.found) {
                if (matchInternalName(owner) || matchDescriptor(descriptor)) {
                    foundFlag.found = true;
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            if (!foundFlag.found) {
                if (bootstrapMethodHandle != null) {
                    if (matchInternalName(bootstrapMethodHandle.getOwner()) || matchDescriptor(bootstrapMethodHandle.getDesc())) {
                        foundFlag.found = true;
                    }
                }
            }
            if (!foundFlag.found && bootstrapMethodArguments != null) {
                for (Object arg : bootstrapMethodArguments) {
                    if (foundFlag.found) {
                        break;
                    }
                    scanConstant(arg, foundFlag);
                }
            }
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitLdcInsn(Object value) {
            scanConstant(value, foundFlag);
            super.visitLdcInsn(value);
        }

        @Override
        public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end, org.objectweb.asm.Label handler, String type) {
            if (!foundFlag.found && matchInternalName(type)) {
                foundFlag.found = true;
            }
            super.visitTryCatchBlock(start, end, handler, type);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            super.visitMultiANewArrayInsn(descriptor, numDimensions);
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, org.objectweb.asm.Label start, org.objectweb.asm.Label end, int index) {
            if (!foundFlag.found && matchDescriptor(descriptor)) {
                foundFlag.found = true;
            }
            scanSignatureIfPresent(signature, foundFlag);
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
        }

        private void scanConstant(Object value, FoundFlag foundFlag) {
            if (foundFlag.found || value == null) {
                return;
            }
            if (value instanceof String) {
                if (matchText((String) value)) {
                    foundFlag.found = true;
                }
                return;
            }
            if (value instanceof Type) {
                if (matchDescriptor(((Type) value).getDescriptor())) {
                    foundFlag.found = true;
                }
                return;
            }
            if (value instanceof Handle) {
                Handle h = (Handle) value;
                if (matchInternalName(h.getOwner()) || matchDescriptor(h.getDesc())) {
                    foundFlag.found = true;
                }
            }
        }
    }
}

