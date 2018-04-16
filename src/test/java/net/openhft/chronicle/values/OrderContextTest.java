package net.openhft.chronicle.values;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static net.openhft.chronicle.values.Generators.generateNativeClass;


public class OrderContextTest {
    String classFile = "";

    @Before
    public void setup() {
        try {
            classFile = generateNativeClass(ValueModel.acquire(OrderContext.class),
                    ValueModel.simpleName(OrderContext.class) + "$$Native").trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGeneration() {
        String actual = generateNativeClass(ValueModel.acquire(OrderContext.class),
                ValueModel.simpleName(OrderContext.class) + "$$Native").trim();
        Assert.assertTrue( classFile.equals(actual));
    }


}
