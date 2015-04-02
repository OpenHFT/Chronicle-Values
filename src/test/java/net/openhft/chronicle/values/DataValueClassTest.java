/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static java.util.Arrays.asList;
import static net.openhft.chronicle.values.CodeTemplate.direct;
import static net.openhft.chronicle.values.CodeTemplate.heap;
import static org.junit.Assert.assertNotNull;

/**
 * Created by peter.lawrey on 03/03/2015.
 */

@RunWith(value = Parameterized.class)
public class DataValueClassTest {
    final CodeTemplate template;

    public DataValueClassTest(CodeTemplate template) {
        this.template = template;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
//                {heap().generateJava(false)},
//                {direct().generateJava(false)},
                {heap().generateJava(true)},
                {direct().generateJava(true)},
        });
    }

    @Test
    @Ignore
    public void instances() {
        JavaBeanInterface jbi = template.newInstance(JavaBeanInterface.class);
        assertNotNull(jbi);
        NestedInterface ni = template.newInstance(NestedInterface.class);
        assertNotNull(ni);
    }
}
