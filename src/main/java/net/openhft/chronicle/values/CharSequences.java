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

public enum CharSequences {
    ;

    public static int hashCode(CharSequence cs) {
        if (cs == null)
            return 0;
        int h = 0;
        for (int i = 0; i < cs.length(); i++) {
            h = 31 * h + cs.charAt(i);
        }
        return h;
    }

    // to match Objects.equals(Object o1, Object o2)
    public static boolean equals(CharSequence left, CharSequence right) {
        if (left == null)
            return right == null;
        if (left.length() != right.length())
            return false;
        for (int i = 0; i < left.length(); i++) {
            if (left.charAt(i) != right.charAt(i))
                return false;
        }
        return true;
    }
}
