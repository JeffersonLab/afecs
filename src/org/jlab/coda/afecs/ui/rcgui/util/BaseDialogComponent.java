/*
 *   Copyright (c) 2017.  Jefferson Lab (JLab). All rights reserved. Permission
 *   to use, copy, modify, and distribute  this software and its documentation for
 *   governmental use, educational, research, and not-for-profit purposes, without
 *   fee and without a signed licensing agreement.
 *
 *   IN NO EVENT SHALL JLAB BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL
 *   INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING
 *   OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JLAB HAS
 *   BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   JLAB SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *   THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *   PURPOSE. THE CLARA SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY,
 *   PROVIDED HEREUNDER IS PROVIDED "AS IS". JLAB HAS NO OBLIGATION TO PROVIDE
 *   MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 *   This software was developed under the United States Government license.
 *   For more information contact author at gurjyan@jlab.org
 *   Department of Experimental Nuclear Physics, Jefferson Lab.
 */

package org.jlab.coda.afecs.ui.rcgui.util;

import javax.accessibility.Accessible;
import javax.swing.*;
import java.awt.*;

/**
 * Base class for dialog-based GUI components.
 * Provides common dialog creation, layout, and return value handling.
 *
 * <p>Subclasses should:</p>
 * <ul>
 * <li>Call {@link #initDialog(Component)} to create the dialog</li>
 * <li>Implement {@link #setupDialogContent()} to add custom content</li>
 * <li>Use {@link #setReturnValue(int)} to set OK/CANCEL result</li>
 * </ul>
 *
 * @author gurjyan
 * @version 4.x
 */
public abstract class BaseDialogComponent extends JComponent implements Accessible {

    /** Dialog container */
    protected JDialog dialog;

    /** Information title (for informational dialogs) */
    protected String infoTitle;

    /** GUI window title */
    protected String guiTitle;

    /** Return value: User cancelled */
    public static final int RET_CANCEL = 0;

    /** Return value: User confirmed (OK) */
    public static final int RET_OK = 1;

    /** Current return value (OK or CANCEL) */
    private int returnValue = RET_CANCEL;

    /**
     * Initialize and create the dialog.
     * Template method that calls {@link #setupDialogContent()} for customization.
     *
     * @param parent Parent component for dialog positioning
     * @return Created dialog
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true
     */
    protected JDialog initDialog(Component parent) throws HeadlessException {
        Frame frame = parent instanceof Frame ? (Frame) parent :
                      (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);

        dialog = new JDialog(frame, guiTitle, true); // Modal dialog
        dialog.setComponentOrientation(this.getComponentOrientation());

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);

        // Call template method for subclass-specific setup
        setupDialogContent();

        // Standard dialog finalization
        dialog.getRootPane().setDefaultButton(getDefaultButton());
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        return dialog;
    }

    /**
     * Template method for subclasses to add dialog-specific content.
     * Called during {@link #initDialog(Component)}.
     *
     * <p>Subclasses should override to:</p>
     * <ul>
     * <li>Add buttons with listeners</li>
     * <li>Configure layout</li>
     * <li>Set up event handlers</li>
     * </ul>
     */
    protected abstract void setupDialogContent();

    /**
     * Get the default button for the dialog (activated by Enter key).
     * Subclasses should override to return their OK/primary button.
     *
     * @return Default button, or null if none
     */
    protected abstract JButton getDefaultButton();

    /**
     * Show the dialog and wait for user response.
     *
     * @return {@link #RET_OK} or {@link #RET_CANCEL}
     */
    public int showDialog() {
        if (dialog == null) {
            throw new IllegalStateException("Dialog not initialized. Call initDialog() first.");
        }
        returnValue = RET_CANCEL; // Default to cancel
        dialog.setVisible(true);
        return returnValue;
    }

    /**
     * Set the dialog return value.
     * Typically called by OK/Cancel button handlers before closing dialog.
     *
     * @param value {@link #RET_OK} or {@link #RET_CANCEL}
     */
    protected void setReturnValue(int value) {
        this.returnValue = value;
    }

    /**
     * Get the dialog return value.
     *
     * @return Last return value set
     */
    public int getReturnValue() {
        return returnValue;
    }

    /**
     * Close the dialog.
     */
    protected void closeDialog() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }

    /**
     * Set the GUI title (displayed in dialog title bar).
     *
     * @param title Title string
     */
    public void setGuiTitle(String title) {
        this.guiTitle = title;
        if (dialog != null) {
            dialog.setTitle(title);
        }
    }

    /**
     * Get the GUI title.
     *
     * @return Title string
     */
    public String getGuiTitle() {
        return guiTitle;
    }

    /**
     * Set the information title.
     *
     * @param title Info title string
     */
    public void setInfoTitle(String title) {
        this.infoTitle = title;
    }

    /**
     * Get the information title.
     *
     * @return Info title string
     */
    public String getInfoTitle() {
        return infoTitle;
    }
}
