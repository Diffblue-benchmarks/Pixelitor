/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gui;

import com.bric.swing.GradientSlider;
import com.jhlabs.image.Colormap;
import com.jhlabs.image.ImageMath;
import pixelitor.colors.ColorUtils;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;

import static com.bric.swing.MultiThumbSlider.HORIZONTAL;
import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.Color.WHITE;
import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;

/**
 * Represents a gradient.
 * Unlike other filter parameter implementations, this is not
 * a GUI-free model, the actual value is stored inside the GradientSlider.
 */
public class GradientParam extends AbstractFilterParam {
    private static final String GRADIENT_SLIDER_USE_BEVEL = "GradientSlider.useBevel";
    private GradientSlider gradientSlider;
    private GUI gui;
    private final float[] defaultThumbPositions;
    private final Color[] defaultColors;

    // whether the running of the filter should be triggered
    private boolean trigger = true;

    public GradientParam(String name, Color firstColor, Color secondColor) {
        this(name, new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{
                        firstColor,
                        ColorUtils.calcRGBAverage(firstColor, secondColor),
                        secondColor});
    }

    public GradientParam(String name, float[] defaultThumbPositions,
                         Color[] defaultColors) {
        this(name, defaultThumbPositions, defaultColors, ALLOW_RANDOMIZE);
    }

    public GradientParam(String name, float[] defaultThumbPositions,
                         Color[] defaultColors, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);
        this.defaultThumbPositions = defaultThumbPositions;
        this.defaultColors = defaultColors;

