# AFECS Prometheus Exporter

The AFECS Supervisor now includes an embedded Prometheus metrics exporter for monitoring and observability of the CODA DAQ system.

## Quick Start

### 1. Build AFECS

```bash
./gradlew build
```

### 2. Start Platform with Prometheus Enabled

The Prometheus exporter starts automatically when the Supervisor is created.

**Default port (9090):**
```bash
bin/platform
```

**Custom port:**
```bash
# Via system property
java -Dprometheus.port=9091 -jar build/libs/afecs-4.0.0-SNAPSHOT.jar

# Or in bin/platform script, add:
JVM_OPTS="$JVM_OPTS -Dprometheus.port=9091"
```

### 3. Verify Metrics Endpoint

```bash
curl http://localhost:9090/metrics
```

You should see Prometheus metrics output including:
```
# HELP afecs_supervisor_state Supervisor state (0=disconnected, 1=booted, 2=configured...)
# TYPE afecs_supervisor_state gauge
afecs_supervisor_state{session="expid",runtype="physics"} 1.0

# HELP afecs_run_number Current run number
# TYPE afecs_run_number gauge
afecs_run_number{session="expid",runtype="physics"} 1234.0

# ... many more metrics
```

## Metrics Reference

### Supervisor Metrics

These metrics are available immediately when the supervisor starts:

| Metric Name | Type | Labels | Description |
|------------|------|--------|-------------|
| `afecs_supervisor_state` | Gauge | session, runtype | Supervisor state (0-8) |
| `afecs_run_number` | Gauge | session, runtype | Current run number |
| `afecs_run_event_limit` | Gauge | session, runtype | Event limit for auto-stop |
| `afecs_run_data_limit_bytes` | Gauge | session, runtype | Data limit in bytes |
| `afecs_run_time_limit_seconds` | Gauge | session, runtype | Time limit in seconds |
| `afecs_run_start_timestamp_seconds` | Gauge | session, runtype | Run start time (Unix timestamp) |
| `afecs_run_end_timestamp_seconds` | Gauge | session, runtype | Run end time (Unix timestamp) |
| `afecs_run_autostart` | Gauge | session, runtype | Autostart enabled (1=on, 0=off) |
| `afecs_scheduled_runs_remaining` | Gauge | session, runtype | Scheduled runs remaining |
| `afecs_file_writing_enabled` | Gauge | session, runtype | File writing status (1=enabled, 0=disabled) |
| `afecs_supervisor_events_total` | Counter | session, runtype | Total events processed |
| `afecs_supervisor_event_rate_hz` | Gauge | session, runtype | Current event rate |
| `afecs_supervisor_event_rate_average_hz` | Gauge | session, runtype | Average event rate |
| `afecs_supervisor_data_bytes_total` | Counter | session, runtype | Total data bytes processed |
| `afecs_supervisor_data_rate_bytes_per_second` | Gauge | session, runtype | Current data rate |
| `afecs_supervisor_data_rate_average_bytes_per_second` | Gauge | session, runtype | Average data rate |
| `afecs_supervisor_livetime_percent` | Gauge | session, runtype | Live time percentage (0-100) |

### Component Metrics

These metrics are created **dynamically after the Configure transition** as components are discovered:

| Metric Name | Type | Labels | Description |
|------------|------|--------|-------------|
| `afecs_component_state` | Gauge | session, runtype, component, type | Component state (0-8) |
| `afecs_component_events_total` | Counter | session, runtype, component, type | Total events from component |
| `afecs_component_event_rate_hz` | Gauge | session, runtype, component, type | Current event rate |
| `afecs_component_event_rate_average_hz` | Gauge | session, runtype, component, type | Average event rate |
| `afecs_component_data_bytes_total` | Counter | session, runtype, component, type | Total data bytes |
| `afecs_component_data_rate_bytes_per_second` | Gauge | session, runtype, component, type | Current data rate |
| `afecs_component_data_rate_average_bytes_per_second` | Gauge | session, runtype, component, type | Average data rate |
| `afecs_component_livetime_percent` | Gauge | session, runtype, component, type | Live time percentage |
| `afecs_component_event_size_min_bytes` | Gauge | session, runtype, component, type | Min event size (EBs only) |
| `afecs_component_event_size_max_bytes` | Gauge | session, runtype, component, type | Max event size (EBs only) |
| `afecs_component_event_size_avg_bytes` | Gauge | session, runtype, component, type | Avg event size (EBs only) |
| `afecs_component_build_time_min_microseconds` | Gauge | session, runtype, component, type | Min build time (EBs only) |
| `afecs_component_build_time_max_microseconds` | Gauge | session, runtype, component, type | Max build time (EBs only) |
| `afecs_component_build_time_mean_microseconds` | Gauge | session, runtype, component, type | Mean build time (EBs only) |
| `afecs_component_buffer_depth_percent` | Gauge | session, runtype, component, type, buffer, buffer_name | Buffer depth (0-100) |

