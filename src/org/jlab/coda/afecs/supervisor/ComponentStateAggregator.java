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

import org.jlab.coda.afecs.codarc.CodaRCAgent;
import org.jlab.coda.afecs.system.AConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for aggregating component states.
 *
 * <p>Provides methods to analyze the collective state of multiple
 * CODA components, useful for determining supervisor intermediate
 * states during transitions.</p>
 *
 * @author gurjyan
 * @version 4.x
 */
public class ComponentStateAggregator {

    /**
     * Check if all components are in a specific state.
     *
     * @param statuses List of component states
     * @param state    Target state to check
     * @return true if all components are in the target state
     */
    public static boolean areAllInState(List<String> statuses, String state) {
        if (statuses == null || statuses.isEmpty()) {
            return false;
        }
        for (String s : statuses) {
            if (!s.equals(state)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if all components are in a specific state.
     *
     * @param components Collection of agents
     * @param state      Target state to check
     * @return true if all components are in the target state
     */
    public static boolean areAllInState(Collection<CodaRCAgent> components, String state) {
        if (components == null || components.isEmpty()) {
            return false;
        }
        for (CodaRCAgent agent : components) {
            if (!agent.me.getState().equals(state)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if any component is in a specific state.
     *
     * @param statuses List of component states
     * @param state    Target state to check
     * @return true if at least one component is in the target state
     */
    public static boolean isAnyInState(List<String> statuses, String state) {
        if (statuses == null || statuses.isEmpty()) {
            return false;
        }
        return statuses.contains(state);
    }

    /**
     * Get list of all component states.
     *
     * @param components Collection of agents
     * @return List of state strings
     */
    public static List<String> getComponentStates(Collection<CodaRCAgent> components) {
        List<String> states = new ArrayList<>();
        if (components != null) {
            for (CodaRCAgent c : components) {
                states.add(c.me.getState());
            }
        }
        return states;
    }

    /**
     * Determine intermediate state during CodaRcStartRun service execution.
     *
     * <p>Analyzes component states to determine the overall supervisor
     * intermediate state during a run start transition.</p>
     *
     * @param components       Collection of supervised agents
     * @param requestedService Name of currently requested service
     * @return Intermediate state name, or AConstants.udf if not applicable
     */
    public static String determineIntermediateState(
            Collection<CodaRCAgent> components,
            String requestedService) {

        if (requestedService == null || !requestedService.equals("CodaRcStartRun")) {
            return AConstants.udf;
        }

        List<String> statuses = getComponentStates(components);

        // Check for downloading state
        if (isAnyInState(statuses, "downloading")) {
            return "downloading";
        }

        // Check if all downloaded
        if (areAllInState(statuses, AConstants.downloaded)) {
            return AConstants.downloaded;
        }

        // Check for prestarting state
        if (isAnyInState(statuses, "prestarting")) {
            return "prestarting";
        }

        // Check if all paused (means prestarted completed)
        if (areAllInState(statuses, AConstants.paused)) {
            return AConstants.prestarted;
        }

        // Check for activating state
        if (isAnyInState(statuses, "activating")) {
            return "activating";
        }

        // Check if all active
        if (areAllInState(statuses, AConstants.active)) {
            return AConstants.active;
        }

        return AConstants.udf;
    }
}
