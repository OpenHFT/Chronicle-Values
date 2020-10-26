/*
 *     Copyright (C) 2015-2020 chronicle.software
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

/**
 */
public interface NestedArrayInterface {
    String getText();

    void setText(String text);

    int getIntAt(int index);

    void setIntAt(@MaxUtf8Length(16) int index, int value);

    @Array(length = 32)
    JavaBeanInterface getJBIAt(int index);

    void setJBIAt(int index, JavaBeanInterface jbi);
}