        // has to be created in the constructor because getValue() can be called early
        createGradientSlider(defaultThumbPositions, defaultColors);
    }

    public static GradientParam createBlackToWhite(String name) {
        return new GradientParam(name,
                new float[]{0.0f, 0.5f, 1.0f},
                new Color[]{BLACK, GRAY, WHITE});
    }

    private void createGradientSlider(float[] defaultThumbPositions, Color[] defaultColors) {
        gradientSlider = new GradientSlider(HORIZONTAL, defaultThumbPositions, defaultColors);
        gradientSlider.addPropertyChangeListener(this::sliderPropertyChanged);
        gradientSlider.putClientProperty(GRADIENT_SLIDER_USE_BEVEL, "true");
        gradientSlider.setPreferredSize(new Dimension(250, 30));
    }

    private void sliderPropertyChanged(PropertyChangeEvent evt) {
        if (shouldStartFilter(evt)) {
            if (gui != null) {
                gui.updateDefaultButtonIcon();
            }
            adjustmentListener.paramAdjusted();
        }
    }

    private boolean shouldStartFilter(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(GRADIENT_SLIDER_USE_BEVEL)) {
            return false;
        }
        if (evt.getPropertyName().equals("enabled")) {
            return false;
        }

        if (trigger && !gradientSlider.isValueAdjusting() && adjustmentListener != null) {
            String propertyName = evt.getPropertyName();
            if (!"ancestor".equals(propertyName)) {
                if (!"selected thumb".equals(propertyName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public JComponent createGUI() {
        gui = new GUI(gradientSlider, this);
        return gui;
    }

    public Colormap getValue() {
        return this::rgbIntFromValue;
    }

    private int rgbIntFromValue(float v) {
        Color c = (Color) gradientSlider.getValue(v);
        if (c == null) {
            throw new IllegalStateException("null color for v = " + v);
        }
        return c.getRGB();
    }

    @Override
    public int getNumGridBagCols() {
        return 2;
    }

    @Override
    protected void doRandomize() {
        Color[] randomColors = new Color[defaultThumbPositions.length];
        for (int i = 0; i < randomColors.length; i++) {
            randomColors[i] = Rnd.createRandomColor();
        }

        trigger = false;
        gradientSlider.setValues(defaultThumbPositions, randomColors);
        trigger = true;
        if (gui != null) {
            gui.updateDefaultButtonIcon();
        }
    }

    @Override
    public boolean isSetToDefault() {
        if (areThumbPositionsChanged()) {
            return false;
        }

        if (areColorsChanged()) {
            return false;
        }

        return true;
    }

    private boolean areThumbPositionsChanged() {
        float[] thumbPositions = gradientSlider.getThumbPositions();
        if (thumbPositions.length != defaultThumbPositions.length) {
            return true;
        }
        for (int i = 0; i < thumbPositions.length; i++) {
            if (thumbPositions[i] != defaultThumbPositions[i]) {
                return true;
            }
        }
        return false;
    }

    private boolean areColorsChanged() {
        Object[] values = gradientSlider.getValues();
        if (values.length != defaultColors.length) {
            return true;
        }

        for (int i = 0; i < defaultColors.length; i++) {
            if (!defaultColors[i].equals(values[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset(boolean trigger) {
        if (trigger) {
            gradientSlider.setValues(defaultThumbPositions, defaultColors);
        } else {
            this.trigger = false;
            gradientSlider.setValues(defaultThumbPositions, defaultColors);
            this.trigger = true;
        }
        if (gui != null) {
            gui.updateDefaultButtonIcon();
        }
    }

    @Override
    void setEnabled(boolean b) {
        if (gradientSlider != null) {
            gradientSlider.setEnabled(b);
        }
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public GState copyState() {
        return new GState(gradientSlider.getThumbPositions(), gradientSlider.getColors());
    }

    @Override
    public void setState(ParamState<?> state) {
        GState gr = (GState) state;

        trigger = false;
        createGradientSlider(gr.thumbPositions, gr.colors);
        trigger = true;
    }

    @Override
    public Object getParamValue() {
        return Arrays.asList(gradientSlider.getValues());
    }

    @Override
    public String toString() {
        return format("%s[name = '%s']", getClass().getSimpleName(), getName());
    }

    private static class GState implements ParamState<GState> {
        final float[] thumbPositions;
        final Color[] colors;

        public GState(float[] thumbPositions, Color[] colors) {
            this.thumbPositions = thumbPositions;
            this.colors = colors;
        }

        @Override
        public GState interpolate(GState endState, double progress) {
            // This will not work if the number of thumbs changes

            float[] interpolatedPositions = getInterpolatedPositions((float) progress, endState);

            Color[] interpolatedColors = getInterpolatedColors((float) progress, endState);

            return new GState(interpolatedPositions, interpolatedColors);
        }

        private float[] getInterpolatedPositions(float progress, GState grEndState) {
            float[] interpolatedPositions = new float[thumbPositions.length];
            for (int i = 0; i < thumbPositions.length; i++) {
                float initial = thumbPositions[i];
                float end = grEndState.thumbPositions[i];
                float interpolated = ImageMath.lerp(progress, initial, end);
                interpolatedPositions[i] = interpolated;
            }
            return interpolatedPositions;
        }

        private Color[] getInterpolatedColors(float progress, GState grEndState) {
            Color[] interpolatedColors = new Color[colors.length];
            for (int i = 0; i < colors.length; i++) {
                Color initial = colors[i];
                Color end = grEndState.colors[i];
                Color interpolated = ColorUtils.interpolateInRGB(initial, end, progress);
                interpolatedColors[i] = interpolated;
            }
            return interpolatedColors;
        }
    }

    static class GUI extends JPanel {
        private final GradientSlider slider;
        private final DefaultButton defaultButton;

        public GUI(GradientSlider slider, GradientParam gradientParam) {
            super(new FlowLayout());
            this.slider = slider;
            add(slider);
            defaultButton = new DefaultButton(gradientParam);
            add(defaultButton);
        }

        @Override
        public void setEnabled(boolean enabled) {
            slider.setEnabled(enabled);
        }

        @Override
        public boolean isEnabled() {
            return slider.isEnabled();
        }

        public void updateDefaultButtonIcon() {
            defaultButton.updateIcon();
        }
    }
}
