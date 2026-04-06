package com.github.jinahya.time.temporal.tile;

import java.time.temporal.Temporal;
import java.util.Objects;

abstract class TemporalTileTest<T extends Temporal & Comparable<? super T>> {

    TemporalTileTest(final Class<T> temporalClass) {
        super();
        this.temporalClass = Objects.requireNonNull(temporalClass, "temporalClass is null");
    }

    // -----------------------------------------------------------------------------------------------------------------
    final Class<T> temporalClass;
}
