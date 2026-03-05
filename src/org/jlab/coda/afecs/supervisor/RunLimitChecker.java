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

package org.jlab.coda.afecs.supervisor;

import org.jlab.coda.afecs.system.AConstants;
import org.jlab.coda.afecs.system.util.AfecsTool;

/**
 * Utility class for checking run limits (event, time, data).
 *
 * <p>Encapsulates the logic for determining if a run has exceeded
 * its configured limits. Used by SupervisorAgent to decide when
 * to automatically stop a run.</p>
 *
 * @author gurjyan
 * @version 4.x
 */
public class RunLimitChecker {

    /**
     * Result of a limit check operation.
     */
    public enum LimitExceeded {
        NONE,
        EVENT_LIMIT,
        TIME_LIMIT,
        DATA_LIMIT
    }

    /**
     * Check if any run limits have been exceeded.
     *
     * @param currentState Current supervisor state
     * @param eventLimit   Configured event limit (0 = no limit)
     * @param eventNumber  Current event number
     * @param timeLimit    Configured time limit in milliseconds (0 = no limit)
     * @param runStartTime Run start time in milliseconds
     * @param dataLimit    Configured data limit (0 = no limit)
     * @param numberOfLongs Current number of longs
     * @return LimitExceeded enum indicating which limit was exceeded, or NONE
     */
    public static LimitExceeded checkLimits(
            String currentState,
            long eventLimit,
            long eventNumber,
            long timeLimit,
            long runStartTime,
            long dataLimit,
            long numberOfLongs) {

        // Only check limits when in active state
        if (!AConstants.active.equals(currentState)) {
            return LimitExceeded.NONE;
        }

        // Check event limit
        if (eventLimit > 0 && eventNumber >= eventLimit) {
            return LimitExceeded.EVENT_LIMIT;
        }

        // Check time limit
        if (timeLimit > 0) {
            long currentTime = AfecsTool.getCurrentTimeInMs();
            if (currentTime >= (runStartTime + timeLimit)) {
                return LimitExceeded.TIME_LIMIT;
            }
        }

        // Check data limit
        if (dataLimit > 0 && numberOfLongs >= dataLimit) {
            return LimitExceeded.DATA_LIMIT;
        }

        return LimitExceeded.NONE;
    }

    /**
     * Get a human-readable description of the limit that was exceeded.
     *
     * @param limitExceeded The limit that was exceeded
     * @return Description string
     */
    public static String getLimitDescription(LimitExceeded limitExceeded) {
        switch (limitExceeded) {
            case EVENT_LIMIT:
                return "Event limit reached";
            case TIME_LIMIT:
                return "Time limit reached";
            case DATA_LIMIT:
                return "Data limit reached";
            case NONE:
            default:
                return "No limit exceeded";
        }
    }
}
