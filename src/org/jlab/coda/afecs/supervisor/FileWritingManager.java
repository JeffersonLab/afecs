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

import org.jlab.coda.afecs.cool.ontology.AComponent;
import org.jlab.coda.afecs.codarc.CodaRCAgent;
import org.jlab.coda.afecs.system.ACodaType;

import java.util.Collection;

/**
 * Utility class for managing file writing settings across components.
 *
 * <p>Handles enabling/disabling file writing on persistency components
 * based on component types (ER, PEB, SEB) or individual components.</p>
 *
 * @author gurjyan
 * @version 4.x
 */
public class FileWritingManager {

    /**
     * Set file writing enabled/disabled on appropriate components.
     *
     * @param components           Collection of all supervised agents
     * @param persistencyComponent The component responsible for persistency
     * @param fileWritingSetting   Setting value ("enabled" or "disabled")
     */
    public static void setFileWriting(
            Collection<CodaRCAgent> components,
            AComponent persistencyComponent,
            String fileWritingSetting) {

        if (persistencyComponent == null) {
            return;
        }

        // Check if persistency component is a class type (e.g., ER_class, PEB_class)
        if (persistencyComponent.getName().contains("class")) {
            setFileWritingByClass(components, persistencyComponent.getName(), fileWritingSetting);
        } else {
            // Set file writing on specific named component
            setFileWritingByName(components, persistencyComponent.getName(), fileWritingSetting);
        }
    }

    /**
     * Set file writing on all components of a specific class type.
     */
    private static void setFileWritingByClass(
            Collection<CodaRCAgent> components,
            String className,
            String fileWritingSetting) {

        ACodaType targetType = null;

        switch (className) {
            case "ER_class":
                targetType = ACodaType.ER;
                break;
            case "PEB_class":
                targetType = ACodaType.PEB;
                break;
            case "SEB_class":
                targetType = ACodaType.SEB;
                break;
        }

        if (targetType != null) {
            for (CodaRCAgent agent : components) {
                if (agent.me.getType().equals(targetType.name())) {
                    System.out.println("DDD ----| Info: setting fileWriting = " +
                            fileWritingSetting + " for component = " + agent.me.getName());
                    agent.agentControlRequestSetFileWriting(fileWritingSetting);
                }
            }
        }
    }

    /**
     * Set file writing on a specific named component.
     */
    private static void setFileWritingByName(
            Collection<CodaRCAgent> components,
            String componentName,
            String fileWritingSetting) {

        for (CodaRCAgent agent : components) {
            if (agent.me.getName().equals(componentName)) {
                agent.agentControlRequestSetFileWriting(fileWritingSetting);
                break;
            }
        }
    }
}
