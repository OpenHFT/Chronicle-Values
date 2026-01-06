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
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Created by daniel on 11/06/2014.
 */
public class VolatileTest extends ValuesTestCommon {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testGenerateJavaCode() {

        //Test the heap interface
        GoodInterface jbi = newHeapInstance(GoodInterface.class);

        jbi.setOrderedY(5);
        assertEquals(5, jbi.getVolatileY(), "heap: volatile Y after setOrderedY");
        jbi.setOrderedIntAt(0, 0);
        jbi.setOrderedIntAt(1, 1);
        jbi.setOrderedIntAt(2, 2);
        jbi.setOrderedIntAt(3, 3);

        assertEquals(0, jbi.getVolatileIntAt(0), "heap: volatileInt[0]");
        assertEquals(1, jbi.getVolatileIntAt(1), "heap: volatileInt[1]");
        assertEquals(2, jbi.getVolatileIntAt(2), "heap: volatileInt[2]");
        assertEquals(3, jbi.getVolatileIntAt(3), "heap: volatileInt[3]");

        //Test the native interface
        GoodInterface jbi2 = newNativeReference(GoodInterface.class);
        BytesStore<?, ByteBuffer> bytes = BytesStore.wrap(ByteBuffer.allocate(64));
        ((Byteable) jbi2).bytesStore(bytes, 0L, ((Byteable) jbi2).maxSize());

        jbi2.setOrderedY(5);
        assertEquals(5, jbi2.getVolatileY(), "native: volatile Y after setOrderedY");
        jbi2.setOrderedIntAt(0, 0);
        jbi2.setOrderedIntAt(1, 1);
        jbi2.setOrderedIntAt(2, 2);
        jbi2.setOrderedIntAt(3, 3);

        assertEquals(0, jbi2.getVolatileIntAt(0), "native: volatileInt[0]");
        assertEquals(1, jbi2.getVolatileIntAt(1), "native: volatileInt[1]");
        assertEquals(2, jbi2.getVolatileIntAt(2), "native: volatileInt[2]");
        assertEquals(3, jbi2.getVolatileIntAt(3), "native: volatileInt[3]");
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
