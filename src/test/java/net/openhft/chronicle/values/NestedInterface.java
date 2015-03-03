package net.openhft.chronicle.values;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public interface NestedInterface extends JavaBeanInterface {
    JavaBeanInterface getNestedA();

    void setNestedA(JavaBeanInterface nestedA);

    JavaBeanInterface getNestedB();

    void setNestedB(JavaBeanInterface nestedB);
}
