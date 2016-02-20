/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.gui;

import pixelitor.gui.utils.DropDownSlider;
import pixelitor.layers.BlendingMode;

import javax.swing.*;
import java.awt.Composite;
import java.awt.FlowLayout;
import java.util.Random;

/**
 * A GUI selector for opacity and blending mode
 */
public class BlendingModePanel extends JPanel {
    protected final DropDownSlider opacityDDSlider;
    protected final JComboBox<BlendingMode> blendingModeCombo;
    private final JLabel opacityLabel;
    private final JLabel bmLabel;

    public BlendingModePanel(boolean longText) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        opacityLabel = new JLabel("Opacity:");
        add(opacityLabel);
        opacityDDSlider = new DropDownSlider(100, 0, 100, true);

        add(opacityDDSlider);

        if (longText) {
            bmLabel = new JLabel("%, Blending Mode:", SwingConstants.LEFT);
        } else {
            bmLabel = new JLabel("%", SwingConstants.LEFT);
        }
        add(bmLabel);

        BlendingMode[] blendingModes = BlendingMode.values();
        blendingModeCombo = new JComboBox<>(blendingModes);
        blendingModeCombo.setMaximumRowCount(blendingModes.length);
        add(blendingModeCombo);
    }

    protected float getOpacity() {
        return opacityDDSlider.getValue() / 100.0f;
    }

    protected BlendingMode getBlendingMode() {
        return (BlendingMode) blendingModeCombo.getSelectedItem();
    }

    public Composite getComposite() {
        Composite composite = getBlendingMode().getComposite(getOpacity());
        return composite;
    }

    public void randomize() {
        BlendingMode[] blendingModes = BlendingMode.values();
        Random r = new Random();
        int randomIndex = r.nextInt(blendingModes.length);
        blendingModeCombo.setSelectedIndex(randomIndex);

        int newOpacity = r.nextInt(100);
        opacityDDSlider.setValue(newOpacity);
    }

    @Override
    public void setEnabled(boolean b) {
        opacityLabel.setEnabled(b);
        opacityDDSlider.setEnabled(b);
        bmLabel.setEnabled(b);
        blendingModeCombo.setEnabled(b);
    }
}