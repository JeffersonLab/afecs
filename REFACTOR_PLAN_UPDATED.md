# AFECS Refactoring Plan - UPDATED (Step 2 Revised)

**Generated:** 2026-03-04
**Updated:** 2026-03-04 (Based on user feedback)
**Repository:** /Users/gurjyan/Documents/Devel/coda/afecs
**Status:** READY FOR EXECUTION

---

## User Feedback Incorporated

**Q1: Plugin stubs (AEpics, ARc, ASnmp)** → ✅ **DELETE**
**Q2: Execute all 12 commits?** → ✅ **YES, proceed with all**
**Q3: InfluxDB usage** → ✅ **REMOVE completely (not just fix bug)**
**Q4: UI testing** → ✅ **User will test on different node**
**Q5: Test environment** → ✅ **Remote node attached to real DAQ**

**Additional Request:** ✅ **Gradle build system created**

---

## Changes from Original Plan

| Original Commit | Change | New Commit |
|-----------------|--------|------------|
| - | **NEW** | Commit 0: Add Gradle build system |
| Commit 1 | **EXPANDED** | Now includes plugin stub deletion (3 more files) |
| Commit 8 | **REPLACED** | Remove InfluxDB support entirely (not just fix bug) |
| All others | Unchanged | Same as original plan |

---

## Updated Commit Sequence (13 Commits Total)

### COMMIT 0: Add Gradle build system ✅ COMPLETED

**Objective:** Modernize build process with Gradle

**Risk Level:** 🟢 **LOW**
**Time Estimate:** COMPLETED
**LOC Impact:** +300 lines (new files)

---

#### Files Created ✅

```
CREATE build.gradle (250 lines)
CREATE settings.gradle
CREATE gradle.properties
CREATE .gitignore
CREATE gradlew (wrapper script)
CREATE gradlew.bat (Windows wrapper)
CREATE gradle/wrapper/gradle-wrapper.jar
CREATE gradle/wrapper/gradle-wrapper.properties
```

---

#### Build Features

**Dependencies:**
- Automatic dependency resolution
- Local JARs from `lib/` directory
- cMsg from parent directory (auto-detected)
- JUnit 4.13.2 for tests

**Tasks:**
```bash
./gradlew build             # Compile and test
./gradlew fatJar           # Create uber JAR with all dependencies
./gradlew runPlatform      # Run APlatform
./gradlew runContainer     # Run AContainer
./gradlew runGui           # Run RcGui
./gradlew compileCool      # Run COOL compiler
./gradlew test             # Run tests
./gradlew checkDeps        # Verify dependencies
./gradlew buildInfo        # Show build information
```

**Key Features:**
- Java 8 compatibility
- Parallel builds enabled
- Build caching
- Fat JAR creation for deployment
- Wrapper included (no local Gradle installation needed)

---

#### Verification ✅

```bash
# Already tested:
./gradlew buildInfo
# Output: Build info displayed correctly

# Next steps:
./gradlew checkDeps        # Check if all dependencies available
./gradlew build            # Compile entire project
```

---

### COMMIT 1: Remove dead code, deprecated classes, and plugin stubs

**Objective:** Delete 10 unused files including plugin stubs

**Risk Level:** 🟢 **LOW**
**Time Estimate:** 30 minutes
**LOC Impact:** -300 lines (10 files deleted)

---

#### Files Deleted (10 total)

**Dead Code:**
```
DELETE src/org/jlab/coda/afecs/system/util/FETConnection.java
DELETE src/org/jlab/coda/afecs/ui/rcgui/util/ATableData.java
DELETE src/org/jlab/coda/afecs/ui/rcgui/util/TreeHashMap.java
DELETE src/org/jlab/coda/afecs/ui/rcgui/util/AListDDialog.java (@Deprecated duplicate)
```

**Deprecated Package (4 files):**
```
DELETE src/org/jlab/coda/afecs/usr/rcapi/deprecated/AskPlatformApp.java
DELETE src/org/jlab/coda/afecs/usr/rcapi/deprecated/AskPlatform.java
DELETE src/org/jlab/coda/afecs/usr/rcapi/deprecated/RcCommand.java
RMDIR  src/org/jlab/coda/afecs/usr/rcapi/deprecated/
```

