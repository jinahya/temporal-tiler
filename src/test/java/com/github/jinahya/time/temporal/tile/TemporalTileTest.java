package com.github.jinahya.time.temporal.tile;

import java.io.Serializable;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Objects;

abstract class TemporalTileTest<
        T extends TemporalTile<U, V>, U extends Temporal & Comparable<? super U> & Serializable,
        V extends TemporalUnit
        > {

    TemporalTileTest(final Class<T> tileClass, final Class<U> temporalClass, final Class<V> unitClass) {
        super();
        this.tileClass = Objects.requireNonNull(tileClass, "tileClass is null");
        this.temporalClass = Objects.requireNonNull(temporalClass, "temporalClass is null");
        this.unitClass = Objects.requireNonNull(unitClass, "unitClass is null");
    }

    final Class<T> tileClass;

    final Class<U> temporalClass;

    final Class<V> unitClass;
}
