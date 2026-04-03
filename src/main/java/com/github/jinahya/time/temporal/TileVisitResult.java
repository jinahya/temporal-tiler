package com.github.jinahya.time.temporal;

/**
 * Controls traversal in the tiling tree.
 */
public enum TileVisitResult {

    /**
     * Decompose this tile into narrower grains (fork children).
     */
    CONTINUE,

    /**
     * Accept this tile as a leaf — accumulate it, do not decompose further.
     */
    SKIP_CHILDREN
}