**Plugin Stubs (NEW - per Q1):**
```
DELETE src/org/jlab/coda/afecs/plugin/AEpics.java
DELETE src/org/jlab/coda/afecs/plugin/ARc.java
DELETE src/org/jlab/coda/afecs/plugin/ASnmp.java
```

---

#### Special Handling: Plugin Interface

**File:** `src/org/jlab/coda/afecs/plugin/IAClientCommunication.java`

**Decision:** ⚠️ **KEEP** interface but remove implementations

**Rationale:**
- Interface defines plugin contract
- May be referenced by AParent (check line ~321)
- Removing implementations won't break compilation if no instantiation

**Verification Required:**
```bash
# Check if plugin classes are instantiated
grep -r "new AEpics\|new ARc\|new ASnmp" src/
grep -r "Class.forName.*AEpics\|Class.forName.*ARc\|Class.forName.*ASnmp" src/

# If found, update instantiation code to handle missing plugins gracefully
```

---

#### Updated Verification Steps

**Pre-commit checks:**
```bash
# 1. Confirm no Java imports reference deleted files
grep -r "import.*FETConnection" src/
grep -r "import.*ATableData" src/
grep -r "import.*TreeHashMap" src/
grep -r "import.*AskPlatformApp" src/
grep -r "import.*AListDDialog" src/
grep -r "import.*AEpics" src/
grep -r "import.*ARc" src/
grep -r "import.*ASnmp" src/

# Expected output: No matches (or only self-references)
```

**Post-commit checks:**
```bash
# 2. Build with Gradle
./gradlew clean build

# Expected: BUILD SUCCESSFUL

# 3. Verify deleted files are gone
ls src/org/jlab/coda/afecs/system/util/FETConnection.java 2>/dev/null
ls src/org/jlab/coda/afecs/plugin/AEpics.java 2>/dev/null

# Expected: No such file or directory
```

**Testing:**
- ✅ Gradle build test (must pass)
- ✅ Platform startup test
- ✅ Container startup test
- ⚠️ Verify no plugin loading errors in logs

---

#### Potential Issue: Plugin Loading in AParent

**Location:** `src/org/jlab/coda/afecs/agent/AParent.java` (around line 321)

**Current Code (approximate):**
```java
Class c = loader.loadClass(myPluginPath + "." + myPluginClass);
myPlugin = (IAClientCommunication) c.newInstance();
```

**If this tries to load ARc/AEpics/ASnmp:**
- Loading will fail with ClassNotFoundException
- Need to add try-catch with graceful fallback or skip plugin loading

**Mitigation:**
```java
try {
    Class c = loader.loadClass(myPluginPath + "." + myPluginClass);
    myPlugin = (IAClientCommunication) c.newInstance();
} catch (ClassNotFoundException e) {
    System.out.println("Warning: Plugin " + myPluginClass + " not found, skipping");
    myPlugin = null; // or use a NoOp plugin
}
```

**Action:** Check COOL configurations to see which plugins are specified. If none use AEpics/ARc/ASnmp, no code changes needed.

---

### COMMITS 2-7: Unchanged from Original Plan

See original `REFACTOR_PLAN_STEP2.md` for details:
- Commit 2: Add reportEvent() utility to ABase
- Commit 3: Replace logging pairs in CodaRCAgent
- Commit 4: Replace logging pairs in remaining files
- Commit 5: Add SubscriptionManager utility
- Commit 6: Use SubscriptionManager in AParent
- Commit 7: Use SubscriptionManager in CodaRCAgent

---

### COMMIT 8: Remove InfluxDB support entirely (REPLACED)

**Original:** Fix InfluxInjector blocking constructor
**New:** Remove InfluxDB integration completely

**Objective:** Remove unused InfluxDB monitoring feature

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 1.5 hours
**LOC Impact:** -300 lines (2 files deleted, 1 edited)

---

#### Files Deleted

```
DELETE src/org/jlab/coda/afecs/influx/InfluxInjector.java
DELETE src/org/jlab/coda/afecs/influx/JinFluxDriver.java
RMDIR  src/org/jlab/coda/afecs/influx/
```

