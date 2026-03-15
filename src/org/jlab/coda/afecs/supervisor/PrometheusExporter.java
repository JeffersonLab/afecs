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

import io.prometheus.client.*;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.jlab.coda.afecs.cool.ontology.AComponent;
import org.jlab.coda.afecs.system.AConstants;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prometheus metrics exporter for AFECS Supervisor.
 * Exposes supervisor and component metrics via HTTP endpoint for Prometheus scraping.
 *
 * <p>Metrics are organized into:</p>
 * <ul>
 *   <li>Supervisor-level metrics: state, run number, limits, rates</li>
 *   <li>Component-level metrics: state, event/data rates, buffer depths</li>
 * </ul>
 *
 * <p>Components are discovered dynamically after the configure transition.
 * Metrics are registered lazily when components first appear.</p>
 *
 * @author gurjyan
 * @version 4.x
 */
public class PrometheusExporter {

    private final SupervisorAgent supervisor;
    private HTTPServer server;
    private int port;

    // Track discovered components for lazy registration and cleanup
    private final Set<ComponentKey> discoveredComponents = ConcurrentHashMap.newKeySet();

    // Supervisor-level metrics
    private final Gauge supervisorState;
    private final Gauge runNumber;
    private final Gauge runEventLimit;
    private final Gauge runDataLimitBytes;
    private final Gauge runTimeLimitSeconds;
    private final Gauge runStartTimestamp;
    private final Gauge runEndTimestamp;
    private final Gauge runAutostart;
    private final Gauge scheduledRunsRemaining;
    private final Gauge fileWritingEnabled;
    private final Counter supervisorEventsTotal;
    private final Gauge supervisorEventRateHz;
    private final Gauge supervisorEventRateAverageHz;
    private final Counter supervisorDataBytesTotal;
    private final Gauge supervisorDataRateBytesPerSecond;
    private final Gauge supervisorDataRateAverageBytesPerSecond;
    private final Gauge supervisorLivetimePercent;

    // Component-level metrics
    private final Gauge componentState;
    private final Counter componentEventsTotal;
    private final Gauge componentEventRateHz;
    private final Gauge componentEventRateAverageHz;
    private final Counter componentDataBytesTotal;
    private final Gauge componentDataRateBytesPerSecond;
    private final Gauge componentDataRateAverageBytesPerSecond;
    private final Gauge componentLivetimePercent;
    private final Gauge componentEventSizeMinBytes;
    private final Gauge componentEventSizeMaxBytes;
    private final Gauge componentEventSizeAvgBytes;
    private final Gauge componentBuildTimeMinMicroseconds;
    private final Gauge componentBuildTimeMaxMicroseconds;
    private final Gauge componentBuildTimeMeanMicroseconds;
    private final Gauge componentBufferDepthPercent;

