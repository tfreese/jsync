// Created: 04.05.2020
package de.freese.jsync.swing.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.Serial;

/**
 * Expand the {@link GridBagConstraints} by a Builder-Pattern.
 *
 * @author Thomas Freese
 */
public final class GbcBuilder extends GridBagConstraints {
    @Serial
    private static final long serialVersionUID = -7701391832421914842L;

    /**
     * Defaults:
     * <ul>
     * <li>fill = NONE</li>
     * <li>weightX = 0.0D</li>
     * <li>weightY = 0.0D</li>
     * <li>insets = new Insets(5, 5, 5, 5)</li>
     * </ul>
     */
    public static GbcBuilder of(final int gridX, final int gridY) {
        final GbcBuilder gbcBuilder = new GbcBuilder(gridX, gridY);
        gbcBuilder.fillNone();
        gbcBuilder.insets(2, 2, 2, 2);

        return gbcBuilder;
    }

    private GbcBuilder(final int gridX, final int gridY) {
        super();

        this.gridx = gridX;
        this.gridy = gridY;
    }

    public GbcBuilder anchorCenter() {
        anchor = CENTER;

        return this;
    }

    public GbcBuilder anchorEast() {
        anchor = EAST;

        return this;
    }

    public GbcBuilder anchorNorth() {
        anchor = NORTH;

        return this;
    }

    public GbcBuilder anchorNorthEast() {
        anchor = NORTHEAST;

        return this;
    }

    public GbcBuilder anchorNorthWest() {
        anchor = NORTHWEST;

        return this;
    }

    public GbcBuilder anchorSouth() {
        anchor = SOUTH;

        return this;
    }

    public GbcBuilder anchorWest() {
        anchor = WEST;

        return this;
    }

    /**
     * Defaults:
     * <ul>
     * <li>weightX = 1.0D</li>
     * <li>weightY = 1.0D</li>
     * </ul>
     */
    public GbcBuilder fillBoth() {
        fill = BOTH;

        weightX(1.0D);
        weightY(1.0D);

        return this;
    }

    /**
     * Defaults:
     * <ul>
     * <li>weightX = 1.0D</li>
     * <li>weightY = 0.0D</li>
     * </ul>
     */
    public GbcBuilder fillHorizontal() {
        fill = HORIZONTAL;

        weightX(1.0D);
        weightY(0.0D);

        return this;
    }

    /**
     * Defaults:
     * <ul>
     * <li>weightX = 0.0D</li>
     * <li>weightY = 0.0D</li>
     * </ul>
     */
    public GbcBuilder fillNone() {
        fill = NONE;

        weightX(0.0D);
        weightY(0.0D);

        return this;
    }

    /**
     * Defaults:
     * <ul>
     * <li>weightX = 0.0D</li>
     * <li>weightY = 1.0D</li>
     * </ul>
     */
    public GbcBuilder fillVertical() {
        fill = VERTICAL;

        weightX(0.0D);
        weightY(1.0D);

        return this;
    }

    public GbcBuilder gridHeight(final int gridHeight) {
        this.gridheight = gridHeight;

        return this;
    }

    public GbcBuilder gridWidth(final int gridWidth) {
        this.gridwidth = gridWidth;

        return this;
    }

    public GbcBuilder insets(final Insets insets) {
        this.insets = insets;

        return this;
    }

    public GbcBuilder insets(final int top, final int left, final int bottom, final int right) {
        insets = new Insets(top, left, bottom, right);

        return this;
    }

    public GbcBuilder weightX(final double weightX) {
        this.weightx = weightX;

        return this;
    }

    public GbcBuilder weightY(final double weightY) {
        this.weighty = weightY;

        return this;
    }
}
