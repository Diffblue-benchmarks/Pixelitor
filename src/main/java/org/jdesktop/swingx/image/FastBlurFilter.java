/*
 * $Id: FastBlurFilter.java 1699 2007-01-22 12:54:27Z gfx $
 *
 * Dual-licensed under LGPL (Sun and Romain Guy) and BSD (Romain Guy).
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * Copyright (c) 2006 Romain Guy <romain.guy@mac.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jdesktop.swingx.image;

import org.jdesktop.swingx.graphics.GraphicsUtilities;
import pixelitor.utils.ProgressTracker;

import java.awt.image.BufferedImage;

/**
 * <p>A fast blur filter can be used to blur pictures quickly. This filter is an
 * implementation of the box blur algorithm. The blurs generated by this
 * algorithm might show square artifacts, especially on pictures containing
 * straight lines (rectangles, text, etc.) On most pictures though, the
 * result will look very good.</p>
 * <p>The force of the blur can be controlled with a radius and the
 * default radius is 3. Since the blur clamps values on the edges of the
 * source picture, you might need to provide a picture with empty borders
 * to avoid artifacts at the edges. The performance of this filter are
 * independant from the radius.</p>
 *
 * @author Romain Guy <romain.guy@mac.com>
 */
public class FastBlurFilter extends AbstractFilter {
    private final int radius;

    /**
     * <p>Creates a new blur filter with a default radius of 3.</p>
     */
    public FastBlurFilter() {
        this(3);
    }

    /**
     * <p>Creates a new blur filter with the specified radius. If the radius
     * is lower than 1, a radius of 1 will be used automatically.</p>
     *
     * @param radius the radius, in pixels, of the blur
     */
    public FastBlurFilter(int radius) {
        if (radius < 1) {
            radius = 1;
        }

        this.radius = radius;
    }

    /**
     * <p>Returns the radius used by this filter, in pixels.</p>
     *
     * @return the radius of the blur
     */
    public int getRadius() {
        return radius;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        ProgressTracker pt = new ProgressTracker(width + height);

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] srcPixels = new int[width * height];
        int[] dstPixels = new int[width * height];

        GraphicsUtilities.getPixels(src, 0, 0, width, height, srcPixels);
        // horizontal pass
        blur(srcPixels, dstPixels, width, height, radius, pt);
        // vertical pass
        //noinspection SuspiciousNameCombination
        blur(dstPixels, srcPixels, height, width, radius, pt);
        // the result is now stored in srcPixels due to the 2nd pass
        GraphicsUtilities.setPixels(dst, 0, 0, width, height, srcPixels);

        pt.finish();

        return dst;
    }

    /**
     * <p>Blurs the source pixels into the destination pixels. The force of
     * the blur is specified by the radius which must be greater than 0.</p>
     * <p>The source and destination pixels arrays are expected to be in the
     * INT_ARGB format.</p>
     * <p>After this method is executed, dstPixels contains a transposed and
     * filtered copy of srcPixels.</p>
     *
     *  @param srcPixels the source pixels
     * @param dstPixels the destination pixels
     * @param width     the width of the source picture
     * @param height    the height of the source picture
     * @param radius    the radius of the blur effect
     */
    static void blur(int[] srcPixels, int[] dstPixels,
                     int width, int height, int radius, ProgressTracker pt) {
        final int windowSize = radius * 2 + 1;
        final int radiusPlusOne = radius + 1;

        int sumAlpha;
        int sumRed;
        int sumGreen;
        int sumBlue;

        int srcIndex = 0;
        int dstIndex;
        int pixel;

        int[] sumLookupTable = new int[256 * windowSize];
        for (int i = 0; i < sumLookupTable.length; i++) {
            sumLookupTable[i] = i / windowSize;
        }

        int[] indexLookupTable = new int[radiusPlusOne];
        if (radius < width) {
            for (int i = 0; i < indexLookupTable.length; i++) {
                indexLookupTable[i] = i;
            }
        } else {
            for (int i = 0; i < width; i++) {
                indexLookupTable[i] = i;
            }
            for (int i = width; i < indexLookupTable.length; i++) {
                indexLookupTable[i] = width - 1;
            }
        }

        for (int y = 0; y < height; y++) {
            sumAlpha = sumRed = sumGreen = sumBlue = 0;
            dstIndex = y;

            pixel = srcPixels[srcIndex];
            sumAlpha += radiusPlusOne * ((pixel >> 24) & 0xFF);
            sumRed += radiusPlusOne * ((pixel >> 16) & 0xFF);
            sumGreen += radiusPlusOne * ((pixel >> 8) & 0xFF);
            sumBlue += radiusPlusOne * (pixel & 0xFF);

            for (int i = 1; i <= radius; i++) {
                pixel = srcPixels[srcIndex + indexLookupTable[i]];
                sumAlpha += (pixel >> 24) & 0xFF;
                sumRed += (pixel >> 16) & 0xFF;
                sumGreen += (pixel >> 8) & 0xFF;
                sumBlue += pixel & 0xFF;
            }

            for (int x = 0; x < width; x++) {
                dstPixels[dstIndex] = sumLookupTable[sumAlpha] << 24 |
                        sumLookupTable[sumRed] << 16 |
                        sumLookupTable[sumGreen] << 8 |
                        sumLookupTable[sumBlue];
                dstIndex += height;

                int nextPixelIndex = x + radiusPlusOne;
                if (nextPixelIndex >= width) {
                    nextPixelIndex = width - 1;
                }

                int previousPixelIndex = x - radius;
                if (previousPixelIndex < 0) {
                    previousPixelIndex = 0;
                }

                int nextPixel = srcPixels[srcIndex + nextPixelIndex];
                int previousPixel = srcPixels[srcIndex + previousPixelIndex];

                sumAlpha += (nextPixel >> 24) & 0xFF;
                sumAlpha -= (previousPixel >> 24) & 0xFF;

                sumRed += (nextPixel >> 16) & 0xFF;
                sumRed -= (previousPixel >> 16) & 0xFF;

                sumGreen += (nextPixel >> 8) & 0xFF;
                sumGreen -= (previousPixel >> 8) & 0xFF;

                sumBlue += nextPixel & 0xFF;
                sumBlue -= previousPixel & 0xFF;
            }

            srcIndex += width;

            pt.itemProcessed();
        }
    }
}
