/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values.pointer;

import net.openhft.chronicle.values.Array;
import net.openhft.chronicle.values.Pointer;

public interface PointingInterface {

    PointedInterface getPoint();

    void setPoint(@Pointer PointedInterface point);

    PointedInterface getVolatilePoint();

    void setVolatilePoint(PointedInterface point);

    void setOrderedPoint(PointedInterface point);

    boolean compareAndSwapPoint(PointedInterface oldPoint, PointedInterface newPoint);

    PointedInterface getPointArrayAt(int index);

    PointedInterface getVolatilePointArrayAt(int index);

    @Array(length = 2)
    void setPointArrayAt(int index, @Pointer PointedInterface point);

    void setVolatilePointArrayAt(int index, PointedInterface point);

    void setOrderedPointArrayAt(int index, PointedInterface point);

    boolean compareAndSwapPointArrayAt(
            int index, PointedInterface oldPoint, PointedInterface newPoint);
}
