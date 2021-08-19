/*
 * Copyright 2016-2021 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.junit.Test;

import static net.openhft.chronicle.values.Values.newNativeReference;

public class AlignTest extends ValuesTestCommon {
    @Test
    public void testAlign() {
        DemoOrderVOInterface value = newNativeReference(DemoOrderVOInterface.class);
        long size = value.maxSize();
        BytesStore<?, Void> bs = BytesStore.nativeStore(size);
        value.bytesStore(bs, 0, size);
        value.addAtomicOrderQty(10.0);
        System.out.println(value);
        bs.releaseLast();
    }

    interface DemoOrderVOInterface extends Byteable {
        public CharSequence getSymbol();
//    public StringBuilder getUsingSymbol(StringBuilder sb);

        public void setSymbol(@MaxUtf8Length(20) CharSequence symbol);

        public double addAtomicOrderQty(double toAdd);

        public double getOrderQty();

        public void setOrderQty(double orderQty);

    }
}