### JVM Metrics

Standard JVM metrics from `io.prometheus.client.hotspot.DefaultExports`:

- `jvm_memory_bytes_*` - Heap and non-heap memory usage
- `jvm_threads_*` - Thread counts and states
- `jvm_gc_collection_seconds_*` - Garbage collection statistics
- `jvm_classes_loaded` - Loaded class count
- And more...

## State Enumeration Values

Both supervisor and component states use numeric enumerations:

| Value | State |
|-------|-------|
| 0 | disconnected |
| 1 | booted |
| 2 | configured |
| 3 | downloaded |
| 4 | prestarted |
| 5 | active |
| 6 | paused |
| 7 | ending |
| 8 | ended |

## Label Reference

### Standard Labels

All metrics include these labels for multi-tenancy:

- **session** - Session name (e.g., "expid", "hallb")
- **runtype** - Run type name (e.g., "physics", "calibration")

### Component Labels

Component-specific metrics also include:

- **component** - Component name (e.g., "ROC1", "EB1", "TS1")
- **type** - Component type from ACodaType enum (e.g., "ROC", "EB", "TS")

### Buffer Labels

Buffer depth metrics include:

- **buffer** - Direction: "input" or "output"
- **buffer_name** - Full buffer name (e.g., "ROC1:EB1")

## Prometheus Configuration

### Scrape Configuration

Add to your `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'afecs'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:9090']
        labels:
          instance: 'supervisor1'
```

### Multiple Supervisors

If running multiple supervisors, use different ports:

```yaml
scrape_configs:
  - job_name: 'afecs'
    scrape_interval: 15s
    static_configs:
      - targets:
        - 'supervisor1:9090'
        - 'supervisor2:9091'
        - 'supervisor3:9092'
```

### Service Discovery

For dynamic environments, use Prometheus service discovery:

```yaml
scrape_configs:
  - job_name: 'afecs'
    dns_sd_configs:
      - names:
        - 'supervisor.jlab.org'
        type: 'A'
        port: 9090
```

## Example Queries

### PromQL Examples

```promql
# Total event rate across all ROCs in a session
sum(afecs_component_event_rate_hz{session="expid",type="ROC"})

# Average data rate by component type
avg by (type) (afecs_component_data_rate_bytes_per_second{session="expid"})

# Components in error state (not active during active run)
afecs_component_state{session="expid"} != 5

# Components with state=active
afecs_component_state{session="expid"} == 5

# High buffer depth alert (>90%)
afecs_component_buffer_depth_percent{session="expid"} > 90

# Run duration
afecs_run_end_timestamp_seconds - afecs_run_start_timestamp_seconds

# Time remaining until event limit
(afecs_run_event_limit - afecs_supervisor_events_total) /
afecs_supervisor_event_rate_hz

# Event rate by component
sort_desc(afecs_component_event_rate_hz{session="expid",type="ROC"})

# Total data processed in current run (approx, based on longs)
afecs_supervisor_data_bytes_total{session="expid"}

# Average event size across all EBs
avg(afecs_component_event_size_avg_bytes{type="EB"})
```

## Grafana Dashboards

### Suggested Panels

**Run Overview:**
```
Panel: Current Run State
Query: afecs_supervisor_state{session="$session"}
Type: Stat (with state mapping)

Panel: Run Progress
Query: (afecs_supervisor_events_total / afecs_run_event_limit) * 100
Type: Gauge (0-100%)

Panel: Time to Event Limit
Query: (afecs_run_event_limit - afecs_supervisor_events_total) / afecs_supervisor_event_rate_hz
Type: Stat (with unit: seconds)
```

