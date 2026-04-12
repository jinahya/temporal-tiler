package com.github.jinahya.time.temporal.tile;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TemporalTileTest {

    @Test
    void equals__() {
        EqualsVerifier.forClass(TemporalTile.class).verify();
    }
}
