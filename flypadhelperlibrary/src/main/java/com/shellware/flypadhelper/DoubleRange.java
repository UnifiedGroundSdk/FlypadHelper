/*
 *     Copyright (C) 2019 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.shellware.flypadhelper;

import androidx.annotation.NonNull;

/** Range of doubles. */
public interface DoubleRange {

    /**
     * Creates an immutable {@code DoubleRange}.
     *
     * @param lower range lower bound
     * @param upper range upper bound
     *
     * @return a new immutable {@code DoubleRange} instance
     */
    @NonNull
    static DoubleRange of(double lower, double upper) {
        return new DoubleRangeCore(lower, upper);
    }

    /**
     * Gets lower bound.
     *
     * @return the lower bound
     */
    double getLower();

    /**
     * Gets upper bound.
     *
     * @return the upper bound
     */
    double getUpper();

    /**
     * Tells whether this range may contain some value.
     *
     * @param value value to test
     *
     * @return {@code true} if the provided value fits within this range, otherwise {@code false}
     */
    default boolean contains(double value) {
        return value >= getLower() && value <= getUpper();
    }

    /**
     * Clamps a value to this range.
     * <ul>
     * <li>In case {@code value} is less than {@link #getLower() lower bound}, returns lower bound,</li>
     * <li>otherwise if {@code value} is greater than {@link #getUpper() upper bound}, returns upper bound,</li>
     * <li>otherwise, returns {@code value}.</li>
     * </ul>
     *
     * @param value value to clamp
     *
     * @return a value clamped to this range
     */
    default double clamp(double value) {
        double bound;
        if ((bound = getLower()) > value) {
            return bound;
        } else if ((bound = getUpper()) < value) {
            return bound;
        } else {
            return value;
        }
    }

    /**
     * Scales a value from a given range to this range.
     * <p>
     * The given range should respect the following condition: {@code src.getLower() < src.getUpper()}
     *
     * @param value value to scale
     * @param src   range to scale from
     *
     * @return a value linearly scaled to this range from {@code value} in {@code src}
     */
    default double scaleFrom(double value, @NonNull DoubleRange src) {
        double srcLower = src.getLower();
        double srcUpper = src.getUpper();
        if (srcLower >= srcUpper) {
            throw new IllegalArgumentException("lower must be less than upper");
        }
        double destLower = getLower();
        return clamp(destLower + ((value - srcLower) * (getUpper() - destLower) / (srcUpper - srcLower)));
    }
}
