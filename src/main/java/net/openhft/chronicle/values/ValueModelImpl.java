/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.values;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.values.constraints.Group;
import net.openhft.chronicle.values.constraints.MaxSize;
import net.openhft.chronicle.values.constraints.Range;

import java.io.Externalizable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * User: peter.lawrey Date: 06/10/13private static final int VALUE Time: 17:23
 */
class ValueModelImpl<T> implements ValueModel<T> {
    private static final Map<Class, Integer> HEAP_SIZE_MAP = new HashMap<>();
    private static final String VOLATILE_GETTER_PREFIX = "volatile";
    private static final String ORDERED_SETTER_PREFIX = "ordered";

    static {
        HEAP_SIZE_MAP.put(boolean.class, 1);
        HEAP_SIZE_MAP.put(byte.class, 8);
        HEAP_SIZE_MAP.put(char.class, 16);
        HEAP_SIZE_MAP.put(short.class, 16);
        HEAP_SIZE_MAP.put(int.class, 32);
        HEAP_SIZE_MAP.put(float.class, 32);
        HEAP_SIZE_MAP.put(long.class, 64);
        HEAP_SIZE_MAP.put(double.class, 64);
        HEAP_SIZE_MAP.put(Date.class, 64);
    }

    private final Map<String, FieldModelImpl> fieldModelMap = new TreeMap<String, FieldModelImpl>();
    private final Class<T> type;
    private final Map<Class, ValueModel> nestedMap = new HashMap<Class, ValueModel>();

    public ValueModelImpl(Class<T> type) {
        this.type = type;

        if (!type.isInterface())
            throw new IllegalArgumentException("type must be an interface, was " + type);

        Method[] methods = type.getMethods();
        for (Method method : methods) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class
                    || declaringClass == Externalizable.class
                    || declaringClass == net.openhft.chronicle.bytes.BytesMarshallable.class
                    || declaringClass == Copyable.class
                    || declaringClass == net.openhft.chronicle.bytes.Byteable.class)
                continue;

            // ignore the default or static methods
            if(isMethodDefaultOrStatic(method))
                continue;