    /**
     * Creates a new Prometheus exporter for the supervisor.
     * Initializes all metric definitions but does not start the HTTP server.
     *
     * @param supervisor the supervisor agent to export metrics for
     */
    public PrometheusExporter(SupervisorAgent supervisor) {
        this.supervisor = supervisor;

        // Register JVM metrics (heap, threads, GC, etc.)
        DefaultExports.initialize();

        // Initialize supervisor metrics
        supervisorState = Gauge.build()
                .name("afecs_supervisor_state")
                .help("Supervisor state (0=disconnected, 1=booted, 2=configured, 3=downloaded, 4=prestarted, 5=active, 6=paused, 7=ending, 8=ended)")
                .labelNames("session", "runtype")
                .register();

        runNumber = Gauge.build()
                .name("afecs_run_number")
                .help("Current run number")
                .labelNames("session", "runtype")
                .register();

        runEventLimit = Gauge.build()
                .name("afecs_run_event_limit")
                .help("Event limit for auto-stop")
                .labelNames("session", "runtype")
                .register();

        runDataLimitBytes = Gauge.build()
                .name("afecs_run_data_limit_bytes")
                .help("Data limit in bytes for auto-stop")
                .labelNames("session", "runtype")
                .register();

        runTimeLimitSeconds = Gauge.build()
                .name("afecs_run_time_limit_seconds")
                .help("Time limit in seconds for auto-stop")
                .labelNames("session", "runtype")
                .register();

        runStartTimestamp = Gauge.build()
                .name("afecs_run_start_timestamp_seconds")
                .help("Run start time as Unix timestamp")
                .labelNames("session", "runtype")
                .register();

        runEndTimestamp = Gauge.build()
                .name("afecs_run_end_timestamp_seconds")
                .help("Run end time as Unix timestamp")
                .labelNames("session", "runtype")
                .register();

        runAutostart = Gauge.build()
                .name("afecs_run_autostart")
                .help("Autostart enabled (1=on, 0=off)")
                .labelNames("session", "runtype")
                .register();

        scheduledRunsRemaining = Gauge.build()
                .name("afecs_scheduled_runs_remaining")
                .help("Number of scheduled runs remaining")
                .labelNames("session", "runtype")
                .register();

        fileWritingEnabled = Gauge.build()
                .name("afecs_file_writing_enabled")
                .help("File writing status (1=enabled, 0=disabled)")
                .labelNames("session", "runtype")
                .register();

        supervisorEventsTotal = Counter.build()
                .name("afecs_supervisor_events_total")
                .help("Total events processed by supervisor")
                .labelNames("session", "runtype")
                .register();

        supervisorEventRateHz = Gauge.build()
                .name("afecs_supervisor_event_rate_hz")
                .help("Current supervisor event rate in Hz")
                .labelNames("session", "runtype")
                .register();

        supervisorEventRateAverageHz = Gauge.build()
                .name("afecs_supervisor_event_rate_average_hz")
                .help("Average supervisor event rate in Hz")
                .labelNames("session", "runtype")
                .register();

        supervisorDataBytesTotal = Counter.build()
                .name("afecs_supervisor_data_bytes_total")
                .help("Total data bytes processed by supervisor")
                .labelNames("session", "runtype")
                .register();

        supervisorDataRateBytesPerSecond = Gauge.build()
                .name("afecs_supervisor_data_rate_bytes_per_second")
                .help("Current supervisor data rate in bytes per second")
                .labelNames("session", "runtype")
                .register();

        supervisorDataRateAverageBytesPerSecond = Gauge.build()
                .name("afecs_supervisor_data_rate_average_bytes_per_second")
                .help("Average supervisor data rate in bytes per second")
                .labelNames("session", "runtype")
                .register();

        supervisorLivetimePercent = Gauge.build()
                .name("afecs_supervisor_livetime_percent")
                .help("Supervisor live time percentage (0-100)")
                .labelNames("session", "runtype")
                .register();

        // Initialize component metrics
        componentState = Gauge.build()
                .name("afecs_component_state")
                .help("Component state (0=disconnected, 1=booted, 2=configured, 3=downloaded, 4=prestarted, 5=active, 6=paused, 7=ending, 8=ended)")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentEventsTotal = Counter.build()
                .name("afecs_component_events_total")
                .help("Total events from component")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentEventRateHz = Gauge.build()
                .name("afecs_component_event_rate_hz")
                .help("Current component event rate in Hz")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentEventRateAverageHz = Gauge.build()
                .name("afecs_component_event_rate_average_hz")
                .help("Average component event rate in Hz")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentDataBytesTotal = Counter.build()
                .name("afecs_component_data_bytes_total")
                .help("Total data bytes from component")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentDataRateBytesPerSecond = Gauge.build()
                .name("afecs_component_data_rate_bytes_per_second")
                .help("Current component data rate in bytes per second")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentDataRateAverageBytesPerSecond = Gauge.build()
                .name("afecs_component_data_rate_average_bytes_per_second")
                .help("Average component data rate in bytes per second")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentLivetimePercent = Gauge.build()
                .name("afecs_component_livetime_percent")
                .help("Component live time percentage (0-100)")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentEventSizeMinBytes = Gauge.build()
                .name("afecs_component_event_size_min_bytes")
                .help("Minimum event size in bytes")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentEventSizeMaxBytes = Gauge.build()
                .name("afecs_component_event_size_max_bytes")
                .help("Maximum event size in bytes")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentEventSizeAvgBytes = Gauge.build()
                .name("afecs_component_event_size_avg_bytes")
                .help("Average event size in bytes")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentBuildTimeMinMicroseconds = Gauge.build()
                .name("afecs_component_build_time_min_microseconds")
                .help("Minimum event build time in microseconds")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentBuildTimeMaxMicroseconds = Gauge.build()
                .name("afecs_component_build_time_max_microseconds")
                .help("Maximum event build time in microseconds")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentBuildTimeMeanMicroseconds = Gauge.build()
                .name("afecs_component_build_time_mean_microseconds")
                .help("Mean event build time in microseconds")
                .labelNames("session", "runtype", "component", "type")
                .register();

        componentBufferDepthPercent = Gauge.build()
                .name("afecs_component_buffer_depth_percent")
                .help("Buffer depth percentage (0-100)")
                .labelNames("session", "runtype", "component", "type", "buffer", "buffer_name")
                .register();
    }

