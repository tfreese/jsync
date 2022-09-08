// Created: 04.05.2020
package de.freese.jsync.swing.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.Serial;

/**
 * Erweitert die {@link GridBagConstraints} um das Builder-Pattern.
 *
 * @author Thomas Freese
 */
public class GbcBuilder extends GridBagConstraints
{
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7701391832421914842L;

    /**
     * Erstellt ein neues {@link GbcBuilder} Object.<br>
     * Defaults:
     * <ul>
     * <li>fill = NONE</li>
     * <li>weightX = 0.0D</li>
     * <li>weightY = 0.0D</li>
     * <li>insets = new Insets(5, 5, 5, 5)</li>
     * </ul>
     */
    public GbcBuilder(final int gridX, final int gridY)
    {
        super();

        this.gridx = gridX;
        this.gridy = gridY;

        fillNone();
        insets(2, 2, 2, 2);
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder anchorCenter()
    {
        this.anchor = CENTER;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder anchorEast()
    {
        this.anchor = EAST;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder anchorNorth()
    {
        this.anchor = NORTH;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder anchorNorthEast()
    {
        this.anchor = NORTHEAST;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder anchorNorthWest()
    {
        this.anchor = NORTHWEST;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder anchorSouth()
    {
        this.anchor = SOUTH;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder anchorWest()
    {
        this.anchor = WEST;

        return this;
    }

    /**
     * Defaults:
     * <ul>
     * <li>weightX = 1.0D</li>
     * <li>weightY = 1.0D</li>
     * </ul>
     *
     * @return {@link GbcBuilder}
     */
    public GbcBuilder fillBoth()
    {
        this.fill = BOTH;

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
     *
     * @return {@link GbcBuilder}
     */
    public GbcBuilder fillHorizontal()
    {
        this.fill = HORIZONTAL;

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
     *
     * @return {@link GbcBuilder}
     */
    public GbcBuilder fillNone()
    {
        this.fill = NONE;

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
     *
     * @return {@link GbcBuilder}
     */
    public GbcBuilder fillVertical()
    {
        this.fill = VERTICAL;

        weightX(0.0D);
        weightY(1.0D);

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder gridHeight(final int gridHeight)
    {
        this.gridheight = gridHeight;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder gridWidth(final int gridWidth)
    {
        this.gridwidth = gridWidth;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder insets(final Insets insets)
    {
        this.insets = insets;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder insets(final int top, final int left, final int bottom, final int right)
    {
        this.insets = new Insets(top, left, bottom, right);

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder weightX(final double weightX)
    {
        this.weightx = weightX;

        return this;
    }

    /**
     * @return {@link GbcBuilder}
     */
    public GbcBuilder weightY(final double weightY)
    {
        this.weighty = weightY;

        return this;
    }
}
