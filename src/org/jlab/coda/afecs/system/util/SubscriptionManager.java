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

package org.jlab.coda.afecs.system.util;

import org.jlab.coda.cMsg.cMsg;
import org.jlab.coda.cMsg.cMsgCallbackAdapter;
import org.jlab.coda.cMsg.cMsgException;
import org.jlab.coda.cMsg.cMsgSubscriptionHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cMsg subscription handles with automatic cleanup and error handling.
 *
 * <p>
 * This utility simplifies subscription lifecycle management across agents and
 * platform components by:
 * <ul>
 * <li>Automatically tracking all subscription handles</li>
 * <li>Providing centralized unsubscribe with error handling</li>
 * <li>Supporting both platform and RC domain connections</li>
 * <li>Thread-safe operation via ConcurrentHashMap</li>
 * </ul>
 * </p>
 *
 * <p>Usage example:
 * <pre>
 * SubscriptionManager mgr = new SubscriptionManager();
 * mgr.subscribe(myConnection, "mySubject", "myType", new MyCallback());
 * ...
 * mgr.unsubscribeAll(myConnection); // Cleanup all subscriptions
 * </pre>
 * </p>
 *
 * @author gurjyan
 * @version 4.x
 */
public class SubscriptionManager {

    /**
     * Map of subscription handles keyed by "subject:type"
     */
    private final Map<String, cMsgSubscriptionHandle> handles = new ConcurrentHashMap<>();

    /**
     * Subscribe to cMsg subject/type pattern and register the handle for later cleanup.
     *
     * @param connection cMsg connection object
     * @param subject    Subject pattern (can include wildcards)
     * @param type       Type pattern (can include wildcards)
     * @param callback   Callback handler implementing cMsgCallbackAdapter
     * @return Subscription handle (also stored internally)
     * @throws cMsgException if subscription fails
     */
    public cMsgSubscriptionHandle subscribe(cMsg connection, String subject,
            String type, cMsgCallbackAdapter callback) throws cMsgException {

        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        if (subject == null || type == null) {
            throw new IllegalArgumentException("Subject and type cannot be null");
        }

        String key = subject + ":" + type;
        cMsgSubscriptionHandle handle = connection.subscribe(subject, type, callback, null);
        handles.put(key, handle);
        return handle;
    }

    /**
     * Unsubscribe a specific subscription by subject and type.
     *
     * @param connection cMsg connection
     * @param subject    Subject pattern used in original subscribe
     * @param type       Type pattern used in original subscribe
     * @throws cMsgException if unsubscribe fails
     */
    public void unsubscribe(cMsg connection, String subject, String type) throws cMsgException {
        if (connection == null) return;

        String key = subject + ":" + type;
        cMsgSubscriptionHandle handle = handles.remove(key);
        if (handle != null) {
            connection.unsubscribe(handle);
        }
    }

    /**
     * Unsubscribe all registered subscriptions. Errors are caught and logged
     * but do not stop cleanup of remaining subscriptions.
     *
     * @param connection cMsg connection to unsubscribe from
     */
    public void unsubscribeAll(cMsg connection) {
        if (connection == null) return;

        for (Map.Entry<String, cMsgSubscriptionHandle> entry : handles.entrySet()) {
            try {
                if (entry.getValue() != null) {
                    connection.unsubscribe(entry.getValue());
                }
            } catch (cMsgException e) {
                System.err.println("Warning: Failed to unsubscribe " + entry.getKey() +
                        ": " + e.getMessage());
                // Continue cleaning up other subscriptions
            }
        }
        handles.clear();
    }

    /**
     * Get the number of active subscriptions being managed.
     *
     * @return Count of registered subscription handles
     */
    public int getSubscriptionCount() {
        return handles.size();
    }

    /**
     * Check if a specific subscription exists.
     *
     * @param subject Subject pattern
     * @param type    Type pattern
     * @return true if subscription is registered
     */
    public boolean hasSubscription(String subject, String type) {
        String key = subject + ":" + type;
        return handles.containsKey(key);
    }
}
