/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.values.Values.newHeapInstance;
import static net.openhft.chronicle.values.Values.newNativeReference;
import static org.junit.jupiter.api.Assertions.*;

/*
 * Created by daniel on 11/06/2014.
 */
public class VolatileTest extends ValuesTestCommon {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testGenerateJavaCode() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

   /*     try{
            BadInterface1 jbi = dvg.heapInstance(BadInterface1.class);
            assertFalse(true, "Should have thrown an IllegalArgumentException");
        }catch(AssertionError e){
            assertTrue("Throws an IllegalArgumentException", true);
        }

        try{
            BadInterface2 jbi = dvg.heapInstance(BadInterface2.class);
            assertFalse(true, "Should have thrown an IllegalArgumentException");
        }catch(AssertionError e){
            assertTrue("Throws an IllegalArgumentException", true);
        }
*/
        //Test the heap interface
        try {
            GoodInterface jbi = newHeapInstance(GoodInterface.class);

            jbi.setOrderedY(5);
            assertEquals(5, jbi.getVolatileY());
            jbi.setOrderedIntAt(0, 0);
            jbi.setOrderedIntAt(1, 1);
            jbi.setOrderedIntAt(2, 2);
            jbi.setOrderedIntAt(3, 3);

            assertEquals(0, jbi.getVolatileIntAt(0));
            assertEquals(1, jbi.getVolatileIntAt(1));
            assertEquals(2, jbi.getVolatileIntAt(2));
            assertEquals(3, jbi.getVolatileIntAt(3));
        } catch (AssertionError e) {
            e.printStackTrace();
            assertFalse(true, "Throws an IllegalArgumentException");
        }

        //Test the native interface
        try {
            GoodInterface jbi = newNativeReference(GoodInterface.class);
            BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(64));
            ((Byteable) jbi).bytesStore(bytes, 0L, ((Byteable) jbi).maxSize());

            jbi.setOrderedY(5);
            assertEquals(5, jbi.getVolatileY());
            jbi.setOrderedIntAt(0, 0);
            jbi.setOrderedIntAt(1, 1);
            jbi.setOrderedIntAt(2, 2);
            jbi.setOrderedIntAt(3, 3);

            assertEquals(0, jbi.getVolatileIntAt(0));
            assertEquals(1, jbi.getVolatileIntAt(1));
            assertEquals(2, jbi.getVolatileIntAt(2));
            assertEquals(3, jbi.getVolatileIntAt(3));
        } catch (AssertionError e) {
            e.printStackTrace();
            assertFalse(true, "Throws an IllegalArgumentException");
        }
    }

    public interface BadInterface1 {
        int getX();

        void setOrderedX(int x);
    }

    public interface BadInterface2 {
        int getVolatileX();

        void setX(int x);
    }

    public interface GoodInterface {
        int getX();

        void setX(int x);

        int getVolatileY();

        void setOrderedY(int y);

        int getY();

        void setY(int y);

        @Array(length = 4)
        void setOrderedIntAt(int idx, int i);

        int getVolatileIntAt(int idx);
    }
}
