/**
 * Defines the temporal-tiler API for decomposing half-open temporal ranges into boundary-aligned tiles.
 *
 * <p>The public API is in the {@link com.github.jinahya.time.temporal.tile} package:
 * <ul>
 *     <li>{@link com.github.jinahya.time.temporal.tile.TemporalTile} /
 *         {@link com.github.jinahya.time.temporal.tile.TemporalTiler} &mdash; abstract bases for any
 *         {@link java.time.temporal.TemporalUnit}</li>
 *     <li>{@link com.github.jinahya.time.temporal.tile.ChronoTile} /
 *         {@link com.github.jinahya.time.temporal.tile.ChronoTiler} &mdash; concrete implementations for
 *         {@link java.time.temporal.ChronoUnit}</li>
 * </ul>
 *
 * @see com.github.jinahya.time.temporal.tile
 */
module com.github.jinahya.time.temporal {
    exports com.github.jinahya.time.temporal.tile;
}
