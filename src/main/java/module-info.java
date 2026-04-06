/**
 * Defines the temporal-tiler API for decomposing half-open temporal ranges into boundary-aligned tiles.
 *
 * <p>The public API is in the {@link com.github.jinahya.time.temporal.tile} package:
 * <ul>
 *     <li>{@link com.github.jinahya.time.temporal.tile.TemporalTile} &mdash; an immutable tile representing a
 *         half-open range {@code [start, end)} at a {@link java.time.temporal.TemporalUnit} grain</li>
 *     <li>{@link com.github.jinahya.time.temporal.tile.TemporalTiler} &mdash; static utility that partitions a range
 *         into tiles; includes a convenience overload for {@link java.time.temporal.ChronoUnit}</li>
 * </ul>
 *
 * @see com.github.jinahya.time.temporal.tile
 */
module com.github.jinahya.time.temporal {
    exports com.github.jinahya.time.temporal.tile;
}
