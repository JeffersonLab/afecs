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

package org.jlab.coda.afecs.ui.rcgui;

import org.jlab.coda.cMsg.cMsgCallbackAdapter;
import org.jlab.coda.cMsg.cMsgMessage;

import javax.swing.*;

/**
 * Base class for message callbacks in the run control GUI.
 *
 * <p>Provides template method pattern for cMsg callbacks that need to
 * process messages on the Swing EDT using SwingWorker. Subclasses
 * implement {@link #createAction(cMsgMessage)} to provide message-specific
 * processing logic.</p>
 *
 * @author gurjyan
 * @version 4.x
 */
public abstract class BaseMessageCallback extends cMsgCallbackAdapter {

    protected CodaRcGui owner;

    /**
     * Constructor.
     *
     * @param owner The CodaRcGui instance that owns this callback
     */
    public BaseMessageCallback(CodaRcGui owner) {
        this.owner = owner;
    }

    /**
     * Template method for callback processing.
     *
     * <p>Handles common null checks and message validation, then delegates
     * to {@link #createAction(cMsgMessage)} to create a SwingWorker for
     * processing the message on the EDT.</p>
     *
     * @param msg        The received cMsg message
     * @param userObject Optional user object (unused)
     */
    @Override
    public void callback(cMsgMessage msg, Object userObject) {
        if (msg != null) {
            String type = msg.getType();
            if (type != null) {
                SwingWorker<?, ?> action = createAction(msg);
                if (action != null) {
                    action.execute();
                }
            }
        }
    }

    /**
     * Create a SwingWorker to process the message.
     *
     * <p>Subclasses implement this method to provide message-specific
     * processing logic. The returned SwingWorker will be executed on
     * the Swing EDT.</p>
     *
     * @param msg The received cMsg message
     * @return SwingWorker to process the message, or null to skip processing
     */
    protected abstract SwingWorker<?, ?> createAction(cMsgMessage msg);
}
