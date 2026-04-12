package com.github.jinahya.time.temporal.tile;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

final class TemporalTiles_TestUtils {

    static <T extends Temporal & Comparable<? super T>> void verify(final ChronoUnit grain,
                                                                    final List<TemporalTile<T>> tiles) {
        Objects.requireNonNull(grain, "grain is null");
        Objects.requireNonNull(tiles, "tiles is null");
        assertThat(tiles)
                .doesNotContainNull()
                .extracting(TemporalTile::grain)
                .containsOnly(grain);
        for (var i = 1; i < tiles.size(); i++) {
            assertThat(grain.between(tiles.get(i - 1).end(), tiles.get(i).start())).isZero();
        }
    }

    private TemporalTiles_TestUtils() {
        throw new AssertionError("instantiation is not allowed");
    }
}
