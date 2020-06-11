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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.values.Values.newHeapInstance;
import static net.openhft.chronicle.values.Values.newNativeReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/*
 * Created by daniel on 11/06/2014.
 */
public class VolatileTest extends ValuesTestCommon {
    @Test
    public void testGenerateJavaCode() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

   /*     try{
            BadInterface1 jbi = dvg.heapInstance(BadInterface1.class);
            assertFalse("Should have thrown an IllegalArgumentException", true);
        }catch(AssertionError e){
            assertTrue("Throws an IllegalArgumentException", true);
        }

        try{
            BadInterface2 jbi = dvg.heapInstance(BadInterface2.class);
            assertFalse("Should have thrown an IllegalArgumentException", true);
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
            assertFalse("Throws an IllegalArgumentException", true);
        }

        //Test the native interface
        try {
            GoodInterface jbi = newNativeReference(GoodInterface.class);
            BytesStore bytes = BytesStore.wrap(ByteBuffer.allocate(64));
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
            assertFalse("Throws an IllegalArgumentException", true);
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
