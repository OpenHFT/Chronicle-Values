package net.openhft.chronicle.values;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
class MethodTemplates {
    final Function<Method, Class> fieldType;
    final BiConsumer<Method, FieldModel> templateExtractor;
    final BiConsumer<FieldModel, JavaCodeModel> javaCodeGenerator;
    final BiConsumer<FieldModel, ByteCodeModel> byteCodeGenerator;

    MethodTemplates(Function<Method, Class> fieldType,
                    BiConsumer<Method, FieldModel> templateExtractor,
                    BiConsumer<FieldModel, JavaCodeModel> javaCodeGenerator,
                    BiConsumer<FieldModel, ByteCodeModel> byteCodeGenerator) {
        this.fieldType = fieldType;
        this.templateExtractor = templateExtractor;
        this.javaCodeGenerator = javaCodeGenerator;
        this.byteCodeGenerator = byteCodeGenerator;
    }
}
