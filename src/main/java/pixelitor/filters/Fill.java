/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters;

import pixelitor.colors.FillType;
import pixelitor.utils.ImageUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Fills an image with a color
 */
public class Fill extends Filter {
    private final FillType fillType;

    public Fill(FillType fillType) {
        this.fillType = fillType;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Color c = fillType.getColor();
        fillImage(dest, c);

        return dest;
    }

    /**
     * Fills the BufferedImage with the specified color
     */
    public static void fillImage(BufferedImage img, Color c) {
        int[] pixels = ImageUtils.getPixelsAsArray(img);

        int fillColor = c.getRGB();

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = fillColor;
        }
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}
