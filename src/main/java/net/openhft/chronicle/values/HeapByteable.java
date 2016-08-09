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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;

/**
 * NOT FOR DIRECT USE. Implements {@link Byteable} by throwing {@link UnsupportedOperationException}
 * from all methods.
 */
public interface HeapByteable extends Byteable {

    @Override
    default void bytesStore(BytesStore bytesStore, long l, long l1) {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    @Override
    default BytesStore bytesStore() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    @Override
    default long offset() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }

    @Override
    default long maxSize() {
        throw new UnsupportedOperationException(getClass() + " doesn't support Byteable interface");
    }
}
