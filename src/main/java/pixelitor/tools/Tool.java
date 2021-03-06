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

package pixelitor.tools;

import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Layer;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.gui.ToolSettingsPanel;
import pixelitor.tools.toolhandlers.ToolHandlerChain;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.KeyListener;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.debug.DebugNode;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

/**
 * An abstract superclass for all tools.
 *
 * A tool defines the interaction between the
 * mouse and key events and a {@link Composition}
 */
public abstract class Tool implements KeyListener {
    // holding the alt key down generates continuously
    // altPressed calls, but only the first one is relevant
    protected boolean altDown = false;

    private ToolButton toolButton;

    private final String name;
    private final String iconFileName;
    private final String toolMessage;
    protected Cursor cursor;
    private final ClipStrategy clipStrategy;

    private final char activationKey;

    protected ToolSettingsPanel settingsPanel;
    protected boolean ended = false;

    final ToolHandlerChain handlerChain;

    protected Tool(String name, char activationKey, String iconFileName,
                   String toolMessage, Cursor cursor,
                   boolean allowOnlyDrawables, boolean handToolForwarding,
                   ClipStrategy clipStrategy) {
        this.activationKey = activationKey;
        assert Character.isUpperCase(activationKey);

        this.name = name;
        this.iconFileName = iconFileName;
        this.toolMessage = toolMessage;
        this.cursor = cursor;
        this.clipStrategy = clipStrategy;

        handlerChain = new ToolHandlerChain(this, cursor,
                allowOnlyDrawables, handToolForwarding);
    }

    public boolean doColorPickerForwarding() {
        return false;
    }

    public String getStatusBarMessage() {
        return "<html>" + name + " Tool: " + toolMessage;
    }

    public void mouseClicked(PMouseEvent e) {
    }

    public void setButton(ToolButton toolButton) {
        this.toolButton = toolButton;
    }

    public ToolButton getButton() {
        return toolButton;
    }

    public abstract void initSettingsPanel();

    public String getName() {
        return name;
    }

    public String getIconFileName() {
        return iconFileName;
    }

    public char getActivationKey() {
        return activationKey;
    }

    protected void toolStarted() {
        ended = false;

        GlobalEvents.setKeyListener(this);
        OpenImages.setCursorForAll(cursor);
    }

    protected void toolEnded() {
        ended = true;

        DraggablePoint.activePoint = null;

        closeToolDialogs();
    }

    protected void closeToolDialogs() {
    }

    public Cursor getStartingCursor() {
        return cursor;
    }

    /**
     * Paint over the active layer, used only by the shapes tool.
     * The transform of the given Graphics2D is in image space.
     */
    public void paintOverActiveLayer(Graphics2D g, Composition comp) {
    }

    /**
     * Paint on the {@link Composition} after all the layers have been painted.
     * The transform of the given Graphics2D is in component space.
     */
    public void paintOverImage(Graphics2D g2, Composition comp,
                               AffineTransform imageTransform) {

    }

    public void mouseMoved(MouseEvent e, View view) {
    }

    public void mouseEntered(MouseEvent e, View view) {
    }

    public void mouseExited(MouseEvent e, View view) {
    }

    public abstract void mousePressed(PMouseEvent e);

    public abstract void mouseDragged(PMouseEvent e);

    public abstract void mouseReleased(PMouseEvent e);

    public void setSettingsPanel(ToolSettingsPanel settingsPanel) {
        this.settingsPanel = settingsPanel;
    }

    public void randomize() {
        GUIUtils.randomizeGUIWidgetsOn(settingsPanel);
    }

    public void setClipFor(Graphics2D g, View view) {
        clipStrategy.setClipFor(g, view);
    }

    @Override
    public void spacePressed() {
        handlerChain.spacePressed();
    }

    @Override
    public void spaceReleased() {
        handlerChain.spaceReleased();
    }

    /**
     * Returns true if the key event was used for something
     */
    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        return false; // not consumed
    }

    @Override
    public void escPressed() {
        // empty by default
    }

    @Override
    public void altPressed() {
        if (!altDown && doColorPickerForwarding()) {
            OpenImages.setCursorForAll(
                    Tools.COLOR_PICKER.getStartingCursor());
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if(doColorPickerForwarding()) {
            OpenImages.setCursorForAll(cursor);
        }
        altDown = false;
    }

    @Override
    public void otherKeyPressed(KeyEvent e) {
    }

    @Override
    public String toString() {
        return name; // so that they can be easily selected from a JComboBox
    }

    // used for debugging
    public String getStateInfo() {
        return null;
    }

    public DebugNode getDebugNode() {
        DebugNode toolNode = new DebugNode("active tool", this);
        toolNode.addString("name", getName());
        return toolNode;
    }

    public void allCompsClosed() {
        resetInitialState();
    }

    public void compActivated(View oldCV, View newCV) {
        assert Tools.currentTool == this;
        if (oldCV != null) {
            oldCV.repaint();
            resetInitialState();
        }
    }

    public void compReplaced(Composition oldComp, Composition newComp, boolean reloaded) {
    }

    /**
     * Called after (1) a filter without dialog ends, or
     * (2) the active layer changes, or (3) the mask editing changes
     */
    public void editedObjectChanged(Layer layer) {
    }

    public void firstModalDialogShown() {
    }

    public void firstModalDialogHidden() {
    }

    public void resetInitialState() {
    }

    public void fgBgColorsChanged() {
    }

    public void setupMaskEditing(boolean maskEditing) {
    }

    /**
     * Called when the component space coordinates of the pixels changed,
     * but the image coordinates are still the same (zooming, view resizing).
     *
     * The component coordinates of the widgets must be restored
     * from their image coordinates
     */
    public void coCoordsChanged(View view) {
    }

    /**
     * Called when the image space coordinates of the pixels changed
     * (image resizing, cropping, canvas enlargement, flipping, etc.),
     * and this change is described by the given {@link AffineTransform}
     *
     * The change in image coords implies a change in component coords,
     * therefore the component space coordinates also have to be recalculated.
     */
    public void imCoordsChanged(Composition comp, AffineTransform at) {
    }

    public void activate() {
        if (toolButton != null) {
            toolButton.doClick();
        } else {
            assert Build.isUnitTesting();
            Tools.changeTo(this);
        }
    }

    public boolean isActive() {
        return Tools.currentIs(this);
    }
}