            String name = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            final Class<?> returnType = method.getReturnType();
            switch (parameterTypes.length) {
                case 0: {
                    String name6 = getSizeOf(name);
                    if (name6 != null && returnType == int.class) {
                        FieldModelImpl fm = acquireField(name6);
                        fm.sizeOf(method);
                        break;
                    }
                    if (returnType == void.class)
                        throw new IllegalArgumentException("void () not supported " + method);

                    String name2 = getGetter(name, returnType);
                    if (isVolatileGetter(name2)) {
                        FieldModelImpl fm = acquireField(volatileGetterFieldName(name2));
                        fm.volatileGetter(method);
                        fm.setVolatile(true);

                    } else {
                        FieldModelImpl fm = acquireField(name2);
                        fm.getter(method);
                    }

                    break;
                }

                case 1: {

                    String name7 = getUsing(name, method);
                    if (name7 != null) {
                        FieldModelImpl fm = acquireField(name7);
                        fm.getUsing(method);
                        break;
                    }

                    String name4 = getAtomicAdder(name);
                    if (name4 != null) {
                        FieldModelImpl fm = acquireField(name4);
                        fm.atomicAdder(method);
                        break;
                    }

                    String name3 = getAdder(name);
                    if (name3 != null) {
                        FieldModelImpl fm = acquireField(name3);
                        fm.adder(method);
                        break;
                    }

                    String name6 = getGetterAt(name, returnType);
                    if (name6 != null && parameterTypes[0] == int.class && returnType != void.class) {
                        if (isVolatileGetter(name6)) {
                            FieldModelImpl fm = acquireField(volatileGetterFieldName(name6));
                            fm.volatileIndexedGetter(method);
                            fm.setVolatile(true);

                        } else {
                            FieldModelImpl fm = acquireField(name6);
                            fm.indexedGetter(method);
                        }
                        break;
                    }

                    if (returnType != void.class)
                        throw new IllegalArgumentException("setter must be void " + method);

                    String name2 = getSetter(name);
                    if (isOrderedSetter(name2)) {
                        FieldModelImpl fm = acquireField(orderedSetterFieldName(name2));
                        fm.orderedSetter(method);

                    } else {
                        FieldModelImpl fm = acquireField(name2);
                        fm.setter(method);
                    }
                    break;
                }

                case 2: {
                    String name2 = getCAS(name);
                    if (name2 != null && returnType == boolean.class) {
                        FieldModelImpl fm = acquireField(name2);
                        fm.cas(method);
                        break;
                    }
                    String name3 = getSetterAt(name);
                    if (name3 != null && parameterTypes[0] == int.class && returnType == void.class) {
                        if (isOrderedSetter(name3)) {
                            FieldModelImpl fm = acquireField(orderedSetterFieldName(name3));
                            fm.orderedIndexedSetter(method);

                        } else {
                            FieldModelImpl fm = acquireField(name3);
                            fm.indexedSetter(method);
                        }
                        break;
                    }
                }

                default: {
                    throw new IllegalArgumentException("method not supported " + method);
                }
            }
        }

        for (Map.Entry<String, FieldModelImpl> entry : fieldModelMap.entrySet()) {
            FieldModelImpl model = entry.getValue();
            if ((model.getter() == null && model.getUsing() == null) || (model.setter() == null && model
                    .getter()
                    .getReturnType()
                    .isPrimitive()))
                if (model.volatileGetter() == null || (model.orderedSetter() == null && model.volatileGetter().getReturnType().isPrimitive()))
                    if (model.indexedGetter() == null || (model.indexedSetter() == null && model.indexedGetter().getReturnType().isPrimitive()))
                        if (model.volatileIndexedGetter() == null || (model.orderedIndexedSetter() == null && model.volatileIndexedGetter().getReturnType().isPrimitive()))
                            throw new IllegalArgumentException("Field " + entry.getKey() + " must have a getter & setter, or getAt & setAt.");
            if (model.indexedGetter() != null || model.indexedSetter() != null)
                if (model.indexSize() == null)
                    throw new IllegalStateException("You must set a MaxSize for the range of the index for the getter or setter");

            Class ftype = model.type();
            if (!isScalar(ftype) && !nestedMap.containsKey(ftype))
                nestedMap.put(ftype, new ValueModelImpl(ftype));
        }
    }

    public static int heapSize(Class primitiveType) {
        if (!primitiveType.isPrimitive())
            throw new IllegalArgumentException();
        return (int) Maths.divideRoundUp(HEAP_SIZE_MAP.get(primitiveType), 8);
    }

    private static String getCAS(String name) {
        final int len = 14;
        if (name.length() > len && name.startsWith("compareAndSwap") && Character.isUpperCase(name.charAt(len)))
            return Character.toLowerCase(name.charAt(len)) + name.substring(len + 1);
        return null;
    }

    private static String getSizeOf(String name) {
        final int len = 6;
        if (name.length() > len && name.startsWith("sizeOf") && Character.isUpperCase(name.charAt(len)))
            return Character.toLowerCase(name.charAt(len)) + name.substring(len + 1);
        return null;
    }

    private static String getAtomicAdder(String name) {
        final int len = 9;
        if (name.length() > len && name.startsWith("addAtomic") && Character.isUpperCase(name.charAt(len)))
            return Character.toLowerCase(name.charAt(len)) + name.substring(len + 1);
        return null;
    }

    private static String getAdder(String name) {
        final int len = 3;
        if (name.length() > len && name.startsWith("add") && Character.isUpperCase(name.charAt(len)))
            return Character.toLowerCase(name.charAt(len)) + name.substring(len + 1);
        return null;
    }

    private static String getSetter(String name) {
        final int len = 3;
        if (name.length() > len && name.startsWith("set") && Character.isUpperCase(name.charAt(len)))
            return Character.toLowerCase(name.charAt(len)) + name.substring(len + 1);
        return name;
    }

    private static String getSetterAt(String name) {
        final int len = 3;
        final int len2 = 2;
        if (name.length() > len + len2 && name.startsWith("set") && Character.isUpperCase(
                name.charAt(len)) && name.endsWith("At"))
            return Character.toLowerCase(name.charAt(len)) + name.substring(len + 1, name.length() - len2);
        return name;
    }

    private static String getGetter(String name, Class returnType) {
        if (name.length() > 3 && name.startsWith("get") && Character.isUpperCase(name.charAt(3)))
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        if ((returnType == boolean.class || returnType == Boolean.class)
                && name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2)))
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        return name;
    }

    private static String getUsing(String name, Method method) {
        Class<?> returnType = method.getReturnType();
        if (method.getParameterTypes().length != 1)
            return null;

        Class<?> parameter = method.getParameterTypes()[0];

        if ((returnType == StringBuilder.class || returnType == void.class) && parameter ==
                StringBuilder.class &&
                name.length() > "getUsing".length() && name.startsWith
                ("getUsing") && Character.isUpperCase(name.charAt("getUsing".length())))
            return Character.toLowerCase(name.charAt("getUsing".length())) + name.substring("getUsing"
                    .length() + 1);
        return null;
    }

    private static String getGetterAt(String name, Class returnType) {
        final int len = 3;
        final int len2 = 2;
        if (name.length() > len + len2 && name.startsWith("get") && Character.isUpperCase(
                name.charAt(len)) && name.endsWith("At"))
            return Character.toLowerCase(name.charAt(len)) + name.substring(4, name.length() - len2);
        return name;
    }

    public boolean isMethodDefaultOrStatic(Method method) {
        return ((method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC)) ==
                Modifier.PUBLIC) && method.getDeclaringClass().isInterface();
    }

    private boolean isOrderedSetter(String name2) {
        return name2.startsWith(ORDERED_SETTER_PREFIX) ? true : false;
    }

    private boolean isVolatileGetter(String name2) {
        return name2.startsWith(VOLATILE_GETTER_PREFIX) ? true : false;
    }

    private String volatileGetterFieldName(String name) {
        name = name.substring(VOLATILE_GETTER_PREFIX.length());
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private String orderedSetterFieldName(String name) {
        name = name.substring(ORDERED_SETTER_PREFIX.length());
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private FieldModelImpl acquireField(String name) {
        FieldModelImpl fieldModelImpl = fieldModelMap.get(name);
        if (fieldModelImpl == null)
            fieldModelMap.put(name, fieldModelImpl = new FieldModelImpl(name));

        return fieldModelImpl;
    }

    @Override
    public Map<String, ? extends FieldModel> fieldMap() {
        return fieldModelMap;
    }

    public boolean isScalar(Class type) {
        return type.isPrimitive() || CharSequence.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type) || Date.class == type;
    }

    @Override
    public Set<Class> nestedModels() {
        return nestedMap.keySet();
    }

    @Override
    public <N> ValueModel<N> nestedModel(Class<N> nClass) {
        @SuppressWarnings("unchecked")
        ValueModel<N> model = (ValueModel<N>) (nClass == type ? this : nestedMap.get(nClass));
        return model;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    static class FieldModelImpl<T> implements FieldModel<T> {

        private final String name;
        private Method getter, setter;
        private Method volatileGetter;
        private Method orderedSetter;
        private Range range;
        private MaxSize maxSize;
        private Group group;
        private MaxSize indexSize;
        private Method adder;
        private Method atomicAdder;
        private Method getUsing;
        private Method cas;
        private Method getterAt;
        private Method setterAt;
        private Method volatileGetterAt;
        private Method orderedSetterAt;
        private Method sizeOf;
        private boolean isArray = false;
        private boolean isVolatile = false;

        public FieldModelImpl(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public boolean isArray() {
            return isArray;
        }

        @Override
        public boolean isVolatile() {
            return isVolatile;
        }

        @Override
        public void setVolatile(boolean isVolatile) {
            this.isVolatile = isVolatile;
        }

        public void getter(Method getter) {
            this.getter = getter;
        }

        public Method getter() {
            return getter;
        }

        public void setter(Method setter) {
            this.setter = setter;
            for (Annotation a : setter.getParameterAnnotations()[0]) {
                if (a instanceof Range)
                    range = (Range) a;
                if (a instanceof MaxSize)
                    maxSize = (MaxSize) a;
            }

            for (Annotation a : setter.getAnnotations()) {
                if (a instanceof Group)
                    group = (Group) a;
            }
        }
        public Method setter() {
            return setter;
        }

        public void volatileGetter(Method volatileGetter) {
            this.volatileGetter = volatileGetter;
        }

        public Method volatileGetter() {
            return volatileGetter;
        }

        public void orderedSetter(Method orderedSetter) {
            this.orderedSetter = orderedSetter;
            for (Annotation a : orderedSetter.getParameterAnnotations()[0]) {
                if (a instanceof Range)
                    range = (Range) a;
                if (a instanceof MaxSize)
                    maxSize = (MaxSize) a;
            }

            for (Annotation a : orderedSetter.getAnnotations()) {
                if (a instanceof Group)
                    group = (Group) a;
            }
        }

        public Method orderedSetter() {
            return orderedSetter;
        }

        @Override
        public Class<T> type() {
            return (Class<T>) (getter != null ? getter.getReturnType() :
                    volatileGetter != null ? volatileGetter.getReturnType() :
                            getterAt != null ? getterAt.getReturnType() :
                                    volatileGetterAt != null ? volatileGetterAt.getReturnType() :
                                            setter != null && setter.getParameterTypes().length == 1 ?
                                                            setter.getParameterTypes()[0] : null);
        }

        public void adder(Method method) {
            adder = method;
        }

        public Method adder() {
            return adder;
        }

        @Override
        public int heapSize() {
            Integer size = HEAP_SIZE_MAP.get(type());
            if (size == null) return -1;
            return size;
        }

        // maxSize in bits.
        @Override
        public int nativeSize() {
            Class<T> type = type();
            Integer size = HEAP_SIZE_MAP.get(type);
            if (Enum.class.isAssignableFrom(type))
                return smallEnum(type) ? 8 : 16;
            if (size != null)
                return size;
            return size().value() << 3;
        }

        @Override
        public Range range() {
            return range;
        }

        @Override
        public MaxSize size() {
            if (maxSize == null)
                throw new IllegalStateException("Field " + name + " is missing @MaxSize on the setter");
            return maxSize;
        }

        @Override
        public Group group() {
            return group;
        }

        @Override
        public String toString() {
            return "FieldModel{" +
                    "name='" + name + '\'' +
                    ", getter=" + (getterAt != null ? getterAt : getter) +
                    ", setter=" + (setterAt != null ? setterAt : setter) +
                    (range == null ? "" : ", range= " + range) +
                    (maxSize == null ? "" : ", size= " + maxSize) +
                    ((getterAt == null && setterAt == null) ? "" : ", indexSize= " + indexSize.toString().replace("@net.openhft.lang.values.constraints.", "")) +
                    '}';
        }

        public void atomicAdder(Method method) {
            atomicAdder = method;
        }

        public void getUsing(Method method) {
            getUsing = method;
        }

        public Method atomicAdder() {
            return atomicAdder;
        }

        public void cas(Method method) {
            cas = method;
        }

        public Method cas() {
            return cas;
        }

        public void sizeOf(Method method) {
            sizeOf = method;
        }

        public Method sizeOf() {
            return sizeOf;
        }

        public void indexSize(MaxSize indexSize) {
            if (indexSize != null)
                this.indexSize = indexSize;
        }

        public MaxSize indexSize() {
            return indexSize;
        }

        public void indexedGetter(Method indexedGetter) {
            isArray = true;
            this.getterAt = indexedGetter;
            indexAnnotations(indexedGetter);
        }

        public Method indexedGetter() {
            return getterAt;
        }

        public void indexedSetter(Method indexedSetter) {
            isArray = true;
            this.setterAt = indexedSetter;
            indexAnnotations(indexedSetter);
        }

        public Method indexedSetter() {
            return setterAt;
        }

        public void indexAnnotations(Method method) {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (Annotation a : parameterAnnotations[0]) {
//                if (a instanceof Range)
//                    range = (Range) a;
                if (a instanceof MaxSize)
                    indexSize = (MaxSize) a;
            }
            if( parameterAnnotations.length > 1 ) {
                for (Annotation a : parameterAnnotations[1]) {
                    if (a instanceof Range)
                        range = (Range) a;
                    if (a instanceof MaxSize)
                        maxSize = (MaxSize) a;
                }
            }
        }

        public void volatileIndexedGetter(Method volatileIndexedGetter) {
            isArray = true;
            this.volatileGetterAt = volatileIndexedGetter;
            indexAnnotations(volatileIndexedGetter);
        }

        public Method volatileIndexedGetter() {
            return volatileGetterAt;
        }

        public void orderedIndexedSetter(Method orderedIndexedSetter) {
            isArray = true;
            this.orderedSetterAt = orderedIndexedSetter;
            indexAnnotations(orderedIndexedSetter);
        }

        public Method orderedIndexedSetter() {
            return orderedSetterAt;
        }

        @Override
        public Method getUsing() {
            return getUsing;
        }
    }

    static boolean smallEnum(Class<?> enumType) {
        return EnumSet.allOf((Class<Enum>) enumType).size() <= 256;
    }
}
