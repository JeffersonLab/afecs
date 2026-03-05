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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for CODA state validation and transition rules.
 *
 * <p>Provides centralized validation logic for state transitions,
 * state name verification, and transition path validation.</p>
 *
 * @author gurjyan
 * @version 4.x
 */
public class StateValidator {

    /** Valid CODA state names */
    private static final Set<String> VALID_STATES = new HashSet<>(Arrays.asList(
            AConstants.booted,
            AConstants.configured,
            AConstants.downloaded,
            AConstants.prestarted,
            AConstants.active,
            AConstants.ended,
            AConstants.paused,
            AConstants.reseted,
            AConstants.emergencyreset,
            AConstants.disconnected,
            AConstants.failed,
            AConstants.udf
    ));

    /** States that indicate a reset operation */
    private static final Set<String> RESET_STATES = new HashSet<>(Arrays.asList(
            AConstants.reseted,
            AConstants.emergencyreset
    ));

    /** States that indicate an error condition */
    private static final Set<String> ERROR_STATES = new HashSet<>(Arrays.asList(
            AConstants.failed,
            AConstants.disconnected
    ));

    /** States that allow normal operations */
    private static final Set<String> OPERATIONAL_STATES = new HashSet<>(Arrays.asList(
            AConstants.configured,
            AConstants.downloaded,
            AConstants.prestarted,
            AConstants.active,
            AConstants.paused,
            AConstants.ended
    ));

    /**
     * Check if a state name is valid.
     *
     * @param state State name to validate
     * @return true if state is a valid CODA state
     */
    public static boolean isValidState(String state) {
        return state != null && VALID_STATES.contains(state);
    }

    /**
     * Check if a state is a reset state.
     *
     * @param state State name to check
     * @return true if state is reseted or emergencyreset
     */
    public static boolean isResetState(String state) {
        return state != null && RESET_STATES.contains(state);
    }

    /**
     * Check if a state indicates an error condition.
     *
     * @param state State name to check
     * @return true if state is failed or disconnected
     */
    public static boolean isErrorState(String state) {
        return state != null && ERROR_STATES.contains(state);
    }

    /**
     * Check if a state is operational (not error, not booted, not udf).
     *
     * @param state State name to check
     * @return true if state allows normal operations
     */
    public static boolean isOperationalState(String state) {
        return state != null && OPERATIONAL_STATES.contains(state);
    }

    /**
     * Check if transition from one state to another is valid.
     *
     * <p>Basic validation rules:</p>
     * <ul>
     * <li>Can always reset from any state</li>
     * <li>Cannot transition from error states except to reset</li>
     * <li>Must be booted before configuring</li>
     * </ul>
     *
     * @param fromState Current state
     * @param toState   Target state
     * @return true if transition is allowed
     */
    public static boolean isValidTransition(String fromState, String toState) {
        if (!isValidState(fromState) || !isValidState(toState)) {
            return false;
        }

        // Can always reset
        if (isResetState(toState)) {
            return true;
        }

        // Cannot transition from error states except to reset
        if (isErrorState(fromState) && !isResetState(toState)) {
            return false;
        }

        // Must be booted to configure
        if (toState.equals(AConstants.configured) &&
            fromState.equals(AConstants.udf)) {
            return false;
        }

        // Generally allow transitions between operational states
        return true;
    }

    /**
     * Get a human-readable description of a state.
     *
     * @param state State name
     * @return Description string
     */
    public static String getStateDescription(String state) {
        if (state == null) {
            return "null";
        }

        switch (state) {
            case AConstants.booted:
                return "System booted and ready";
            case AConstants.configured:
                return "Configuration loaded";
            case AConstants.downloaded:
                return "Code downloaded to components";
            case AConstants.prestarted:
                return "Components prestarted";
            case AConstants.active:
                return "Data acquisition active";
            case AConstants.ended:
                return "Run ended";
            case AConstants.paused:
                return "Data acquisition paused";
            case AConstants.reseted:
                return "System reset";
            case AConstants.emergencyreset:
                return "Emergency reset initiated";
            case AConstants.failed:
                return "System failed";
            case AConstants.disconnected:
                return "Component disconnected";
            case AConstants.udf:
                return "Undefined state";
            default:
                return "Unknown state: " + state;
        }
    }
}
