/*
 *      Copyright (C) 2015, 2016  higherfrequencytrading.com
 *      Copyright (C) 2016 Roman Leventov
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

package net.openhft.chronicle.values;

final class Utils {

    private Utils() {
    }

    public static int roundUp(int divident, int divisor) {
        return ((divident + divisor - 1) / divisor) * divisor;
    }

    static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static String formatIntOrLong(long v) {
        if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
            return v + "";
        return v + "L";
    }
}