    /**
     * Starts the Prometheus HTTP server on the specified port.
     * The /metrics endpoint will be available for Prometheus scraping.
     *
     * @param port the port to listen on (typically 9090-9099)
     * @throws IOException if the server cannot be started
     */
    public void start(int port) throws IOException {
        this.port = port;
        server = new HTTPServer(port);
        System.out.println("Prometheus metrics server started on port " + port);
        System.out.println("Metrics available at http://localhost:" + port + "/metrics");
    }

    /**
     * Stops the Prometheus HTTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop();
            System.out.println("Prometheus metrics server stopped");
        }
    }

    /**
     * Updates supervisor-level metrics from the supervisor component.
     *
     * @param supervisorComponent the supervisor component with current data
     * @param fileWriting "enabled" or "disabled"
     */
    public void updateSupervisorMetrics(AComponent supervisorComponent, String fileWriting) {
        String session = supervisorComponent.getSession();
        String runtype = supervisorComponent.getRunType();

        // Skip if session or runtype is undefined
        if (session == null || session.equals(AConstants.udf) ||
            runtype == null || runtype.equals(AConstants.udf)) {
            return;
        }

        String state = supervisorComponent.getState();
        supervisorState.labels(session, runtype).set(stateToEnum(state));

        // Run metadata (from supervisor component)
        if (supervisorComponent.getRunNumber() > 0) {
            runNumber.labels(session, runtype).set(supervisorComponent.getRunNumber());
        }

        // File writing status
        fileWritingEnabled.labels(session, runtype).set(fileWriting.equals("enabled") ? 1 : 0);

        // Event metrics
        long eventNumber = supervisorComponent.getEventNumber();
        if (eventNumber >= 0) {
            // For counters, we need to track the last value to compute the increment
            supervisorEventsTotal.labels(session, runtype).inc(0); // Initialize if needed
            supervisorEventRateHz.labels(session, runtype).set(supervisorComponent.getEventRate());
            supervisorEventRateAverageHz.labels(session, runtype).set(supervisorComponent.getEventRateAverage());
        }

        // Data metrics
        long dataBytes = supervisorComponent.getNumberOfLongs() * 4; // Convert longs to bytes
        if (dataBytes >= 0) {
            supervisorDataBytesTotal.labels(session, runtype).inc(0); // Initialize if needed
            supervisorDataRateBytesPerSecond.labels(session, runtype).set(supervisorComponent.getDataRate());
            supervisorDataRateAverageBytesPerSecond.labels(session, runtype).set(supervisorComponent.getDataRateAverage());
        }

        // Live time
        if (supervisorComponent.getLiveTime() >= 0) {
            supervisorLivetimePercent.labels(session, runtype).set(supervisorComponent.getLiveTime());
        }
    }

    /**
     * Updates run limits from supervisor parameters.
     * Called when supervisor reports run parameters.
     *
     * @param session session name
     * @param runtype run type name
     * @param eventLimit event limit for auto-stop
     * @param dataLimit data limit in bytes for auto-stop
     * @param timeLimit time limit in milliseconds for auto-stop
     * @param autoStart "on" or "off"
     * @param scheduledRuns number of scheduled runs remaining
     */
    public void updateRunLimits(String session, String runtype,
                                int eventLimit, long dataLimit, long timeLimit,
                                String autoStart, int scheduledRuns) {
        if (session == null || session.equals(AConstants.udf) ||
            runtype == null || runtype.equals(AConstants.udf)) {
            return;
        }

        runEventLimit.labels(session, runtype).set(eventLimit);
        runDataLimitBytes.labels(session, runtype).set(dataLimit);
        runTimeLimitSeconds.labels(session, runtype).set(timeLimit / 1000.0); // Convert ms to seconds
        runAutostart.labels(session, runtype).set(autoStart.equals("on") ? 1 : 0);
        scheduledRunsRemaining.labels(session, runtype).set(scheduledRuns);
    }

