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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Utility class for building run log XML.
 *
 * <p>Encapsulates the logic for generating XML logs of CODA runs,
 * including run metadata, component statistics, and RTV values.</p>
 *
 * @author gurjyan
 * @version 4.x
 */
public class RunLogXmlBuilder {

    private final SimpleDateFormat dateFormatter;

    public RunLogXmlBuilder(SimpleDateFormat dateFormatter) {
        this.dateFormatter = dateFormatter;
    }

    /**
     * Build complete run log XML including start and optionally end information.
     *
     * @param runType       Run type name
     * @param session       Session name
     * @param supervisor    Supervisor component data
     * @param components    Collection of agent components
     * @param rtvs          Run-time variables map (can be null)
     * @param includeEndTag Whether to include run-end section
     * @return XML string
     */
    public String buildRunLogXml(
            String runType,
            String session,
            AComponent supervisor,
            Collection<CodaRCAgent> components,
            Map<String, String> rtvs,
            boolean includeEndTag) {

        StringBuilder sb = new StringBuilder();

        // Root element
        sb.append("<coda runtype = \"").append(runType).append("\"");
        sb.append(" session = \"").append(session).append("\">").append("\n");

        // Run start section
        sb.append("   <run-start>").append("\n");
        appendRunMetadata(sb, supervisor);
        appendComponentsSection(sb, components);
        if (rtvs != null && !rtvs.isEmpty()) {
            appendRtvSection(sb, rtvs);
        }
        sb.append("      <update-time>").append(dateFormatter.format(new Date())).append("</update-time>").append("\n");
        sb.append("      <total-evt>").append(supervisor.getEventNumber()).append("</total-evt>").append("\n");
        sb.append("   </run-start>").append("\n");

        // Run end section (optional)
        if (includeEndTag) {
            sb.append("   <run-end>").append("\n");
            sb.append("      <end-time>").append(supervisor.getRunEndTime()).append("</end-time>").append("\n");
            sb.append("      <total-evt>").append(supervisor.getEventNumber()).append("</total-evt>").append("\n");
            appendComponentsSection(sb, components);
            sb.append("   </run-end>").append("\n");
        }

        sb.append("</coda>").append("\n");

        return sb.toString();
    }

    /**
     * Append run metadata (run number, start time, output files).
     */
    private void appendRunMetadata(StringBuilder sb, AComponent supervisor) {
        sb.append("      <run-number>").append(supervisor.getRunNumber()).append("</run-number>").append("\n");
        sb.append("      <start-time>").append(supervisor.getRunStartTime()).append("</start-time>").append("\n");

        if (supervisor.getDestinationNames() != null) {
            for (String fileName : supervisor.getDestinationNames()) {
                sb.append("      <out-file>").append(fileName).append("</out-file>").append("\n");
            }
        }
    }

    /**
     * Append components section with component statistics.
     */
    private void appendComponentsSection(StringBuilder sb, Collection<CodaRCAgent> components) {
        sb.append("      <components>").append("\n");
        for (CodaRCAgent comp : components) {
            appendComponentData(sb, comp.me);
        }
        sb.append("      </components>").append("\n");
    }

    /**
     * Append single component data.
     */
    private void appendComponentData(StringBuilder sb, AComponent comp) {
        sb.append("         <component name = \"").append(comp.getName()).append("\"");
        sb.append(" type = \"").append(comp.getType()).append("\">").append("\n");

        sb.append("            <evt-rate>").append(comp.getEventRateAverage()).append("</evt-rate>").append("\n");
        sb.append("            <data-rate>").append(comp.getDataRateAverage()).append("</data-rate>").append("\n");
        sb.append("            <evt-number>").append(comp.getEventNumber()).append("</evt-number>").append("\n");
        sb.append("            <min-evt-size>").append(comp.getMinEventSize()).append("</min-evt-size>").append("\n");
        sb.append("            <max-evt-size>").append(comp.getMaxEventSize()).append("</max-evt-size>").append("\n");
        sb.append("            <average-evt-size>").append(comp.getAvgEventSize()).append("</average-evt-size>").append("\n");
        sb.append("            <min-evt-build-time>").append(comp.getMinTimeToBuild()).append("</min-evt-build-time>").append("\n");
        sb.append("            <max-evt-build-time>").append(comp.getMaxTimeToBuild()).append("</max-evt-build-time>").append("\n");
        sb.append("            <average-evt-build-time>").append(comp.getMeanTimeToBuild()).append("</average-evt-build-time>").append("\n");
        sb.append("            <chunk-x-et-buffer>").append(comp.getChunkXEtBuffer()).append("</chunk-x-et-buffer>").append("\n");

        if (comp.getDestinationNames() != null) {
            for (String fileName : comp.getDestinationNames()) {
                sb.append("            <out-file>").append(fileName).append("</out-file>").append("\n");
            }
        }

        sb.append("         </component>").append("\n");
    }

    /**
     * Append RTVs (run-time variables) section.
     */
    private void appendRtvSection(StringBuilder sb, Map<String, String> rtvs) {
        sb.append("      <rtvs>").append("\n");
        for (Map.Entry<String, String> entry : rtvs.entrySet()) {
            String value = entry.getValue();
            if (value.contains("&")) {
                value = value.replaceAll("&", "&amp;");
            }
            sb.append("         <rtv name = \"").append(entry.getKey()).append("\"");
            sb.append(" value = \"").append(value).append("\"/>").append("\n");
        }
        sb.append("      </rtvs>").append("\n");
    }
}
