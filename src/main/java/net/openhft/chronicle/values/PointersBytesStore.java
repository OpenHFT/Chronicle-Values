/*
 *      Copyright (C) 2015, 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.NativeBytesStore;

// TODO REPLACE WITH PointerBytesStore in bytes.
@Deprecated(/* to be removed in x.22*/)
public final class PointersBytesStore extends NativeBytesStore<Void> {

    private static final PointersBytesStore INSTANCE = new PointersBytesStore();

    public PointersBytesStore() {
        super(0, Long.MAX_VALUE, null, false, false);
    }

    public static NativeBytesStore<Void> instance() {
        return INSTANCE;
    }

    @Override
    public void setAddress(long address) {
        this.address = address;
    }
}