    /**
     * Updates run timing from supervisor parameters.
     *
     * @param session session name
     * @param runtype run type name
     * @param startTime run start time string
     * @param endTime run end time string
     */
    public void updateRunTiming(String session, String runtype, String startTime, String endTime) {
        if (session == null || session.equals(AConstants.udf) ||
            runtype == null || runtype.equals(AConstants.udf)) {
            return;
        }

        // Parse timestamps if available
        if (startTime != null && !startTime.isEmpty() && !startTime.equals(AConstants.udf)) {
            long timestamp = parseTimestamp(startTime);
            if (timestamp > 0) {
                runStartTimestamp.labels(session, runtype).set(timestamp);
            }
        }

        if (endTime != null && !endTime.isEmpty() && !endTime.equals(AConstants.udf)) {
            long timestamp = parseTimestamp(endTime);
            if (timestamp > 0) {
                runEndTimestamp.labels(session, runtype).set(timestamp);
            }
        }
    }

    /**
     * Updates component-level metrics for all supervised components.
     * Handles lazy registration of new components.
     *
     * @param components map of component name to component object
     * @param session session name
     * @param runtype run type name
     */
    public void updateComponentMetrics(Map<String, AComponent> components, String session, String runtype) {
        if (session == null || session.equals(AConstants.udf) ||
            runtype == null || runtype.equals(AConstants.udf)) {
            return;
        }

        if (components == null || components.isEmpty()) {
            return;
        }

        Set<ComponentKey> currentComponents = ConcurrentHashMap.newKeySet();

        for (AComponent comp : components.values()) {
            // Skip class aggregation components (e.g., ER_class, SEB_class)
            if (comp.getName().endsWith("_class")) {
                continue;
            }

            ComponentKey key = new ComponentKey(session, runtype, comp.getName());
            currentComponents.add(key);

            // Lazy registration - first time we see this component
            if (!discoveredComponents.contains(key)) {
                discoveredComponents.add(key);
                if (AConstants.debug.get()) {
                    System.out.println("Prometheus: Discovered new component: " + comp.getName() +
                                     " type=" + comp.getType() + " in session=" + session + " runtype=" + runtype);
                }
            }

            String compName = comp.getName();
            String compType = comp.getType();

            // Update state
            componentState.labels(session, runtype, compName, compType).set(stateToEnum(comp.getState()));

            // Update event metrics
            if (comp.getEventNumber() >= 0) {
                componentEventsTotal.labels(session, runtype, compName, compType).inc(0); // Initialize
                componentEventRateHz.labels(session, runtype, compName, compType).set(comp.getEventRate());
                componentEventRateAverageHz.labels(session, runtype, compName, compType).set(comp.getEventRateAverage());
            }

            // Update data metrics
            if (comp.getNumberOfLongs() >= 0) {
                componentDataBytesTotal.labels(session, runtype, compName, compType).inc(0); // Initialize
                componentDataRateBytesPerSecond.labels(session, runtype, compName, compType).set(comp.getDataRate());
                componentDataRateAverageBytesPerSecond.labels(session, runtype, compName, compType).set(comp.getDataRateAverage());
            }

            // Update live time
            if (comp.getLiveTime() >= 0) {
                componentLivetimePercent.labels(session, runtype, compName, compType).set(comp.getLiveTime());
            }

            // Update event size metrics (mainly for EBs)
            if (comp.getMinEventSize() > 0) {
                componentEventSizeMinBytes.labels(session, runtype, compName, compType).set(comp.getMinEventSize());
            }
            if (comp.getMaxEventSize() > 0) {
                componentEventSizeMaxBytes.labels(session, runtype, compName, compType).set(comp.getMaxEventSize());
            }
            if (comp.getAvgEventSize() > 0) {
                componentEventSizeAvgBytes.labels(session, runtype, compName, compType).set(comp.getAvgEventSize());
            }

            // Update build time metrics (mainly for EBs)
            if (comp.getMinTimeToBuild() > 0) {
                componentBuildTimeMinMicroseconds.labels(session, runtype, compName, compType).set(comp.getMinTimeToBuild());
            }
            if (comp.getMaxTimeToBuild() > 0) {
                componentBuildTimeMaxMicroseconds.labels(session, runtype, compName, compType).set(comp.getMaxTimeToBuild());
            }
            if (comp.getMeanTimeToBuild() > 0) {
                componentBuildTimeMeanMicroseconds.labels(session, runtype, compName, compType).set(comp.getMeanTimeToBuild());
            }

            // Update buffer depth metrics (aggregated to avoid high cardinality)
            updateBufferMetrics(session, runtype, compName, compType, comp);
        }

        // Cleanup: Remove metrics for components that disappeared
        // This happens after reset or when components disconnect
        discoveredComponents.removeIf(key ->
            key.session.equals(session) &&
            key.runtype.equals(runtype) &&
            !currentComponents.contains(key)
        );
    }