---

#### Files Modified

**File 1:** `src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java`

**Changes:**

**1. Remove import:**
```java
// DELETE this import:
import org.jlab.coda.afecs.influx.InfluxInjector;
import org.jlab.coda.jinflux.JinFluxException;
```

**2. Remove field (around line 100):**
```java
// DELETE this field:
private InfluxInjector influxInjector;
```

**3. Remove initialization code:**

Find and DELETE block similar to:
```java
// DELETE this entire block:
if (owner.myPlatform.influxDb) {
    try {
        influxInjector = new InfluxInjector(this, dbNode, dbName);
        // May be in constructor or other method
    } catch (JinFluxException e) {
        e.printStackTrace();
    }
}
```

**4. Remove cleanup code (if exists):**
```java
// DELETE:
if (influxInjector != null && influxInjector.isRunning()) {
    influxInjector.stop();
}
```

**5. Search for any other InfluxInjector references:**
```bash
grep -n "InfluxInjector\|influxInjector\|influxDb" \
  src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java
```

---

**File 2:** `src/org/jlab/coda/afecs/platform/APlatform.java`

**Check for InfluxDB configuration:**
```bash
grep -n "influxDb" src/org/jlab/coda/afecs/platform/APlatform.java
```

**If found:**
- Remove field: `public boolean influxDb;`
- Remove any configuration parsing for InfluxDB

---

**File 3:** `build.gradle`

**Remove JinFlux dependency:**
```gradle
// DELETE this line:
implementation files('lib/JinFlux.jar')
```

---

**File 4 (Optional):** `lib/JinFlux.jar`

