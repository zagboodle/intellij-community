package com.intellij.compiler.notNullVerification;

import org.objectweb.asm.*;

import java.util.ArrayList;

/**
 * @author ven
 * @noinspection HardCodedStringLiteral
 */
public class NotNullVerifyingInstrumenter extends ClassAdapter {

  private boolean myIsModification = false;
  private String myClassName;

  public NotNullVerifyingInstrumenter(final ClassVisitor classVisitor) {
    super(classVisitor);
  }

  public boolean isModification() {
    return myIsModification;
  }

  public void visit(final int version,
                    final int access,
                    final String name,
                    final String signature,
                    final String superName,
                    final String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    myClassName = name;
  }

  public MethodVisitor visitMethod(
    final int access,
    final String name,
    final String desc,
    final String signature,
    final String[] exceptions) {
    final Type[] args = Type.getArgumentTypes(desc);
    final Type returnType = Type.getReturnType(desc);
    MethodVisitor v = cv.visitMethod(access,
                                     name,
                                     desc,
                                     signature,
                                     exceptions);
    return new MethodAdapter(v) {

      private ArrayList myNotNullParams = new ArrayList();
      private boolean myIsNotNull = false;

      public AnnotationVisitor visitParameterAnnotation(
        final int parameter,
        final String anno,
        final boolean visible) {
        AnnotationVisitor av;
        av = mv.visitParameterAnnotation(parameter,
                                         anno,
                                         visible);
        if (args[parameter].getSort() == Type.OBJECT &&
            anno.equals("Lorg/jetbrains/annotations/NotNull;")) {
          myNotNullParams.add(new Integer(parameter));
        }
        return av;
      }

      public AnnotationVisitor visitAnnotation(String anno,
                                               boolean isRuntime) {
        final AnnotationVisitor av = mv.visitAnnotation(anno, isRuntime);
        if (returnType.getSort() == Type.OBJECT &&
            anno.equals("Lorg/jetbrains/annotations/NotNull;")) {
          myIsNotNull = true;
        }

        return av;
      }

      public void visitCode() {
        for (int p = 0; p < myNotNullParams.size(); ++p) {
          int var = ((access & Opcodes.ACC_STATIC) == 0) ? 1 : 0;
          int param = ((Integer)myNotNullParams.get(p)).intValue();
          for (int i = 0; i < param; ++i) {
            var += args[i].getSize();
          }
          mv.visitVarInsn(Opcodes.ALOAD, var);

          generateConditionalThrow("Argument " + param + " for @NotNull parameter of " + myClassName + "." + name + " must not be null",
                                   "java/lang/IllegalArgumentException");
        }
      }

      public void visitInsn(int opcode) {
        if (opcode == Opcodes.ARETURN && myIsNotNull) {
          mv.visitInsn(Opcodes.DUP);
          generateConditionalThrow("@NotNull method " + myClassName + "." + name + " must not return null",
                                   "java/lang/IllegalStateException");
        }

        mv.visitInsn(opcode);
      }

      private void generateConditionalThrow(final String descr, final String exceptionClass) {
        String exceptionParamClass = "(Ljava/lang/String;)V";
        Label end = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, end);

        mv.visitTypeInsn(Opcodes.NEW, exceptionClass);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(descr);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                           exceptionClass,
                           "<init>",
                           exceptionParamClass);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(end);

        myIsModification = true;
      }
    };
  }
}
