package net.openhft.chronicle.values;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static net.openhft.chronicle.values.Generators.generateNativeClass;


public class OrderContextTest {
    String classFile = "";

    @Before
    public void setup() {
        try {
            classFile = read("OrderContext1.log").trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGeneration() {
        String actual = generateNativeClass(ValueModel.acquire(OrderContext.class),
                ValueModel.simpleName(OrderContext.class) + "$$Native").trim();
        Assert.assertTrue( classFile.equals(actual));
    }

    /**
     * The static function to read the given orders
     *
     * @param filename
     * @return
     * @throws IOException
     */
    private static String read(String filename) throws IOException {
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
        StringBuilder classFile = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            classFile.append(line).append("\n");
        }
        bufferedReader.close();
        return classFile.toString();
    }

}
