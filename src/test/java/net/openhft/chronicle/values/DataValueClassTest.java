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
