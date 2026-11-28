package de.freese.jsync.swing.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.Serial;

/**
 * Expand the {@link GridBagConstraints} by a Builder-Pattern.
 *
 * @author Thomas Freese
 * @since 04.05.2020
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

        super.gridx = gridX;
        super.gridy = gridY;
    }

    public GbcBuilder anchorCenter() {
        super.anchor = CENTER;

        return this;
    }

    public GbcBuilder anchorEast() {
        super.anchor = EAST;

        return this;
    }

    public GbcBuilder anchorNorth() {
        super.anchor = NORTH;

        return this;
    }

    public GbcBuilder anchorNorthEast() {
        super.anchor = NORTHEAST;

        return this;
    }

    public GbcBuilder anchorNorthWest() {
        super.anchor = NORTHWEST;

        return this;
    }

    public GbcBuilder anchorSouth() {
        super.anchor = SOUTH;

        return this;
    }

    public GbcBuilder anchorWest() {
        super.anchor = WEST;

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
        super.fill = BOTH;

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
        super.fill = HORIZONTAL;

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
        super.fill = NONE;

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
        super.fill = VERTICAL;

        weightX(0.0D);
        weightY(1.0D);

        return this;
    }

    public GbcBuilder gridHeight(final int gridHeight) {
        super.gridheight = gridHeight;

        return this;
    }

    public GbcBuilder gridWidth(final int gridWidth) {
        super.gridwidth = gridWidth;

        return this;
    }

    public GbcBuilder insets(final Insets insets) {
        super.insets = insets;

        return this;
    }

    public GbcBuilder insets(final int top, final int left, final int bottom, final int right) {
        super.insets = new Insets(top, left, bottom, right);

        return this;
    }

    public GbcBuilder weightX(final double weightX) {
        super.weightx = weightX;

        return this;
    }

    public GbcBuilder weightY(final double weightY) {
        super.weighty = weightY;

        return this;
    }
}
