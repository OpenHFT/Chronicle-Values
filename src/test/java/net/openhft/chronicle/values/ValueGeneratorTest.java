/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.LongValue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Date;

import static net.openhft.chronicle.values.Generators.generateHeapClass;
import static net.openhft.chronicle.values.Generators.generateNativeClass;
import static net.openhft.chronicle.values.Values.newHeapInstance;
import static net.openhft.chronicle.values.Values.newNativeReference;
import static org.junit.jupiter.api.Assertions.*;
import static net.openhft.compiler.CompilerUtils.CACHED_COMPILER;

/**
 * Tests code generation and serialisation routines.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ValueGeneratorTest extends ValuesTestCommon {
    @Test
    public void testGenerateJavaCode2() {
        MinimalInterface mi = newHeapInstance(MinimalInterface.class);

        mi.byte$((byte) 1);
        mi.char$('2');
        mi.short$((short) 3);
        mi.int$(4);
        mi.float$(5);
        mi.long$(6);
        mi.double$(7);
        mi.flag(true);

        assertEquals(1, mi.byte$(), "heap instance should preserve byte value through setter/getter");
        assertEquals('2', mi.char$(), "heap instance should preserve char value through setter/getter");
        assertEquals(3, mi.short$(), "heap instance should preserve short value through setter/getter");
        assertEquals(4, mi.int$(), "heap instance should preserve int value through setter/getter");
        assertEquals(5.0, mi.float$(), 0.0, "heap instance should preserve float value through setter/getter");
        assertEquals(6, mi.long$(), "heap instance should preserve long value through setter/getter");
        assertEquals(7.0, mi.double$(), 0.0, "heap instance should preserve double value through setter/getter");
        assertTrue(mi.flag(), "heap instance should preserve boolean flag through setter/getter");

        Bytes<ByteBuffer> bbb = Bytes.wrapForWrite(ByteBuffer.allocate(64));
        mi.writeMarshallable(bbb);
        System.out.println("size: " + bbb.writePosition());

        MinimalInterface mi2 = newHeapInstance(MinimalInterface.class);
        bbb.readPosition(0);
        mi2.readMarshallable(bbb);

        assertEquals(1, mi2.byte$(), "deserialized instance should preserve byte value after marshalling");
        assertEquals('2', mi2.char$(), "deserialized instance should preserve char value after marshalling");
        assertEquals(3, mi2.short$(), "deserialized instance should preserve short value after marshalling");
        assertEquals(4, mi2.int$(), "deserialized instance should preserve int value after marshalling");
        assertEquals(5.0, mi2.float$(), 0.0, "deserialized instance should preserve float value after marshalling");
        assertEquals(6, mi2.long$(), "deserialized instance should preserve long value after marshalling");
        assertEquals(7.0, mi2.double$(), 0.0, "deserialized instance should preserve double value after marshalling");
        assertTrue(mi2.flag(), "deserialized instance should preserve boolean flag after marshalling");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGenerateNativeWithGetUsing() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        String actual = generateNativeClass(ValueModel.acquire(JavaBeanInterfaceGetUsing.class),
                ValueModel.simpleName(JavaBeanInterfaceGetUsing.class) + "$$Native");
        System.out.println(actual);
        Class<?> aClass = CACHED_COMPILER.loadFromJava(
                BytecodeGen.getClassLoader(JavaBeanInterfaceGetUsing.class),
                JavaBeanInterfaceGetUsing.class.getName() + "$$Native", actual);
        JavaBeanInterfaceGetUsing jbi = aClass.asSubclass(JavaBeanInterfaceGetUsing.class).getDeclaredConstructor().newInstance();
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(64));
        ((Byteable) jbi).bytesStore(bytes, 0L, ((Byteable) jbi).maxSize());

        jbi.setString("G'day");

        assertEquals("G'day", jbi.getUsingString(new StringBuilder()).toString(), "native instance should populate provided StringBuilder with string value via getUsing method");
    }

    @Test
    public void testGenerateNativeWithGetUsingAt() throws IllegalAccessException, InstantiationException {
        JavaBeanInterfaceGetUsingAt jbi = loadNativeTypeAndCreateValue(JavaBeanInterfaceGetUsingAt.class);
        JavaBeanInterfaceGetUsingAt jbi2 = loadNativeTypeAndCreateValue(JavaBeanInterfaceGetUsingAt.class);

        LongValue val = Values.newHeapInstance(LongValue.class);
        val.setValue(2L);
        jbi.setItemAt(0, val);

        LongValue ret = jbi.getUsingItemAt(0, val);
        assertNotNull(ret, "native getUsingItemAt should return non-null value object");
        assertEquals(2L, val.getValue(), "native getUsingItemAt should retrieve previously set value at valid index");
        ret = jbi.getUsingItemAt(1, val);
        assertEquals(0L, ret.getValue(), "native getUsingItemAt should return zero for uninitialized array element");

        assertNotEquals(jbi, jbi2, "native instances with different array values should not be equal");
        val.setValue(2L);
        jbi2.setItemAt(0, val);
        assertEquals(jbi, jbi2, "native instances with identical array values should be equal");
    }

    // CPD-OFF - heap/native variants intentionally mirror each other
    @Test
    public void testGenerateHeapWithGetUsingAt() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        JavaBeanInterfaceGetUsingAt jbi = loadHeapTypeAndCreateValue(JavaBeanInterfaceGetUsingAt.class);
        JavaBeanInterfaceGetUsingAt jbi2 = loadHeapTypeAndCreateValue(JavaBeanInterfaceGetUsingAt.class);

        LongValue val = Values.newHeapInstance(LongValue.class);
        val.setValue(2L);
        jbi.setItemAt(0, val);

        LongValue ret = jbi.getUsingItemAt(0, val);
        assertNotNull(ret, "heap getUsingItemAt should return non-null value object");
        assertEquals(2L, val.getValue(), "heap getUsingItemAt should retrieve previously set value at valid index");
        ret = jbi.getUsingItemAt(1, val);
        assertEquals(0L, ret.getValue(), "heap getUsingItemAt should return zero for uninitialized array element");

        assertNotEquals(jbi, jbi2, "heap instances with different array values should not be equal");
        val.setValue(2L);
        jbi2.setItemAt(0, val);
        assertEquals(jbi, jbi2, "heap instances with identical array values should be equal");
    }
    // CPD-ON

    @Test
    public void testGenerateNativeWithGetAt() throws IllegalAccessException, InstantiationException {
        JavaBeanInterfaceGetAt jbi = loadNativeTypeAndCreateValue(JavaBeanInterfaceGetAt.class);
        JavaBeanInterfaceGetAt jbi2 = loadNativeTypeAndCreateValue(JavaBeanInterfaceGetAt.class);

        LongValue val = Values.newHeapInstance(LongValue.class);
        val.setValue(2L);
        jbi.setItemAt(0, val);

        LongValue ret = jbi.getItemAt(0);
        assertEquals(2L, ret.getValue(), "native getItemAt should retrieve previously set value at valid index");
        ret = jbi.getItemAt(1);
        assertEquals(0L, ret.getValue(), "native getItemAt should return zero for uninitialized array element");

        assertNotEquals(jbi, jbi2, "native instances with different array values should not be equal");
        jbi2.setItemAt(0, val);
        assertEquals(jbi, jbi2, "native instances with identical array values should be equal");
    }

    @Test
    public void testGenerateHeapWithGetAt() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        JavaBeanInterfaceGetAt jbi = loadHeapTypeAndCreateValue(JavaBeanInterfaceGetAt.class);
        JavaBeanInterfaceGetAt jbi2 = loadHeapTypeAndCreateValue(JavaBeanInterfaceGetAt.class);

        LongValue val = Values.newHeapInstance(LongValue.class);
        val.setValue(2L);
        jbi.setItemAt(0, val);

        LongValue ret = jbi.getItemAt(0);
        assertEquals(2L, ret.getValue(), "heap getItemAt should retrieve previously set value at valid index");
        ret = jbi.getItemAt(1);
        assertEquals(0L, ret.getValue(), "heap getItemAt should return zero for uninitialized array element");

        assertNotEquals(jbi, jbi2, "heap instances with different array values should not be equal");
        jbi2.setItemAt(0, val);
        assertEquals(jbi, jbi2, "heap instances with identical array values should be equal");
    }

    private <T> T loadNativeTypeAndCreateValue(Class<T> type) throws InstantiationException, IllegalAccessException {
        String actual = generateNativeClass(ValueModel.acquire(type),
                ValueModel.simpleName(type) + "$$Native");
        System.out.println(actual);
        Class<?> aClass = Values.nativeClassFor(type);
        T jbi;
        try {
            jbi = aClass.asSubclass(type).getConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(64));
        ((Byteable) jbi).bytesStore(bytes, 0L, ((Byteable) jbi).maxSize());
        return jbi;
    }

    private <T> T loadHeapTypeAndCreateValue(Class<T> type) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String actual = generateHeapClass(ValueModel.acquire(type),
                ValueModel.simpleName(type) + "$$Heap");
        System.out.println(actual);
        Class<T> aClass = Values.heapClassFor(type);
        return aClass.asSubclass(type).getDeclaredConstructor().newInstance();
    }

    @Test
    public void testGenerateNativeWithHasArrays() {
        HasArraysInterface hai = Values.newNativeReference(HasArraysInterface.class);
        long length = ((Byteable) hai).maxSize();
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate((int) length));
        ((Byteable) hai).bytesStore(bytes, 0L, length);

        hai.setStringAt(0, "G'day");

        assertEquals("G'day", hai.getStringAt(0), "native instance should store and retrieve string values in array at specified index");
    }

    @Test
    public void testGenerateNativeWithGetUsingHeapInstance() {
        JavaBeanInterfaceGetUsingHeap si = newHeapInstance(JavaBeanInterfaceGetUsingHeap.class);

        si.setString("G'day");

        assertEquals("G'day", si.getUsingString(new StringBuilder()).toString(), "heap instance should populate provided StringBuilder with string value via getUsing method");
    }

    @Test
    public void testStringFields() {
        StringInterface si = newHeapInstance(StringInterface.class);
        si.setString("Hello world");
        assertEquals("Hello world", si.getString(), "heap instance should preserve string value through setter/getter");

        StringInterface si2 = newNativeReference(StringInterface.class);
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(192));
        ((Byteable) si2).bytesStore(bytes, 0L, ((Byteable) si2).maxSize());
        si2.setString("Hello world \u00A3\u20AC");
        si2.setText("Hello world \u00A3\u20AC");
        assertEquals("Hello world \u00A3\u20AC", si2.getString(), "native instance should preserve string value with unicode characters");
        assertEquals("Hello world \u00A3\u20AC", si2.getText(), "native instance should preserve text value with unicode characters");
    }

    @Test
    public void testGetUsingStringFieldsWithStringBuilderHeapInstance() {
        GetUsingStringInterface si = newHeapInstance(GetUsingStringInterface.class);
        si.setSomeStringField("Hello world");
        si.setAnotherStringField("Hello world 2");
        assertEquals("Hello world", si.getSomeStringField(), "heap instance should preserve first string field value through standard getter");
        {
            StringBuilder builder = new StringBuilder();
            si.getUsingSomeStringField(builder);
            assertEquals("Hello world", builder.toString(), "heap instance should populate StringBuilder with first string field via getUsing method");
        }
        {
            StringBuilder builder = new StringBuilder();
            si.getUsingAnotherStringField(builder);
            assertEquals("Hello world 2", builder.toString(), "heap instance should populate StringBuilder with second string field via getUsing method");
        }
    }

    @Test
    public void testNested() {
        NestedB nestedB1 = newHeapInstance(NestedB.class);
        nestedB1.ask(100);
        nestedB1.bid(100);
        NestedB nestedB2 = newHeapInstance(NestedB.class);
        nestedB2.ask(91);
        nestedB2.bid(92);

        NestedA nestedA = newNativeReference(NestedA.class);
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(192));
        ((Byteable) nestedA).bytesStore(bytes, 0L, ((Byteable) nestedA).maxSize());
        nestedA.key("key");
        nestedA.one(nestedB1);
        nestedA.two(nestedB2);
        assertEquals("key", nestedA.key(), "native instance should preserve string key in nested structure");
        assertEquals(nestedB1.ask(), nestedA.one().ask(), 0.0, "nested object should preserve ask value from first assigned instance");
        assertEquals(nestedB1.bid(), nestedA.one().bid(), 0.0, "nested object should preserve bid value from first assigned instance");
        assertEquals(nestedB2.ask(), nestedA.two().ask(), 0.0, "nested object should preserve ask value from second assigned instance");
        assertEquals(nestedB2.bid(), nestedA.two().bid(), 0.0, "nested object should preserve bid value from second assigned instance");
        assertEquals(nestedB1, nestedA.one(), "first nested object should be equal to its source instance");
        assertEquals(nestedB2, nestedA.two(), "second nested object should be equal to its source instance");
        assertEquals(nestedB1.hashCode(), nestedA.one().hashCode(), "first nested object should have same hashCode as its source instance");
        assertEquals(nestedB2.hashCode(), nestedA.two().hashCode(), "second nested object should have same hashCode as its source instance");
    }

    @Test
    public void testGenerateInterfaceWithEnumOnHeap() {
        JavaBeanInterfaceGetMyEnum jbie = newHeapInstance(JavaBeanInterfaceGetMyEnum.class);
        jbie.setMyEnum(MyEnum.B);
        assertEquals(MyEnum.B, jbie.getMyEnum(), "heap instance should preserve enum value through setter/getter");
    }

    @Test
    public void testGenerateInterfaceWithEnumNativeInstance() {
        JavaBeanInterfaceGetMyEnum jbie = newNativeReference(JavaBeanInterfaceGetMyEnum.class);
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(64));
        ((Byteable) jbie).bytesStore(bytes, 0L, ((Byteable) jbie).maxSize());
        jbie.setMyEnum(MyEnum.C);
        assertEquals(MyEnum.C, jbie.getMyEnum(), "native instance should preserve enum value through setter/getter");
    }

    @Test
    public void testGenerateInterfaceWithDateOnHeap() {
        //dvg.setDumpCode(true);
        JavaBeanInterfaceGetDate jbid = newHeapInstance(JavaBeanInterfaceGetDate.class);
        Date date = new Date();
        jbid.setDate(date);
        assertEquals(date, jbid.getDate(), "heap instance should preserve Date object through setter/getter");
    }

    @Test
    public void testGenerateInterfaceWithDateNativeInstace() {
        //dvg.setDumpCode(true);
        JavaBeanInterfaceGetDate jbid = newNativeReference(JavaBeanInterfaceGetDate.class);
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(64));
        ((Byteable) jbid).bytesStore(bytes, 0L, ((Byteable) jbid).maxSize());
        Date date = new Date();
        jbid.setDate(date);
        assertEquals(date, jbid.getDate(), "native instance should preserve Date object through setter/getter");
    }

    @Test
    public void testGenerateInterfaceWithMoreThanOneEnums() {
        //dvg.setDumpCode(true);
        JavaBeanInterfaceMoreThanOneEnums jbid = newNativeReference(JavaBeanInterfaceMoreThanOneEnums.class);
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(64));
        ((Byteable) jbid).bytesStore(bytes, 0L, ((Byteable) jbid).maxSize());
        MyEnum myEnum1 = MyEnum.B;
        jbid.setMyEnum1(myEnum1);
        MyEnum myEnum2 = MyEnum.A;
        jbid.setMyEnum2(myEnum2);
        BuySell buySell = BuySell.BUY;
        jbid.setBuySell(buySell);
        assertEquals(myEnum1, jbid.getMyEnum1(), "native instance should preserve first enum value independently from other enum fields");
        assertEquals(myEnum2, jbid.getMyEnum2(), "native instance should preserve second enum value independently from other enum fields");
        assertEquals(buySell, jbid.getBuySell(), "native instance should preserve BuySell enum value independently from other enum fields");
    }
}
