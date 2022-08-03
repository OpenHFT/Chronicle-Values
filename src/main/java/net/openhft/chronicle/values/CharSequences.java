/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
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

package net.openhft.chronicle.values;

public enum CharSequences {
    ; // none

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
