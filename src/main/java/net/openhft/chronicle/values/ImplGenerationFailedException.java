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

/**
 * Thrown when the Chronicle Values library is not able to generate heap or native implementation
 * for the given interface, i. e. it is not a <i>value interface</i>. This exception thrown for
 * a valid value interface (according to
 * <a href="https://github.com/OpenHFT/Chronicle-Values#value-interface-specification">the
 * specification</a>) might also indicate a bug in the Chronicle Values library.
 */
public final class ImplGenerationFailedException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    public ImplGenerationFailedException(Throwable cause) {
        super(cause);
    }
}
