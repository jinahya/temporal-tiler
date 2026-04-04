package com.github.jinahya.time.temporal.tile;

import org.assertj.core.api.AbstractAssert;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TemporalTileAssert<
        T extends Temporal & Comparable<? super T> & Serializable,
        U extends TemporalUnit
        >
        extends AbstractAssert<TemporalTileAssert<T, U>, TemporalTile<T, U>> {

    static <
            T extends Temporal & Comparable<? super T> & Serializable,
            U extends TemporalUnit
            >
    TemporalTileAssert<T, U> assertTile(final TemporalTile<T, U> actual) {
        return new TemporalTileAssert<>(actual);
    }

    // -----------------------------------------------------------------------------------------------------------------
    TemporalTileAssert(final TemporalTile<T, U> actual) {
        super(actual, TemporalTileAssert.class);
    }

    // ----------------------------------------------------------------------------------------------------------- start
    TemporalTileAssert<T, U> hasStart(final T expectedStart) {
        isNotNull();
        final var actualStart = actual.getStart();
        assertThat(actualStart).isEqualByComparingTo(expectedStart);
        return myself;
    }

    // ------------------------------------------------------------------------------------------------------------- end
    TemporalTileAssert<T, U> hasEnd(final T expectedEnd) {
        isNotNull();
        final var actualEnd = actual.getEnd();
        assertThat(actualEnd).isEqualByComparingTo(expectedEnd);
        return myself;
    }

    // ----------------------------------------------------------------------------------------------------------- grain
    TemporalTileAssert<T, U> hasGrain(final U expectedGrain) {
        isNotNull();
        final var actualGrain = actual.getGrain();
        assertThat(actualGrain).isEqualTo(expectedGrain);
        return myself;
    }

    // --------------------------------------------------------------------------------------------------------- aligned
    TemporalTileAssert<T, U> isAligned() {
        isNotNull();
        final var aligned = actual.isAligned();
        assertThat(aligned).isTrue();
        return myself;
    }

    TemporalTileAssert<T, U> isNotAligned() {
        isNotNull();
        final var aligned = actual.isAligned();
        assertThat(aligned).isFalse();
        return myself;
    }
}
