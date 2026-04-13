package com.github.jinahya.time.temporal.tile;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TemporalTile}, verifying the {@code equals}/{@code hashCode} contract.
 *
 * @see TemporalTile
 */
class TemporalTileTest {

    @Test
    void equals__() {
        EqualsVerifier.forClass(TemporalTile.class).verify();
    }
}