**Action:** Can delete JAR file (or leave it, won't be used)

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Find all InfluxDB references
grep -r "Influx\|influx" src/ --include="*.java" | grep -v ".class"

# Document all occurrences for cleanup
```

**Post-commit checks:**
```bash
# 2. Verify no Influx references remain
grep -r "Influx\|JinFlux" src/ --include="*.java"

# Expected: 0 results

# 3. Build with Gradle
./gradlew clean build

# Expected: BUILD SUCCESSFUL (no missing class errors)

# 4. Check for missing imports
grep -r "import.*influx" src/

# Expected: 0 results
```

**Testing:**

**Critical Tests:**

1. **Supervisor Startup Test**
   ```bash
   ./gradlew runPlatform
   # In another terminal:
   ./gradlew runContainer

   # Expected: Container starts immediately (no hanging)
   # Verify: Supervisor agent created without errors
   # Verify: No InfluxDB-related errors in logs
   ```

2. **Full Run Control Test**
   - Configure supervisor with components
   - Transition through all states
   - **Expected:** No InfluxDB-related errors
   - **Verify:** Normal operation without metrics injection

3. **Configuration Parsing Test**
   - Load COOL configuration
   - **Expected:** No errors about missing InfluxDB settings
   - **Verify:** Platform configuration parses correctly

**Acceptance Criteria:**
- ✅ No InfluxDB code remains
- ✅ Supervisor starts without errors
- ✅ No missing class exceptions
- ✅ No configuration parsing errors
- ✅ Build successful

---

#### Migration Notes

**For Users Who Were Using InfluxDB:**

If InfluxDB monitoring was actively used:
1. Alternative: Export metrics via daLog messages
2. Alternative: Use external monitoring tools (Prometheus, Grafana)
3. Alternative: Create custom monitoring agent

**Data Loss Warning:**
- Real-time web monitoring via InfluxDB will no longer work
- Historical metrics will remain in existing InfluxDB database
- Can still access old data via InfluxDB queries

---

#### Rollback Strategy

```bash
# If InfluxDB removal causes issues:
git revert HEAD
git push origin main

# Restores InfluxDB support (but bug still exists)
```

**Rollback Risk:** LOW - Feature was optional, not core functionality

---

### COMMITS 9-12: Unchanged from Original Plan

See original `REFACTOR_PLAN_STEP2.md` for details:
- Commit 9: Extract BaseDialogComponent
- Commit 10: Refactor remaining dialogs to use base
- Commit 11: Extract SupervisorStateManager from SupervisorAgent
- Commit 12: Add formatting config and apply

---

## Updated Commit Summary Table

| # | Commit | Risk | Time | LOC | Status |
|---|--------|------|------|-----|--------|
| 0 | Add Gradle build system | 🟢 LOW | - | +300 | ✅ DONE |
| 1 | Remove dead code + plugin stubs | 🟢 LOW | 30m | -300 | 📋 READY |
| 2 | Add reportEvent() utility | 🟢 LOW | 15m | +15 | 📋 READY |
| 3 | Replace logging in CodaRCAgent | 🟡 MED | 1h | -52 | 📋 READY |
| 4 | Replace logging in remaining files | 🟡 MED | 45m | -40 | 📋 READY |
| 5 | Add SubscriptionManager | 🟢 LOW | 30m | +80 | 📋 READY |
| 6 | Use SubscriptionManager in AParent | 🟡 MED | 45m | -30 | 📋 READY |
| 7 | Use SubscriptionManager in CodaRCAgent | 🟡 MED | 1h | -40 | 📋 READY |
| 8 | Remove InfluxDB support | 🟡 MED | 1.5h | -300 | 📋 READY |
| 9 | Extract BaseDialogComponent | 🟡 MED | 1h | +100 | 📋 READY |
| 10 | Refactor remaining dialogs | 🟡 MED | 2h | -200 | 📋 READY |
| 11 | Extract SupervisorStateManager | 🔴 HIGH | 3h | -300 | 📋 READY |
| 12 | Add formatting config | 🟢 LOW | 30m | +200 | 📋 READY |
| **TOTAL** | **13 commits** | | **~12h** | **-767** | |

---

## Execution Plan

### Phase 1: Foundation & Quick Wins (Commits 0-4)
**Estimated Time:** 2.5 hours
**Risk:** LOW to MEDIUM

**Workflow:**
1. ✅ Commit 0: Already completed (Gradle)
2. Execute Commit 1: Delete unused files
3. Execute Commit 2: Add reportEvent()
4. Execute Commit 3: Refactor CodaRCAgent logging
5. Execute Commit 4: Refactor remaining logging
6. **Checkpoint:** Test on remote DAQ node

---

### Phase 2: Infrastructure Improvements (Commits 5-8)
**Estimated Time:** 4 hours
**Risk:** MEDIUM

**Workflow:**
1. Execute Commit 5: Add SubscriptionManager
2. Execute Commit 6: Refactor AParent
3. Execute Commit 7: Refactor CodaRCAgent
4. **Checkpoint:** Test agent communication on DAQ
5. Execute Commit 8: Remove InfluxDB
6. **Checkpoint:** Full integration test on DAQ

---

### Phase 3: Structural Refactoring (Commits 9-12)
**Estimated Time:** 6.5 hours
**Risk:** MEDIUM to HIGH

**Workflow:**
1. Execute Commit 9: BaseDialogComponent
2. **User testing:** UI dialog functionality (different node)
3. Execute Commit 10: Remaining dialogs
4. **User testing:** Full UI regression test
5. Execute Commit 11: SupervisorStateManager
6. **Critical testing:** State machine on real DAQ
7. Execute Commit 12: Formatting
8. **Final testing:** Full system test on DAQ

---

## Testing Strategy for Real DAQ Environment

### Pre-Deployment Checklist

```bash
# 1. Build verification
./gradlew clean build test

# 2. Create deployment JAR
./gradlew fatJar

# 3. Copy to remote node
scp build/libs/afecs-all-4.0.0-SNAPSHOT-fat.jar user@daq-node:/path/to/afecs/

# 4. Backup current version on DAQ node
ssh user@daq-node
cd /path/to/afecs/
cp -r current_version backup_$(date +%Y%m%d_%H%M%S)
```

---

### Testing on Real DAQ (Per Commit)

**After Each Commit:**

1. **Build:**
   ```bash
   ./gradlew clean build
   ```

2. **Deploy to DAQ:**
   ```bash
   ./gradlew fatJar
   scp build/libs/afecs-all-*.jar user@daq-node:/path/
   ```

3. **Test on DAQ:**
   - Start platform
   - Start container
   - Load COOL configuration
   - Connect real components
   - Run through state machine
   - Monitor logs for errors

4. **Rollback if needed:**
   ```bash
   # On DAQ node:
   ssh user@daq-node
   cd /path/to/afecs/
   rm current_version/*
   cp backup_YYYYMMDD_HHMMSS/* current_version/
   ```

---

### Critical Test Scenarios (Real DAQ)

**Test 1: Component Connection**
- Physical ROC connects to platform
- Agent represents ROC correctly
- Status messages flow correctly

**Test 2: State Machine Cycle**
- Configure → Download → Prestart → Go → End
- All components transition correctly
- No timeout errors

**Test 3: Data Acquisition Run**
- Full data acquisition run
- Event rate monitoring
- Data file creation
- Clean end of run

**Test 4: Error Recovery**
- Component disconnects during run
- Agent detects and reports
- Supervisor handles gracefully
- Can recover and reset

**Test 5: UI Testing (User on different node)**
- RcGui connects to platform
- Displays component states correctly
- Dialog boxes work (Commits 9-10)
- State transition commands work

---

## Risk Mitigation

### High-Risk Commits (Require Extra Care)

**Commit 7: SubscriptionManager in CodaRCAgent**
- **Risk:** Core component communication
- **Mitigation:** Test with real component before full DAQ
- **Rollback:** Single commit revert

**Commit 8: Remove InfluxDB**
- **Risk:** Configuration parsing errors
- **Mitigation:** Verify COOL configs don't reference InfluxDB
- **Rollback:** Single commit revert

**Commit 11: SupervisorStateManager**
- **Risk:** State machine behavior changes
- **Mitigation:** Extensive state transition testing
- **Rollback:** Single commit revert, test thoroughly

---

### Emergency Rollback Procedure

**If Critical Issue Found:**

1. **Immediate:**
   ```bash
   # On development machine:
   git revert HEAD
   git push origin main

   # On DAQ node:
   cd /path/to/afecs/backup_*/
   ./restore.sh  # or manual copy
   ```

2. **Identify Issue:**
   - Check logs
   - Identify failing component
   - Document error for fix

3. **Fix and Retry:**
   - Create fix in new branch
   - Test locally
   - Test on development DAQ
   - Deploy to production DAQ

---

## Success Criteria

**After All 13 Commits:**

✅ **Build:**
- `./gradlew build` succeeds with 0 errors
- `./gradlew test` passes all tests
- Fat JAR creates successfully

✅ **Functionality:**
- Platform starts without errors
- Container spawns agents correctly
- Components connect and communicate
- State machine works correctly
- UI displays correctly
- No regressions in DAQ operation

✅ **Code Quality:**
- ~767 LOC reduction
- No code duplication in logging
- No code duplication in subscriptions
- InfluxDB removed cleanly
- Dialog code consolidated

✅ **Performance:**
- No performance degradation
- No memory leaks
- Same or better startup time

---

## Final Deliverables

1. **Updated Codebase**
   - All 13 commits merged to main
   - Gradle build system working
   - All tests passing

2. **Build Artifacts**
   - `afecs-4.0.0-SNAPSHOT.jar`
   - `afecs-all-4.0.0-SNAPSHOT-fat.jar`

3. **Documentation**
   - Updated README with Gradle instructions
   - Build guide for new developers
   - Migration notes from previous version

4. **Test Reports**
   - DAQ test results
   - UI test results (from user)
   - Performance comparison

---

## Next Steps

**Ready to proceed?**

1. Execute Commit 1 (delete unused files + plugins)
2. Execute Commit 2-4 (logging refactoring)
3. Test on DAQ after Phase 1
4. Continue with Commits 5-8 if Phase 1 successful
5. User tests UI (Commits 9-10)
6. Execute Commit 11 (highest risk)
7. Final formatting (Commit 12)

**Estimated Total Time:** 12 hours of execution + testing

**Estimated Calendar Time:** 2-3 days (with DAQ testing)

---

*End of Updated Refactoring Plan*

**Status:** READY FOR EXECUTION ✅
