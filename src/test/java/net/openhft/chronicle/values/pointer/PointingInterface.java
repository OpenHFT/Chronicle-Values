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