**Event Rates:**
```
Panel: Total Event Rate
Query: sum(afecs_component_event_rate_hz{session="$session",type="ROC"})
Type: Graph

Panel: Event Rate by Component
Query: afecs_component_event_rate_hz{session="$session",type="ROC"}
Type: Graph (legend: {{component}})
```

**Data Rates:**
```
Panel: Data Rate by Type
Query: sum by (type) (afecs_component_data_rate_bytes_per_second{session="$session"})
Type: Stacked graph
```

**Component Status:**
```
Panel: Component States
Query: afecs_component_state{session="$session"}
Type: Table
Transform: Add field from calculation (state name from value)
```

**Buffer Depths:**
```
Panel: Buffer Depth Heatmap
Query: afecs_component_buffer_depth_percent{session="$session"}
Type: Heatmap

Panel: High Buffers Alert
Query: afecs_component_buffer_depth_percent{session="$session"} > 75
Type: Table (only show buffers >75%)
```

### Dashboard Variables

Create template variables in Grafana:

- **session**: `label_values(afecs_supervisor_state, session)`
- **runtype**: `label_values(afecs_supervisor_state{session="$session"}, runtype)`
- **component**: `label_values(afecs_component_state{session="$session"}, component)`

## Alerting Rules

### Prometheus Alert Examples

```yaml
groups:
  - name: afecs
    interval: 30s
    rules:
      - alert: AFECSComponentDisconnected
        expr: afecs_component_state == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Component {{ $labels.component }} disconnected"
          description: "Component {{ $labels.component }} in session {{ $labels.session }} is disconnected"

      - alert: AFECSHighBufferDepth
        expr: afecs_component_buffer_depth_percent > 90
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High buffer depth on {{ $labels.component }}"
          description: "Buffer {{ $labels.buffer_name }} is {{ $value }}% full"

      - alert: AFECSLowEventRate
        expr: afecs_supervisor_event_rate_hz < 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low event rate in {{ $labels.session }}"
          description: "Event rate is {{ $value }} Hz (expected >100 Hz)"

      - alert: AFECSEventLimitApproaching
        expr: (afecs_supervisor_events_total / afecs_run_event_limit) > 0.9
        for: 1m
        labels:
          severity: info
        annotations:
          summary: "Approaching event limit"
          description: "Run {{ $labels.session }} has processed {{ $value | humanizePercentage }} of event limit"
```

## Metric Lifecycle

### Before Configure

Only supervisor-level metrics are available:
- `afecs_supervisor_state` = 1 (booted)
- `afecs_run_number`
- Other supervisor metrics

### After Configure

Component metrics are **lazily registered** as components report:
- `afecs_component_state{component="ROC1"}`
- `afecs_component_state{component="ROC2"}`
- `afecs_component_state{component="EB1"}`
- All per-component metrics

### During Active Run

All metrics are updated:
- Every **100ms** during state transitions (*ing states)
- Every **1000ms** during stable states

### After Reset

Component metrics are cleared:
- Discovered components list is reset
- Component metrics remain in memory but stop updating
- Next configure will re-discover and re-register components

## Update Frequency

| State | Update Interval | Reason |
|-------|----------------|---------|
| *ing (configuring, downloading, etc.) | 100ms | Fast updates during transitions |
| Stable states (booted, configured, active) | 1000ms | Reduced load during steady state |

## Performance Impact

### Metrics Count

Typical deployment:
- ~17 supervisor metrics
- ~15 metrics per component × N components
- ~2-10 buffer metrics per component

Example with 50 components:
- Supervisor: 17 metrics
- Components: 15 × 50 = 750 metrics
- Buffers: 5 × 50 = 250 metrics
- **Total: ~1,017 time series**

### CPU Overhead

- Metric update: <150 µs per iteration
- HTTP scrape: <100ms per scrape
- **Negligible impact on DAQ performance**

### Memory Usage

- ~1KB per time series
- 1,000 time series ≈ 1 MB
- **Minimal memory footprint**

