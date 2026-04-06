package com.github.jinahya.time.temporal.tile;

import org.assertj.core.api.AbstractAssert;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TemporalTileAssert<T extends Temporal & Comparable<? super T>>
        extends AbstractAssert<TemporalTileAssert<T>, TemporalTile<T>> {

    static <T extends Temporal & Comparable<? super T>>
    TemporalTileAssert<T> assertTile(final TemporalTile<T> actual) {
        return new TemporalTileAssert<>(actual);
    }

    // -----------------------------------------------------------------------------------------------------------------
    TemporalTileAssert(final TemporalTile<T> actual) {
        super(actual, TemporalTileAssert.class);
    }

    // ----------------------------------------------------------------------------------------------------------- start
    TemporalTileAssert<T> hasStart(final T expectedStart) {
        isNotNull();
        final var actualStart = actual.getStart();
        assertThat(actualStart).isEqualByComparingTo(expectedStart);
        return myself;
    }

    // ------------------------------------------------------------------------------------------------------------- end
    TemporalTileAssert<T> hasEnd(final T expectedEnd) {
        isNotNull();
        final var actualEnd = actual.getEnd();
        assertThat(actualEnd).isEqualByComparingTo(expectedEnd);
        return myself;
    }

    // ----------------------------------------------------------------------------------------------------------- grain
    TemporalTileAssert<T> hasGrain(final TemporalUnit expectedGrain) {
        isNotNull();
        final var actualGrain = actual.getGrain();
        assertThat(actualGrain).isEqualTo(expectedGrain);
        return myself;
    }

    // --------------------------------------------------------------------------------------------------------- aligned
    TemporalTileAssert<T> isAligned() {
        isNotNull();
        final var aligned = actual.isAligned();
        assertThat(aligned).isTrue();
        return myself;
    }

    TemporalTileAssert<T> isNotAligned() {
        isNotNull();
        final var aligned = actual.isAligned();
        assertThat(aligned).isFalse();
        return myself;
    }
}
