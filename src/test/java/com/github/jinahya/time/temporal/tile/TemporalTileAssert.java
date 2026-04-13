package com.github.jinahya.time.temporal.tile;

import org.assertj.core.api.AbstractAssert;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom AssertJ assertion class for {@link TemporalTile}, providing fluent assertions on tile start, end, grain, and
 * alignment.
 *
 * @param <T> the temporal type
 * @see TemporalTile
 */
class TemporalTileAssert<T extends Temporal & Comparable<? super T>>
        extends AbstractAssert<TemporalTileAssert<T>, TemporalTile<T>> {

    /**
     * Creates a new assertion for the given tile.
     *
     * @param <T>    the temporal type
     * @param actual the tile to assert on
     * @return a new {@link TemporalTileAssert} instance
     */
    static <T extends Temporal & Comparable<? super T>>
    TemporalTileAssert<T> assertTile(final TemporalTile<T> actual) {
        return new TemporalTileAssert<>(actual);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new assertion for the given tile.
     *
     * @param actual the tile to assert on
     */
    TemporalTileAssert(final TemporalTile<T> actual) {
        super(actual, TemporalTileAssert.class);
    }

    // ----------------------------------------------------------------------------------------------------------- start

    /**
     * Asserts that the tile's {@linkplain TemporalTile#start() start} equals the expected value.
     *
     * @param expectedStart the expected start
     * @return this assertion for chaining
     */
    TemporalTileAssert<T> hasStart(final T expectedStart) {
        isNotNull();
        final var actualStart = actual.start();
        assertThat(actualStart).isEqualByComparingTo(expectedStart);
        return myself;
    }

    // ------------------------------------------------------------------------------------------------------------- end

    /**
     * Asserts that the tile's {@linkplain TemporalTile#end() end} equals the expected value.
     *
     * @param expectedEnd the expected end
     * @return this assertion for chaining
     */
    TemporalTileAssert<T> hasEnd(final T expectedEnd) {
        isNotNull();
        final var actualEnd = actual.end();
        assertThat(actualEnd).isEqualByComparingTo(expectedEnd);
        return myself;
    }

    // ----------------------------------------------------------------------------------------------------------- grain

    /**
     * Asserts that the tile's {@linkplain TemporalTile#grain() grain} equals the expected value.
     *
     * @param expectedGrain the expected grain
     * @return this assertion for chaining
     */
    TemporalTileAssert<T> hasGrain(final TemporalUnit expectedGrain) {
        isNotNull();
        final var actualGrain = actual.grain();
        assertThat(actualGrain).isEqualTo(expectedGrain);
        return myself;
    }

    // --------------------------------------------------------------------------------------------------------- aligned

    /**
     * Asserts that the tile is {@linkplain TemporalTile#aligned() boundary-aligned}.
     *
     * @return this assertion for chaining
     */
    TemporalTileAssert<T> isAligned() {
        isNotNull();
        final var aligned = actual.aligned();
        assertThat(aligned).isTrue();
        return myself;
    }

    /**
     * Asserts that the tile is not {@linkplain TemporalTile#aligned() boundary-aligned} (partial).
     *
     * @return this assertion for chaining
     */
    TemporalTileAssert<T> isNotAligned() {
        isNotNull();
        final var aligned = actual.aligned();
        assertThat(aligned).isFalse();
        return myself;
    }
}