## Troubleshooting

### Exporter Fails to Start

```
WARNING: Failed to start Prometheus exporter on port 9090
  Metrics will not be available. Error: Address already in use
```

**Solution:** Port is in use. Use a different port:
```bash
java -Dprometheus.port=9091 ...
```

### No Metrics Appear

**Check endpoint:**
```bash
curl http://localhost:9090/metrics
```

If connection refused:
- Verify supervisor is running
- Check port number
- Check firewall rules

### Component Metrics Missing

Component metrics only appear **after Configure transition**.

**Before configure:**
```
# Only supervisor metrics
afecs_supervisor_state{session="expid",runtype="physics"} 1
```

**After configure:**
```
# Supervisor + component metrics
afecs_supervisor_state{session="expid",runtype="physics"} 2
afecs_component_state{session="expid",runtype="physics",component="ROC1",type="ROC"} 2
afecs_component_state{session="expid",runtype="physics",component="EB1",type="EB"} 2
```

### Stale Metrics After Reset

Prometheus Java client doesn't support removing metrics. Stale metrics will remain but won't update. They will naturally expire from Prometheus based on your retention settings.

## Advanced Configuration

### Custom Metric Registry

To use a custom registry instead of the default:

```java
// In PrometheusExporter.java constructor
CollectorRegistry registry = new CollectorRegistry();
supervisorState = Gauge.build()
    .name("afecs_supervisor_state")
    .help("...")
    .labelNames("session", "runtype")
    .register(registry);  // Use custom registry

// Start server with custom registry
server = new HTTPServer(port, registry);
```

### Additional JVM Metrics

The exporter already includes standard JVM metrics. To add custom JVM metrics:

```java
import io.prometheus.client.hotspot.*;

// In PrometheusExporter constructor
MemoryPoolsExports.register();
BufferPoolsExports.register();
GarbageCollectorExports.register();
ThreadExports.register();
```

### Metric Filtering

To reduce cardinality, filter metrics before export:

```java
// Only export metrics for specific component types
if (comp.getType().equals("ROC") || comp.getType().equals("EB")) {
    componentEventRateHz.labels(...).set(...);
}
```

## Best Practices

### 1. Session/RunType Naming

Use consistent, lowercase names:
- ✅ Good: `session="hallb", runtype="physics"`
- ❌ Bad: `session="Hall_B", runtype="Physics_Run"`

### 2. Label Cardinality

Avoid high-cardinality labels:
- ✅ Good: Use component type (`type="ROC"`)
- ❌ Bad: Use component IP address as label

### 3. Scrape Interval

Match update frequency:
- Prometheus scrape interval: 15-30 seconds
- AFECS update interval: 1 second (stable state)
- Scraping faster than 1s provides no additional data

### 4. Retention

Configure appropriate retention in Prometheus:
```yaml
# prometheus.yml
storage:
  tsdb:
    retention.time: 30d  # Keep 30 days of metrics
```

### 5. Alerting

Create alerts for critical conditions:
- Component disconnections
- High buffer depths (>90%)
- Low event rates during active runs
- Approaching event/data/time limits

## Integration with Other Tools

### Grafana

1. Add Prometheus as data source
2. Import AFECS dashboard (see Grafana Dashboards section)
3. Create alerts in Grafana

### Alertmanager

Forward Prometheus alerts to Alertmanager:

```yaml
# prometheus.yml
alerting:
  alertmanagers:
    - static_configs:
      - targets: ['localhost:9093']

# alertmanager.yml
route:
  group_by: ['session', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'email-notifications'

receivers:
  - name: 'email-notifications'
    email_configs:
      - to: 'daq-ops@jlab.org'
        from: 'prometheus@jlab.org'
```

### PushGateway

Not recommended for AFECS. The embedded exporter with pull-based scraping is more appropriate for long-running supervisor processes.

## References

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Prometheus Java Client](https://github.com/prometheus/client_java)
- [Grafana Documentation](https://grafana.com/docs/)
- [PromQL Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Alerting Rules](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)

## Support

For issues or questions:
- AFECS: gurjyan@jlab.org
- GitHub Issues: https://github.com/JeffersonLab/afecs/issues
