/*
 *      Copyright (C) 2015  higherfrequencytrading.com
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
