# AFECS Prometheus Examples

This directory contains example configurations for monitoring AFECS with Prometheus and Grafana.

## Files

- **prometheus.yml** - Example Prometheus configuration
- **afecs_alerts.yml** - Alert rules for AFECS metrics
- **docker-compose.yml** - Quick start with Docker Compose

## Quick Start with Docker

### 1. Start Prometheus and Grafana

```bash
cd examples/prometheus
docker-compose up -d
```

This will start:
- Prometheus on http://localhost:9090
- Grafana on http://localhost:3000 (admin/admin)

### 2. Start AFECS Platform

```bash
# In AFECS root directory
bin/platform
```

The Prometheus exporter will start automatically on port 9090.

### 3. Access Prometheus

Open http://localhost:9090 and verify:
- Status → Targets shows "afecs-supervisor" as UP
- Graph tab can query `afecs_supervisor_state`

### 4. Access Grafana

Open http://localhost:3000:
- Login: admin / admin
- Add Prometheus data source: http://prometheus:9090
- Create dashboard using AFECS metrics

## Manual Setup

### Install Prometheus

**macOS:**
```bash
brew install prometheus
```

**Linux:**
```bash
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
tar xvfz prometheus-*.tar.gz
cd prometheus-*
```

**Windows:**
Download from https://prometheus.io/download/

### Configure Prometheus

1. Copy configuration files:
```bash
cp examples/prometheus/prometheus.yml /etc/prometheus/
cp examples/prometheus/afecs_alerts.yml /etc/prometheus/
```

2. Edit `prometheus.yml` to match your environment:
   - Update `targets` with your supervisor hostnames/ports
   - Configure alerting if using Alertmanager

3. Start Prometheus:
```bash
prometheus --config.file=/etc/prometheus/prometheus.yml
```

### Install Grafana

**macOS:**
```bash
brew install grafana
brew services start grafana
```

**Linux:**
```bash
sudo apt-get install -y grafana
sudo systemctl start grafana-server
```

**Windows:**
Download from https://grafana.com/grafana/download

### Configure Grafana

1. Open http://localhost:3000
2. Login (default: admin/admin)
3. Add Prometheus data source:
   - Configuration → Data Sources → Add data source
   - Select Prometheus
   - URL: http://localhost:9090
   - Save & Test

4. Create dashboard:
   - Create → Dashboard → Add new panel
   - Use PromQL queries from PROMETHEUS.md

## Example Queries

### Run Overview

```promql
# Current supervisor state
afecs_supervisor_state{session="expid"}

# Current run number
afecs_run_number{session="expid"}

# Events processed
afecs_supervisor_events_total{session="expid"}

# Event rate
afecs_supervisor_event_rate_hz{session="expid"}
```

### Component Monitoring

```promql
# All component states
afecs_component_state{session="expid"}

# ROC event rates
afecs_component_event_rate_hz{session="expid",type="ROC"}

# EB build times
afecs_component_build_time_mean_microseconds{type="EB"}

# High buffers
afecs_component_buffer_depth_percent > 80
```

### Alerting

```promql
# Disconnected components
afecs_component_state == 0

# Low event rate during active run
afecs_supervisor_state == 5 and afecs_supervisor_event_rate_hz < 100

# High buffer depth
afecs_component_buffer_depth_percent > 90
```

## Alert Testing

To test alerts without waiting for real conditions:

1. Start Prometheus with alert rules
2. Verify rules are loaded: http://localhost:9090/rules
3. Simulate conditions:
   - Stop a component to trigger AFECSComponentDisconnected
   - During active run, check for low rates
4. View active alerts: http://localhost:9090/alerts

## Grafana Dashboard Panels

### Recommended Panels

**Row 1: Run Status**
- Current State (Stat)
- Run Number (Stat)
- Event Count (Stat)
- Run Duration (Stat)

**Row 2: Rates**
- Event Rate (Graph)
- Data Rate (Graph)
- Event Rate by Component (Graph)

**Row 3: Components**
- Component States (Table)
- Component Count by State (Pie chart)

**Row 4: Performance**
- Live Time by Component (Bar gauge)
- Build Times (Graph for EBs)
- Buffer Depths (Heatmap)

**Row 5: JVM**
- Heap Usage (Graph)
- GC Time (Graph)

## Troubleshooting

### Prometheus can't scrape AFECS

Check:
1. Supervisor is running: `ps aux | grep SupervisorAgent`
2. Port is accessible: `curl http://localhost:9090/metrics`
3. Firewall allows connection
4. Prometheus targets page shows status

### No metrics appear

1. Verify endpoint returns metrics:
```bash
curl http://localhost:9090/metrics | grep afecs_
```

2. Check Prometheus logs:
```bash
# Docker
docker-compose logs prometheus

# System service
journalctl -u prometheus
```

3. Verify scrape config in Prometheus UI:
   - Status → Configuration
   - Status → Targets

### Component metrics missing

Component metrics only appear **after Configure**:
- Before: Only `afecs_supervisor_*` metrics
- After: Also `afecs_component_*` metrics

### Alerts not firing

1. Check alert rules are loaded: http://localhost:9090/rules
2. Verify expressions in Prometheus Graph tab
3. Check alert state: http://localhost:9090/alerts
4. Review Alertmanager config if using

## Production Deployment

### High Availability

For production, consider:

1. **Multiple Prometheus instances** with same config
2. **Prometheus Operator** for Kubernetes deployments
3. **Thanos** for long-term storage and global queries
4. **Alertmanager cluster** for reliable alerting

### Retention

Configure retention based on disk space:

```yaml
# prometheus.yml
storage:
  tsdb:
    retention.time: 30d      # Keep 30 days
    retention.size: 50GB     # Or 50GB max
```

### Remote Storage

For long-term storage:

```yaml
# prometheus.yml
remote_write:
  - url: 'http://cortex:9009/api/prom/push'
  # or
  - url: 'http://thanos-receive:19291/api/v1/receive'
```

### Security

1. **Enable authentication** on Prometheus/Grafana
2. **Use TLS** for production endpoints
3. **Restrict network access** to metrics ports
4. **Use read-only access** for Grafana data source

## Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [AFECS Prometheus Guide](../../PROMETHEUS.md)
- [PromQL Cheat Sheet](https://promlabs.com/promql-cheat-sheet/)