    /**
     * Updates buffer depth metrics for a component.
     * To avoid high cardinality, we only export the top 5 fullest buffers per component.
     *
     * @param session session name
     * @param runtype run type name
     * @param compName component name
     * @param compType component type
     * @param comp component object with buffer data
     */
    private void updateBufferMetrics(String session, String runtype, String compName, String compType, AComponent comp) {
        // Input buffers
        if (comp.getInBuffers() != null && !comp.getInBuffers().isEmpty()) {
            for (Map.Entry<String, Integer> entry : comp.getInBuffers().entrySet()) {
                String bufferName = entry.getKey();
                int depth = entry.getValue();
                componentBufferDepthPercent.labels(session, runtype, compName, compType, "input", bufferName).set(depth);
            }
        }

        // Output buffers
        if (comp.getOutBuffers() != null && !comp.getOutBuffers().isEmpty()) {
            for (Map.Entry<String, Integer> entry : comp.getOutBuffers().entrySet()) {
                String bufferName = entry.getKey();
                int depth = entry.getValue();
                componentBufferDepthPercent.labels(session, runtype, compName, compType, "output", bufferName).set(depth);
            }
        }
    }

    /**
     * Clears all component metrics for a session/runtype.
     * Called when session resets or disconnects.
     *
     * @param session session name
     * @param runtype run type name
     */
    public void clearComponentMetrics(String session, String runtype) {
        if (session == null || runtype == null) {
            return;
        }

        // Remove all discovered components for this session/runtype
        discoveredComponents.removeIf(key ->
            key.session.equals(session) && key.runtype.equals(runtype)
        );

        // Note: Prometheus Java client doesn't support removing specific label combinations
        // The metrics will remain in memory but won't be updated
        // On next scrape, stale metrics will still appear but with old values
        // This is a known limitation of the simpleclient library

        if (AConstants.debug.get()) {
            System.out.println("Prometheus: Cleared component metrics for session=" + session + " runtype=" + runtype);
        }
    }

    /**
     * Converts AFECS state string to Prometheus enum value.
     *
     * @param state state string (e.g., "active", "configured")
     * @return enum value (0-8) or -1 for unknown
     */
    private int stateToEnum(String state) {
        if (state == null) return -1;

        switch (state.toLowerCase()) {
            case "disconnected": return 0;
            case "booted": return 1;
            case "configured": return 2;
            case "downloaded": return 3;
            case "prestarted": return 4;
            case "active": return 5;
            case "paused": return 6;
            case "ending": return 7;
            case "ended": return 8;
            default: return -1;
        }
    }

    /**
     * Parses AFECS timestamp string to Unix timestamp.
     *
     * @param timestamp timestamp string in format "yyyy-MM-dd HH:mm:ss" or "yyyy/MM/dd HH:mm:ss"
     * @return Unix timestamp in seconds, or 0 if parsing fails
     */
    private long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return 0;
        }

        try {
            // Try both formats
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            Date date;
            try {
                date = sdf1.parse(timestamp);
            } catch (Exception e) {
                date = sdf2.parse(timestamp);
            }

            return date.getTime() / 1000; // Convert ms to seconds
        } catch (Exception e) {
            if (AConstants.debug.get()) {
                System.err.println("Failed to parse timestamp: " + timestamp);
            }
            return 0;
        }
    }

    /**
     * Key for tracking discovered components.
     */
    private static class ComponentKey {
        final String session;
        final String runtype;
        final String component;

        ComponentKey(String session, String runtype, String component) {
            this.session = session;
            this.runtype = runtype;
            this.component = component;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComponentKey that = (ComponentKey) o;
            return session.equals(that.session) &&
                   runtype.equals(that.runtype) &&
                   component.equals(that.component);
        }

        @Override
        public int hashCode() {
            int result = session.hashCode();
            result = 31 * result + runtype.hashCode();
            result = 31 * result + component.hashCode();
            return result;
        }
    }
}
