# AFECS Refactoring Plan - Step 2 (Detailed Commit Plan)

**Generated:** 2026-03-04
**Repository:** /Users/gurjyan/Documents/Devel/coda/afecs
**Status:** AWAITING APPROVAL

---

## Overview

This document provides a detailed, commit-by-commit refactoring plan for the AFECS codebase. Each commit is designed to be:

✅ **Independently reviewable** - Clear scope and purpose
✅ **Safely reversible** - Can be rolled back without side effects
✅ **Build-passing** - Compiles and runs after each commit
✅ **Test-verified** - Includes verification steps

**Total Commits:** 12
**Estimated Time:** 3-5 days (with testing)
**LOC Impact:** ~-750 lines

---

## Public API Safety Commitment

⚠️ **CRITICAL CONSTRAINT:** This refactoring will NOT change any public APIs or externally observable behavior.

### Public API Surface (Unchanged)

**Java API:**
- `org.jlab.coda.afecs.usr.rcapi.RcApi` - All public methods preserved

**C API (JNI):**
- `org.jlab.coda.afecs.usr.rcapi.rcapi_c.*` - No changes

**cMsg Message Contracts:**
- All message subjects, types, and payload formats preserved
- All subscriptions maintain identical patterns

**Entry Points:**
- All `main()` methods unchanged
- All command-line argument parsing preserved

### Internal vs. External Changes

| Component | Type | Preserved? | Changes |
|-----------|------|------------|---------|
| RcApi public methods | External | ✅ YES | None |
| cMsg message formats | External | ✅ YES | None |
| ABase public methods | Internal | ⚠️ MODIFIED | Add new methods, keep existing |
| AParent protected methods | Internal | ⚠️ MODIFIED | Add new fields, keep existing |
| SupervisorAgent | Internal | ⚠️ MODIFIED | Extract managers, delegate calls |
| UI classes | Internal | ⚠️ MODIFIED | Refactor dialogs |

**Strategy:** All new functionality is additive. Existing methods delegate to new utilities. No deletions of public/protected methods until deprecated for 1+ release.

---

## Phase 1: Quick Wins (Commits 1-4)

### COMMIT 1: Remove dead code and deprecated classes

**Objective:** Delete unused files to reduce maintenance burden

**Risk Level:** 🟢 **LOW**
**Time Estimate:** 30 minutes
**LOC Impact:** -200 lines (7 files deleted)

---

#### Files Deleted (7 total)

```
DELETE src/org/jlab/coda/afecs/system/util/FETConnection.java
DELETE src/org/jlab/coda/afecs/ui/rcgui/util/ATableData.java
DELETE src/org/jlab/coda/afecs/ui/rcgui/util/TreeHashMap.java
DELETE src/org/jlab/coda/afecs/usr/rcapi/deprecated/AskPlatformApp.java
DELETE src/org/jlab/coda/afecs/usr/rcapi/deprecated/AskPlatform.java
DELETE src/org/jlab/coda/afecs/usr/rcapi/deprecated/RcCommand.java
DELETE src/org/jlab/coda/afecs/ui/rcgui/util/AListDDialog.java (duplicate, @Deprecated)
```

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Confirm no Java imports reference these files
grep -r "import.*FETConnection" src/
grep -r "import.*ATableData" src/
grep -r "import.*TreeHashMap" src/
grep -r "import.*AskPlatformApp" src/
grep -r "import.*AskPlatform" src/
grep -r "import.*RcCommand" src/
grep -r "import.*AListDDialog" src/

# Expected output: No matches found (or only in deleted files themselves)
```

**Post-commit checks:**
```bash
# 2. Ensure project compiles
cd /Users/gurjyan/Documents/Devel/coda/afecs
# Manual compilation (no build system)
export CLASSPATH="lib/*"
javac -d build/classes -sourcepath src $(find src -name "*.java" -not -path "*/test/*")

# Expected output: Compilation succeeds with 0 errors

# 3. Verify deleted files are gone
ls src/org/jlab/coda/afecs/system/util/FETConnection.java 2>/dev/null
# Expected output: No such file or directory

# 4. Check git status
git status
# Expected: 7 files deleted
```

**Testing:**
- ✅ Compilation test (must pass)
- ✅ Platform startup test: `bin/coda/platform`
- ✅ Container startup test: `bin/coda/container`
- ⚠️ No runtime tests needed (files were unused)

---

#### Rollback Strategy

```bash
# If issues are found:
git revert HEAD
git push origin main
```

**Rollback Risk:** NONE - Files were completely unused

---

#### API Safety Analysis

✅ **No public API changes** - All deleted classes were internal and unused

---

### COMMIT 2: Add reportEvent() utility to ABase

**Objective:** Create logging utility without changing existing code

**Risk Level:** 🟢 **LOW**
**Time Estimate:** 15 minutes
**LOC Impact:** +15 lines

---

#### Files Modified

```
EDIT src/org/jlab/coda/afecs/system/ABase.java
```

---

#### Detailed Changes

**File:** `src/org/jlab/coda/afecs/system/ABase.java`

**Insert after line 1142 (end of class, before closing brace):**

```java
    /**
     * Reports event to both alarm system and dalog.
     * This is a convenience method that calls both reportAlarmMsg() and dalogMsg()
     * with consistent parameters, reducing code duplication.
     *
     * @param message    Event message text
     * @param severityId Severity level identifier (0-15, where 9+ is error)
     * @param severity   Severity string (e.g., AConstants.INFO, AConstants.WARN, AConstants.ERROR)
     */
    protected void reportEvent(String message, int severityId, String severity) {
        reportAlarmMsg(mySession + "/" + myRunType, myName, severityId, severity, message);
        dalogMsg(myName, severityId, severity, message);
    }
}
```

**Why protected?**
- Accessible to all subclasses (AParent, CodaRCAgent, SupervisorAgent, etc.)
- Not public API (internal utility)
- Consistent with other ABase protected methods

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Verify ABase currently compiles
javac -cp "lib/*" -d build/classes src/org/jlab/coda/afecs/system/ABase.java

# Expected: Compiles successfully
```

**Post-commit checks:**
```bash
# 2. Verify new method compiles
javac -cp "lib/*" -d build/classes src/org/jlab/coda/afecs/system/ABase.java

# Expected: Compiles successfully

# 3. Verify method signature is correct
grep -A 5 "protected void reportEvent" src/org/jlab/coda/afecs/system/ABase.java

# Expected: Shows the new method with JavaDoc

# 4. Compile all subclasses to ensure compatibility
javac -cp "lib/*:build/classes" -d build/classes \
  src/org/jlab/coda/afecs/agent/AParent.java \
  src/org/jlab/coda/afecs/codarc/CodaRCAgent.java \
  src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java

# Expected: All compile successfully
```

**Testing:**
- ✅ Compilation test (must pass)
- ✅ Platform startup test
- ⚠️ Method not used yet (additive change)

---

#### Rollback Strategy

```bash
# If method causes issues:
git revert HEAD
```

**Rollback Risk:** MINIMAL - Additive change, no existing code modified

---

#### API Safety Analysis

✅ **No public API changes** - New protected method in internal base class
✅ **No behavior changes** - Method not called yet
✅ **Backward compatible** - All existing code unchanged

---

### COMMIT 3: Replace logging pairs in CodaRCAgent

**Objective:** Reduce duplication by using new reportEvent() utility

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 1 hour
**LOC Impact:** -52 lines (26 pairs → 26 calls)

---

#### Files Modified

```
EDIT src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
```

---

#### Detailed Changes

**File:** `src/org/jlab/coda/afecs/codarc/CodaRCAgent.java`

**Total Replacements:** 26 occurrences

**Example 1 - Lines 278-281:**

```java
// BEFORE:
if (me.getState().equals(AConstants.connected)) {
    reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
            myName, 5,
            AConstants.WARN,
            " Client is restarted.");
}

// AFTER:
if (me.getState().equals(AConstants.connected)) {
    reportEvent("Client is restarted.", 5, AConstants.WARN);
}
```

**Example 2 - Lines 352-360:**

```java
// BEFORE:
reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
        me.getName(),
        11,
        AConstants.ERROR,
        e.getMessage());
dalogMsg(me.getName(),
        11,
        AConstants.ERROR,
        e.getMessage());

// AFTER:
reportEvent(e.getMessage(), 11, AConstants.ERROR);
```

**Example 3 - Lines 859-864:**

```java
// BEFORE:
reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
        me.getName(),
        11,
        AConstants.ERROR,
        "Cannot connect to the physical component using udl = " + udl);

// AFTER:
reportEvent("Cannot connect to the physical component using udl = " + udl,
        11, AConstants.ERROR);
```

**Example 4 - Lines 885-890:**

```java
// BEFORE:
reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
        me.getName(),
        11,
        AConstants.ERROR,
        "Undefined client udl = " + udl);

// AFTER:
reportEvent("Undefined client udl = " + udl, 11, AConstants.ERROR);
```

**Example 5 - Lines 898-902:**

```java
// BEFORE:
reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
        me.getName(),
        11,
        AConstants.ERROR,
        "Undefined host and port for the client.");

// AFTER:
reportEvent("Undefined host and port for the client.", 11, AConstants.ERROR);
```

**Example 6 - Lines 1175-1193 (ResponseMsgCB callback):**

```java
// BEFORE:
reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
        myName,
        7,
        AConstants.WARN,
        "Conflict with the clients CodaClass. client-codaClass = " +
                txt + " vs. " + myName + "-codaClass = " + me.getType());
dalogMsg(myName,
        7,
        AConstants.WARN,
        "Conflict with the clients CodaClass. client-codaClass = " +
                txt + " vs. " + myName + "-codaClass = " + me.getType());

// AFTER:
reportEvent("Conflict with the clients CodaClass. client-codaClass = " +
        txt + " vs. " + myName + "-codaClass = " + me.getType(),
        7, AConstants.WARN);
```

**Full List of Line Numbers to Replace:**

```
Line 278-281   - Client restarted warning
Line 352-360   - Setup exception error
Line 859-864   - Connection failed error
Line 885-890   - Undefined UDL error
Line 898-902   - Undefined host/port error
Line 1137-1140 - Client message alarm (severity >= 9)
Line 1175-1193 - CodaClass conflict
Line 1198-1214 - Session conflict
Line 1219-1237 - Run number conflict
Line 1242-1260 - Run type conflict
Line 1266-1283 - Config ID conflict
(Plus 15 more similar patterns throughout the file)
```

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Count existing reportAlarmMsg + dalogMsg pairs
grep -c "reportAlarmMsg" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
# Expected: ~30

grep -c "dalogMsg" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
# Expected: ~30 (roughly equal)
```

**Post-commit checks:**
```bash
# 2. Count reportEvent calls
grep -c "reportEvent" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
# Expected: ~26

# 3. Verify no orphaned pairs remain (should be 0 or very few for edge cases)
# Pattern: reportAlarmMsg followed by dalogMsg within 10 lines
grep -A 10 "reportAlarmMsg" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java | grep -c "dalogMsg"
# Expected: 0 (all pairs converted)

# 4. Compile
javac -cp "lib/*:build/classes" -d build/classes \
  src/org/jlab/coda/afecs/codarc/CodaRCAgent.java

# Expected: Compiles successfully
```

**Testing:**

**Required Tests:**
1. **Platform + Container Startup**
   ```bash
   # Start platform
   bin/coda/platform test_expid
   # Expected: Platform starts without errors

   # Start container
   bin/coda/container test_expid
   # Expected: Container starts and connects
   ```

2. **Agent Communication Test**
   - Load COOL configuration with at least one component
   - Trigger component connection
   - **Verify:** Check daLog messages appear in platform logs
   - **Verify:** Alarm messages appear in GUI (if running)

3. **Error Scenario Test**
   - Attempt to connect to non-existent component
   - **Expected:** Error messages logged via reportEvent()
   - **Verify:** Messages appear in both alarm system AND daLog
   - **Verify:** Message format unchanged from before

4. **State Transition Test**
   - Connect component
   - Issue configure command
   - Issue download command
   - **Verify:** No errors in state transitions
   - **Verify:** Warning messages (if any) appear correctly

**Acceptance Criteria:**
- ✅ All log messages still appear in daLog
- ✅ All alarm messages still appear in GUI
- ✅ Message text exactly matches previous format
- ✅ No "missing message" reports
- ✅ Severity levels preserved (INFO=<9, ERROR=9+)

---

#### Rollback Strategy

```bash
# If logging broken or messages lost:
git revert HEAD
git push origin main

# Platform will revert to previous dual-call pattern
```

**Rollback Risk:** LOW - Single file change, easy to revert

---

#### API Safety Analysis

✅ **No public API changes** - Internal refactoring only
✅ **No behavior changes** - reportEvent() calls same methods with same parameters
✅ **Message format preserved** - Exact same strings passed to reportAlarmMsg/dalogMsg
✅ **Severity handling unchanged** - Same severity IDs and constants

---

### COMMIT 4: Replace logging pairs in remaining files

**Objective:** Complete logging duplication removal

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 45 minutes
**LOC Impact:** -40 lines

---

#### Files Modified (3 files)

```
EDIT src/org/jlab/coda/afecs/codarc/ClientHeartBeatMonitor.java
EDIT src/org/jlab/coda/afecs/codarc/StateTransitioningMonitor.java
EDIT src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java
```

---

#### Detailed Changes

**File 1:** `src/org/jlab/coda/afecs/codarc/ClientHeartBeatMonitor.java`

**Replacements:** 4 pairs

**Example - Lines 85-97:**
```java
// BEFORE:
reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
        owner.myName,
        11,
        AConstants.ERROR,
        "Client stopped reporting");
dalogMsg(owner.myName,
        11,
        AConstants.ERROR,
        "Client stopped reporting");

// AFTER:
owner.reportEvent("Client stopped reporting", 11, AConstants.ERROR);
```

**Note:** `owner` is the CodaRCAgent instance, which extends AParent → ABase, so has access to reportEvent()

**Additional Replacements:**
- Lines 122-131 - Client not sending data
- Lines 198-207 - Heartbeat timeout

---

**File 2:** `src/org/jlab/coda/afecs/codarc/StateTransitioningMonitor.java`

**Replacements:** 3 pairs

**Example - Lines 64-76:**
```java
// BEFORE:
reportAlarmMsg(owner.me.getSession() + "/" + owner.me.getRunType(),
        owner.myName,
        11,
        AConstants.ERROR,
        "State transition timeout");
dalogMsg(owner.myName,
        11,
        AConstants.ERROR,
        "State transition timeout");

// AFTER:
owner.reportEvent("State transition timeout", 11, AConstants.ERROR);
```

**Additional Replacements:**
- Lines 98-110 - Transition failed
- Lines 113-121 - Reset timeout

---

**File 3:** `src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java`

**Replacements:** 8 pairs

**Example - Lines 742-750 (approximately):**
```java
// BEFORE:
reportAlarmMsg(me.getSession() + "/" + me.getRunType(),
        myName,
        9,
        AConstants.ERROR,
        "Component " + componentName + " failed to respond");
dalogMsg(myName,
        9,
        AConstants.ERROR,
        "Component " + componentName + " failed to respond");

// AFTER:
reportEvent("Component " + componentName + " failed to respond", 9, AConstants.ERROR);
```

**Note:** Exact line numbers may vary; use grep to find pairs:
```bash
grep -n -A 8 "reportAlarmMsg" src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java | grep -B 8 "dalogMsg"
```

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Count pairs in each file
for file in \
  src/org/jlab/coda/afecs/codarc/ClientHeartBeatMonitor.java \
  src/org/jlab/coda/afecs/codarc/StateTransitioningMonitor.java \
  src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java; do
  echo "File: $file"
  echo "  reportAlarmMsg: $(grep -c 'reportAlarmMsg' $file)"
  echo "  dalogMsg: $(grep -c 'dalogMsg' $file)"
done
```

**Post-commit checks:**
```bash
# 2. Verify reportEvent usage
for file in \
  src/org/jlab/coda/afecs/codarc/ClientHeartBeatMonitor.java \
  src/org/jlab/coda/afecs/codarc/StateTransitioningMonitor.java \
  src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java; do
  echo "File: $file"
  echo "  reportEvent: $(grep -c 'reportEvent' $file)"
done

# Expected: 4, 3, 8 respectively

# 3. Compile all three files
javac -cp "lib/*:build/classes" -d build/classes \
  src/org/jlab/coda/afecs/codarc/ClientHeartBeatMonitor.java \
  src/org/jlab/coda/afecs/codarc/StateTransitioningMonitor.java \
  src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java

# Expected: All compile successfully
```

**Testing:**

**Required Tests:**

1. **Heartbeat Monitoring Test**
   - Start platform + container + agent representing a component
   - Stop the physical component (simulate crash)
   - **Expected:** ClientHeartBeatMonitor detects timeout
   - **Verify:** "Client stopped reporting" appears in daLog
   - **Verify:** Alarm appears in GUI

2. **State Transition Timeout Test**
   - Configure a component with slow state transition
   - Issue transition command
   - Let it timeout
   - **Expected:** StateTransitioningMonitor detects timeout
   - **Verify:** "State transition timeout" appears in logs

3. **Supervisor Error Test**
   - Configure supervisor with multiple components
   - Disconnect one component during active run
   - **Expected:** Supervisor reports component failure
   - **Verify:** Error messages logged correctly

**Acceptance Criteria:**
- ✅ Heartbeat timeouts logged correctly
- ✅ State transition errors reported
- ✅ Supervisor error messages appear
- ✅ All messages have correct severity
- ✅ No duplicate messages (from old code)

---

#### Rollback Strategy

```bash
# If monitoring broken:
git revert HEAD
git push origin main
```

**Rollback Risk:** MEDIUM - Affects monitoring threads, but isolated changes

---

#### API Safety Analysis

✅ **No public API changes** - Internal monitoring threads only
✅ **No behavior changes** - Same messages, same timing
✅ **Message format preserved** - Exact same strings

---

## Phase 1 Summary

**After Commit 4:**
- ✅ 7 dead code files removed
- ✅ 1 new utility method added
- ✅ ~40 logging pairs replaced in 4 files
- ✅ ~160 LOC reduction
- ✅ No public API changes
- ✅ No behavior changes

**Risk Assessment:** LOW - Simple refactoring with clear rollback path

**Checkpoint:** Run full integration test before proceeding to Phase 2

---

## Phase 2: Reduce Duplication (Commits 5-8)

### COMMIT 5: Add SubscriptionManager utility

**Objective:** Create utility for managing cMsg subscriptions

**Risk Level:** 🟢 **LOW**
**Time Estimate:** 30 minutes
**LOC Impact:** +80 lines (new file)

---

#### Files Created

```
CREATE src/org/jlab/coda/afecs/system/util/SubscriptionManager.java
```

---

#### File Content

**File:** `src/org/jlab/coda/afecs/system/util/SubscriptionManager.java`

```java
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
```

---

#### Verification Steps

**Post-commit checks:**
```bash
# 1. Compile new class
javac -cp "lib/*" -d build/classes \
  src/org/jlab/coda/afecs/system/util/SubscriptionManager.java

# Expected: Compiles successfully

# 2. Verify JavaDoc
javadoc -d build/javadoc -cp "lib/*" \
  src/org/jlab/coda/afecs/system/util/SubscriptionManager.java

# Expected: Javadoc generates without errors

# 3. Check for syntax errors
grep -c "public.*subscribe" src/org/jlab/coda/afecs/system/util/SubscriptionManager.java
# Expected: 3 (subscribe, unsubscribe, unsubscribeAll)
```

**Testing:**
- ✅ Compilation test (must pass)
- ⚠️ Class not used yet (additive change)
- No runtime tests needed until next commit

---

#### Rollback Strategy

```bash
# If needed:
git revert HEAD
# Or simply delete the file
rm src/org/jlab/coda/afecs/system/util/SubscriptionManager.java
```

**Rollback Risk:** NONE - New file, not used anywhere

---

#### API Safety Analysis

✅ **No public API changes** - New internal utility class
✅ **No behavior changes** - Not integrated yet

---

### COMMIT 6: Use SubscriptionManager in AParent

**Objective:** Refactor base agent class to use subscription manager

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 45 minutes
**LOC Impact:** -30 lines

---

#### Files Modified

```
EDIT src/org/jlab/coda/afecs/agent/AParent.java
```

---

#### Detailed Changes

**File:** `src/org/jlab/coda/afecs/agent/AParent.java`

**Step 1: Add import**
```java
// Add to imports section (after line 40)
import org.jlab.coda.afecs.system.util.SubscriptionManager;
```

**Step 2: Add field**
```java
// Add field declaration (around line 80, after other field declarations)

/**
 * Manages all cMsg subscriptions for this agent.
 * Provides centralized subscription tracking and cleanup.
 */
protected SubscriptionManager subscriptionManager;
```

**Step 3: Initialize in constructor**
```java
// In constructor (around line 120)

// BEFORE:
public AParent(AComponent component, AContainer container, APlatform platform) {
    super();
    myContainer = container;
    myPlatform = platform;
    ...

// AFTER:
public AParent(AComponent component, AContainer container, APlatform platform) {
    super();
    subscriptionManager = new SubscriptionManager(); // ADD THIS LINE
    myContainer = container;
    myPlatform = platform;
    ...
```

**Step 4: Replace manual subscription with manager (around line 250)**

```java
// BEFORE:
try {
    myPlatformConnection.subscribe(myName,
            AConstants.AgentInfoRequestReport,
            new InfoRequestCB(),
            null);
} catch (cMsgException e) {
    e.printStackTrace();
}

// AFTER:
try {
    subscriptionManager.subscribe(myPlatformConnection,
            myName,
            AConstants.AgentInfoRequestReport,
            new InfoRequestCB());
} catch (cMsgException e) {
    e.printStackTrace();
}
```

**Step 5: Add cleanup method (if not exists) or update existing**

```java
// Add new method or update existing disconnect() method

/**
 * Disconnect from platform and cleanup all subscriptions.
 * Called when agent is being removed or platform is shutting down.
 */
public void disconnect() {
    // Unsubscribe all managed subscriptions
    if (subscriptionManager != null) {
        subscriptionManager.unsubscribeAll(myPlatformConnection);
    }

    // Disconnect from platform
    if (myPlatformConnection != null && myPlatformConnection.isConnected()) {
        try {
            myPlatformConnection.disconnect();
        } catch (cMsgException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
}
```

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Find current subscription pattern
grep -n "myPlatformConnection.subscribe" src/org/jlab/coda/afecs/agent/AParent.java
# Note line numbers for replacement
```

**Post-commit checks:**
```bash
# 2. Verify SubscriptionManager used
grep -c "subscriptionManager" src/org/jlab/coda/afecs/agent/AParent.java
# Expected: At least 4 (field, init, subscribe call, unsubscribeAll call)

# 3. Compile AParent and all subclasses
javac -cp "lib/*:build/classes" -d build/classes \
  src/org/jlab/coda/afecs/agent/AParent.java \
  src/org/jlab/coda/afecs/codarc/CodaRCAgent.java \
  src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java

# Expected: All compile successfully
```

**Testing:**

**Required Tests:**

1. **Agent Registration Test**
   ```bash
   # Start platform
   bin/coda/platform test_expid

   # Start container (spawns agents)
   bin/coda/container test_expid

   # Expected: Agents register successfully
   # Verify: Check platform log for agent registration messages
   ```

2. **Info Request Test**
   - Use RcSpy or PLAsk to query agent info
   - **Expected:** Agents respond to info requests
   - **Verify:** InfoRequestCB callback is triggered

3. **Agent Disconnect Test**
   - Start platform + container + agents
   - Shutdown container gracefully
   - **Expected:** Agents disconnect cleanly
   - **Verify:** No "failed to unsubscribe" errors in logs
   - **Verify:** Platform shows agents as disconnected

4. **Subscription Cleanup Test**
   - Start/stop container multiple times
   - **Expected:** No subscription handle leaks
   - **Verify:** cMsg server shows correct subscription count

**Acceptance Criteria:**
- ✅ Agents register and communicate
- ✅ Info requests work
- ✅ Clean disconnect (no errors)
- ✅ No subscription leaks

---

#### Rollback Strategy

```bash
# If agent communication broken:
git revert HEAD
git push origin main

# Restores manual subscription management
```

**Rollback Risk:** MEDIUM - Affects all agents, but isolated change

---

#### API Safety Analysis

✅ **No public API changes** - Internal refactoring of AParent
✅ **Behavior preserved** - Same subscriptions, same callbacks
✅ **Subclass compatible** - Protected field accessible to subclasses

---

### COMMIT 7: Use SubscriptionManager in CodaRCAgent

**Objective:** Refactor RC agent to use subscription manager

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 1 hour
**LOC Impact:** -40 lines

---

#### Files Modified

```
EDIT src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
```

---

#### Detailed Changes

**File:** `src/org/jlab/coda/afecs/codarc/CodaRCAgent.java`

**Step 1: Remove subscription handle fields (lines 84-101)**

```java
// DELETE these fields:
private cMsgSubscriptionHandle statusSH;
private cMsgSubscriptionHandle daLogSH;
private cMsgSubscriptionHandle responseSH;
private cMsgSubscriptionHandle emuEventsPerEtBufferLevel;
```

**Note:** `subscriptionManager` is inherited from AParent (protected)

**Step 2: Update _codaClientSubscribe() method (lines 656-716)**

```java
// BEFORE:
private void _codaClientSubscribe() {
    if (isRcClientConnected()) {
        try {
            statusSH = myCRCClientConnection.subscribe(myName,
                    AConstants.RcReportStatus,
                    new StatusMsgCB(),
                    null);
            startTime = System.currentTimeMillis();

            responseSH = myCRCClientConnection.subscribe(myName,
                    AConstants.RcResponse,
                    new ResponseMsgCB(),
                    null);
            daLogSH = myCRCClientConnection.subscribe(myName,
                    AConstants.RcReportDalog,
                    new DaLogMsgCB(),
                    null);

            // ... EMU subscription code ...

        } catch (cMsgException e) {
            e.printStackTrace();
        }
    }
}

// AFTER:
private void _codaClientSubscribe() {
    if (isRcClientConnected()) {
        try {
            subscriptionManager.subscribe(myCRCClientConnection,
                    myName,
                    AConstants.RcReportStatus,
                    new StatusMsgCB());
            startTime = System.currentTimeMillis();

            subscriptionManager.subscribe(myCRCClientConnection,
                    myName,
                    AConstants.RcResponse,
                    new ResponseMsgCB());

            subscriptionManager.subscribe(myCRCClientConnection,
                    myName,
                    AConstants.RcReportDalog,
                    new DaLogMsgCB());

            // Ask platform for the linked emu name
            if (me.getType().equals(ACodaType.ROC.name()) ||
                    me.getType().equals(ACodaType.GT.name()) ||
                    me.getType().equals(ACodaType.TS.name())) {

                // ... [existing EMU lookup code] ...

                if (msg != null && msg.getText() != null) {
                    String emu_name = msg.getText();
                    subscriptionManager.subscribe(myPlatformConnection,
                            emu_name,
                            "eventsPerBuffer",
                            new RocEvtPerBufferMsgCB());
                }
            }

        } catch (cMsgException e) {
            e.printStackTrace();
        }
    }
}
```

**Step 3: Update _codaClientUnSubscribe() method (lines 723-735)**

```java
// BEFORE:
private void _codaClientUnSubscribe() {
    if (isRcClientConnected()) {
        try {
            if (statusSH != null) myCRCClientConnection.unsubscribe(statusSH);
            if (responseSH != null) myCRCClientConnection.unsubscribe(responseSH);
            if (daLogSH != null) myCRCClientConnection.unsubscribe(daLogSH);
            if (emuEventsPerEtBufferLevel != null)
                myPlatformConnection.unsubscribe(emuEventsPerEtBufferLevel);
        } catch (cMsgException e) {
            e.printStackTrace();
        }
    }
}

// AFTER:
private void _codaClientUnSubscribe() {
    if (isRcClientConnected() && subscriptionManager != null) {
        subscriptionManager.unsubscribeAll(myCRCClientConnection);
        subscriptionManager.unsubscribeAll(myPlatformConnection);
    }
}
```

**Step 4: Update restartClientStatusSubscription() method (lines 737-749)**

```java
// BEFORE:
void restartClientStatusSubscription() {
    try {
        if (statusSH != null) myCRCClientConnection.unsubscribe(statusSH);
        statusSH = myCRCClientConnection.subscribe(myName,
                AConstants.RcReportStatus,
                new StatusMsgCB(),
                null);
    } catch (cMsgException e) {
        e.printStackTrace();
    }
}

// AFTER:
void restartClientStatusSubscription() {
    try {
        // Unsubscribe existing status subscription
        subscriptionManager.unsubscribe(myCRCClientConnection,
                myName,
                AConstants.RcReportStatus);

        // Re-subscribe
        subscriptionManager.subscribe(myCRCClientConnection,
                myName,
                AConstants.RcReportStatus,
                new StatusMsgCB());
    } catch (cMsgException e) {
        e.printStackTrace();
    }
}
```

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Count manual subscription handle usage
grep -c "SH;" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
# Expected: 4 (field declarations)

grep -c "\\.subscribe" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
# Expected: ~4 manual subscriptions
```

**Post-commit checks:**
```bash
# 2. Verify no subscription handle fields remain
grep -c "cMsgSubscriptionHandle" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
# Expected: 0

# 3. Verify subscriptionManager usage
grep -c "subscriptionManager" src/org/jlab/coda/afecs/codarc/CodaRCAgent.java
# Expected: ~6 (subscribe calls + unsubscribe calls)

# 4. Compile
javac -cp "lib/*:build/classes" -d build/classes \
  src/org/jlab/coda/afecs/codarc/CodaRCAgent.java

# Expected: Compiles successfully
```

**Testing:**

**Critical Tests (Run All):**

1. **Component Connection Test**
   - Start platform + container
   - Load COOL config with ROC component
   - Physical component joins platform
   - **Expected:** Agent connects to RC domain
   - **Verify:** Status subscription active (check cMsg server)
   - **Verify:** Component state updates in GUI

2. **Status Message Test**
   - Component in active state, sending data
   - **Expected:** Status messages received
   - **Verify:** Event rate/data rate display in GUI
   - **Verify:** StatusMsgCB callback triggered (check logs)

3. **DaLog Message Test**
   - Trigger error in component (e.g., file write failure)
   - **Expected:** Component sends daLog message
   - **Verify:** Message appears in platform daLog
   - **Verify:** DaLogMsgCB callback triggered

4. **Response Message Test**
   - Send sync request to component (get state, get session, etc.)
   - **Expected:** Component responds
   - **Verify:** ResponseMsgCB receives response

5. **EMU Buffer Level Test (ROC only)**
   - Configure ROC + EMU pair
   - Run data acquisition
   - **Expected:** ROC receives EMU buffer level updates
   - **Verify:** RocEvtPerBufferMsgCB triggered
   - **Verify:** Buffer levels display correctly

6. **Subscription Restart Test**
   - Trigger `restartClientStatusSubscription()` (e.g., after error)
   - **Expected:** Subscription cleanly restarted
   - **Verify:** Status messages resume
   - **Verify:** No duplicate subscriptions (check cMsg server)

7. **Disconnect Test**
   - Component running in active state
   - Stop component or container
   - **Expected:** Clean unsubscribe
   - **Verify:** No errors in logs
   - **Verify:** No orphaned subscriptions

**Acceptance Criteria:**
- ✅ All subscriptions work (status, daLog, response, EMU)
- ✅ Callbacks triggered correctly
- ✅ No subscription leaks
- ✅ Restart subscription works
- ✅ Clean disconnect

---

#### Rollback Strategy

```bash
# If RC communication broken:
git revert HEAD
git push origin main

# Restores manual handle management
```

**Rollback Risk:** MEDIUM-HIGH - Critical component communication path

**Mitigation:** Test thoroughly in development environment first

---

#### API Safety Analysis

✅ **No public API changes** - Internal CodaRCAgent refactoring
✅ **Behavior preserved** - Same subscriptions, same message handling
✅ **Message flow unchanged** - Status/daLog/response messages work identically

---

### COMMIT 8: Fix InfluxInjector blocking constructor

**Objective:** Convert blocking constructor to proper background thread

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 45 minutes
**LOC Impact:** +30 lines

---

#### Files Modified (2 files)

```
EDIT src/org/jlab/coda/afecs/influx/InfluxInjector.java
EDIT src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java (caller)
```

---

#### Detailed Changes

**File 1:** `src/org/jlab/coda/afecs/influx/InfluxInjector.java`

**Current Code (BROKEN - lines 30-48):**
```java
public InfluxInjector(SupervisorAgent sup, String dbNode, String dbName) throws JinFluxException {
    super();
    owner = sup;
    jinFluxDriver = new JinFluxDriver(dbNode, dbName, null);

    do {
        if (owner.myPlatform.influxDb) {
            if (owner.me.getState().equals(AConstants.active)) {
                jinFluxDriver.push(owner);
            }
        }
        AfecsTool.sleep(2000);
    } while (true);  // NEVER RETURNS!
}
```

**Fixed Code:**
```java
package org.jlab.coda.afecs.influx;

import org.influxdb.dto.Point;
import org.jlab.coda.afecs.platform.APlatform;
import org.jlab.coda.afecs.supervisor.SupervisorAgent;
import org.jlab.coda.afecs.system.ABase;
import org.jlab.coda.afecs.system.AConstants;
import org.jlab.coda.afecs.system.util.AfecsTool;
import org.jlab.coda.cMsg.*;
import org.jlab.coda.jinflux.JinFluxException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Influx DB injector class.
 * Runs as a background thread, periodically pushing metrics to InfluxDB.
 *
 * @author gurjyan on 3/9/18.
 */
public class InfluxInjector implements Runnable {

    // JinFluxDriver object
    private JinFluxDriver jinFluxDriver;

    private static final String dbNode = "claraweb.jlab.org";
    private static final String dbName = "afecs";
    private SupervisorAgent owner;

    // Thread control
    private volatile boolean running = false;
    private Thread workerThread;

    /**
     * Constructor initializes connection but does not start injection.
     * Call start() to begin background injection thread.
     *
     * @param sup Supervisor agent to monitor
     * @param dbNode InfluxDB hostname
     * @param dbName Database name
     * @throws JinFluxException if connection fails
     */
    public InfluxInjector(SupervisorAgent sup, String dbNode, String dbName) throws JinFluxException {
        this.owner = sup;

        // Connect to the influxDB and create JinFlux connection
        this.jinFluxDriver = new JinFluxDriver(dbNode, dbName, null);
    }

    /**
     * Constructor using default database node and name.
     *
     * @param sup Supervisor agent
     * @param isLocal (ignored, kept for backward compatibility)
     * @throws JinFluxException if connection fails
     */
    public InfluxInjector(SupervisorAgent sup, boolean isLocal) throws JinFluxException {
        this(sup, dbNode, dbName);
    }

    /**
     * Start the background injection thread.
     * Safe to call multiple times (will not start duplicate threads).
     */
    public void start() {
        if (!running) {
            running = true;
            workerThread = new Thread(this, "InfluxInjector-" + owner.myName);
            workerThread.setDaemon(true); // Don't prevent JVM shutdown
            workerThread.start();
        }
    }

    /**
     * Stop the background injection thread.
     */
    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Background thread run method.
     * Pushes metrics every 2 seconds while in active state.
     */
    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Only inject if InfluxDB is enabled and supervisor is active
                if (owner.myPlatform.influxDb &&
                    owner.me.getState().equals(AConstants.active)) {
                    jinFluxDriver.push(owner);
                }

                Thread.sleep(2000); // 2 second interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // Exit cleanly on interrupt
            } catch (Exception e) {
                // Log error but continue running
                System.err.println("InfluxInjector error: " + e.getMessage());
            }
        }
        running = false;
    }

    /**
     * Check if injection thread is running.
     *
     * @return true if active
     */
    public boolean isRunning() {
        return running && workerThread != null && workerThread.isAlive();
    }

    /**
     * Method that injects user defined message into influxDB.
     * [EXISTING METHOD - NO CHANGES]
     */
    private void userRequestJinFluxInject(cMsgMessage msg) {
        // ... existing code unchanged ...
    }
}
```

---

**File 2:** `src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java`

**Find the line where InfluxInjector is created (search for "new InfluxInjector")**

```java
// BEFORE (line ~XXX):
new InfluxInjector(this, dbNode, dbName);  // Blocks forever!

// AFTER:
InfluxInjector influxInjector = new InfluxInjector(this, dbNode, dbName);
influxInjector.start();  // Start background thread, returns immediately
```

**Also add field to SupervisorAgent to track injector (for cleanup):**
```java
// Add field (around line 100):
private InfluxInjector influxInjector;

// Update creation code:
if (owner.myPlatform.influxDb) {
    try {
        influxInjector = new InfluxInjector(this, dbNode, dbName);
        influxInjector.start();
    } catch (JinFluxException e) {
        e.printStackTrace();
    }
}

// Add cleanup in SupervisorAgent disconnect/shutdown method:
if (influxInjector != null && influxInjector.isRunning()) {
    influxInjector.stop();
}
```

---

#### Verification Steps

**Pre-commit checks:**
```bash
# 1. Verify current blocking behavior
grep -A 5 "do {" src/org/jlab/coda/afecs/influx/InfluxInjector.java | grep "while (true)"
# Expected: Shows the infinite loop in constructor

# 2. Find caller
grep -n "new InfluxInjector" src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java
# Note line number for update
```

**Post-commit checks:**
```bash
# 3. Verify implements Runnable
grep "implements Runnable" src/org/jlab/coda/afecs/influx/InfluxInjector.java
# Expected: Found

# 4. Verify start() method exists
grep -c "public void start()" src/org/jlab/coda/afecs/influx/InfluxInjector.java
# Expected: 1

# 5. Verify supervisor calls start()
grep "influxInjector.start()" src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java
# Expected: Found

# 6. Compile both files
javac -cp "lib/*:build/classes" -d build/classes \
  src/org/jlab/coda/afecs/influx/InfluxInjector.java \
  src/org/jlab/coda/afecs/supervisor/SupervisorAgent.java

# Expected: Compiles successfully
```

**Testing:**

**Critical Tests:**

1. **Supervisor Startup Test**
   ```bash
   # Start platform with InfluxDB enabled
   bin/coda/platform test_expid

   # Start container (creates supervisor)
   bin/coda/container test_expid

   # Expected: Container starts immediately (does NOT hang)
   # Verify: Supervisor agent created
   # Verify: InfluxInjector thread started (check thread dump)
   ```

2. **Thread Naming Test**
   ```bash
   # After supervisor starts
   jps -l  # Get Java PID
   jstack <PID> | grep InfluxInjector

   # Expected: See "InfluxInjector-<supervisor-name>" thread
   ```

3. **Injection Test**
   - Configure supervisor with components
   - Transition to active state
   - Run for 10+ seconds
   - **Expected:** Metrics pushed to InfluxDB every 2 seconds
   - **Verify:** Check InfluxDB for new data points
   - **Query:** `SELECT * FROM <measurement> ORDER BY time DESC LIMIT 5`

4. **No Injection When Inactive Test**
   - Supervisor in configured state (not active)
   - Wait 10 seconds
   - **Expected:** No metrics pushed
   - **Verify:** InfluxDB has no new data for inactive period

5. **Clean Shutdown Test**
   - Supervisor running with InfluxInjector active
   - Stop container gracefully
   - **Expected:** InfluxInjector.stop() called
   - **Verify:** Thread stops within 5 seconds
   - **Verify:** No "thread did not stop" warnings

6. **Restart Test**
   - Start/stop supervisor multiple times
   - **Expected:** No thread leaks
   - **Verify:** Check thread count with `jstack`
   - **Verify:** Only one InfluxInjector thread per supervisor

**Acceptance Criteria:**
- ✅ Supervisor startup does NOT hang
- ✅ InfluxInjector runs as background thread
- ✅ Metrics pushed every 2 seconds when active
- ✅ No injection when inactive
- ✅ Clean thread shutdown
- ✅ No thread leaks

---

#### Rollback Strategy

```bash
# If thread issues occur:
git revert HEAD
git push origin main

# Reverts to blocking constructor (but that's a bug, so only rollback if critical issue)
```

**Rollback Risk:** LOW - Bug fix, rollback only if new issues introduced

**Alternative Mitigation:**
- If thread management has issues, can disable InfluxDB temporarily via config

---

#### API Safety Analysis

✅ **No public API changes** - Internal InfluxInjector implementation
✅ **Behavior preserved** - Metrics still pushed at same interval
✅ **Constructor signature unchanged** - Still takes same parameters
⚠️ **Caller must call start()** - But this is internal to SupervisorAgent

---

## Phase 2 Summary

**After Commit 8:**
- ✅ SubscriptionManager created and integrated
- ✅ AParent uses subscription manager
- ✅ CodaRCAgent uses subscription manager (removes 4 fields)
- ✅ InfluxInjector bug fixed (no longer blocks)
- ✅ ~120 LOC reduction
- ✅ No public API changes
- ✅ Critical bug fixed

**Risk Assessment:** MEDIUM - Touches communication infrastructure

**Checkpoint:** Full integration test required before Phase 3

---

## Phase 3: Structural Improvements (Commits 9-12)

### COMMIT 9: Extract BaseDialogComponent

**Objective:** Create base class for UI dialog boilerplate

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 1 hour
**LOC Impact:** +100 lines (new), -30 lines (AListDialog refactor)

---

#### Files Created and Modified

```
CREATE src/org/jlab/coda/afecs/ui/rcgui/util/BaseDialogComponent.java
EDIT   src/org/jlab/coda/afecs/ui/rcgui/util/AListDialog.java
```

---

#### Detailed Changes

**File 1 (NEW):** `src/org/jlab/coda/afecs/ui/rcgui/util/BaseDialogComponent.java`

```java
package org.jlab.coda.afecs.ui.rcgui.util;

import javax.accessibility.Accessible;
import javax.swing.*;
import java.awt.*;

/**
 * Base class for dialog-based GUI components.
 * Provides common dialog creation, layout, and return value handling.
 *
 * <p>Subclasses should:</p>
 * <ul>
 * <li>Call {@link #initDialog(Component)} to create the dialog</li>
 * <li>Implement {@link #setupDialogContent()} to add custom content</li>
 * <li>Use {@link #setReturnValue(int)} to set OK/CANCEL result</li>
 * </ul>
 *
 * @author gurjyan
 * @version 4.x
 */
public abstract class BaseDialogComponent extends JComponent implements Accessible {

    /** Dialog container */
    protected JDialog dialog;

    /** Information title (for informational dialogs) */
    protected String infoTitle;

    /** GUI window title */
    protected String guiTitle;

    /** Return value: User cancelled */
    public static final int RET_CANCEL = 0;

    /** Return value: User confirmed (OK) */
    public static final int RET_OK = 1;

    /** Current return value (OK or CANCEL) */
    private int returnValue = RET_CANCEL;

    /**
     * Initialize and create the dialog.
     * Template method that calls {@link #setupDialogContent()} for customization.
     *
     * @param parent Parent component for dialog positioning
     * @return Created dialog
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true
     */
    protected JDialog initDialog(Component parent) throws HeadlessException {
        Frame frame = parent instanceof Frame ? (Frame) parent :
                      (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);

        dialog = new JDialog(frame, guiTitle, true); // Modal dialog
        dialog.setComponentOrientation(this.getComponentOrientation());

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);

        // Call template method for subclass-specific setup
        setupDialogContent();

        // Standard dialog finalization
        dialog.getRootPane().setDefaultButton(getDefaultButton());
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        return dialog;
    }

    /**
     * Template method for subclasses to add dialog-specific content.
     * Called during {@link #initDialog(Component)}.
     *
     * <p>Subclasses should override to:</p>
     * <ul>
     * <li>Add buttons with listeners</li>
     * <li>Configure layout</li>
     * <li>Set up event handlers</li>
     * </ul>
     */
    protected abstract void setupDialogContent();

    /**
     * Get the default button for the dialog (activated by Enter key).
     * Subclasses should override to return their OK/primary button.
     *
     * @return Default button, or null if none
     */
    protected abstract JButton getDefaultButton();

    /**
     * Show the dialog and wait for user response.
     *
     * @return {@link #RET_OK} or {@link #RET_CANCEL}
     */
    public int showDialog() {
        if (dialog == null) {
            throw new IllegalStateException("Dialog not initialized. Call initDialog() first.");
        }
        returnValue = RET_CANCEL; // Default to cancel
        dialog.setVisible(true);
        return returnValue;
    }

    /**
     * Set the dialog return value.
     * Typically called by OK/Cancel button handlers before closing dialog.
     *
     * @param value {@link #RET_OK} or {@link #RET_CANCEL}
     */
    protected void setReturnValue(int value) {
        this.returnValue = value;
    }

    /**
     * Get the dialog return value.
     *
     * @return Last return value set
     */
    public int getReturnValue() {
        return returnValue;
    }

    /**
     * Close the dialog.
     */
    protected void closeDialog() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }

    /**
     * Set the GUI title (displayed in dialog title bar).
     *
     * @param title Title string
     */
    public void setGuiTitle(String title) {
        this.guiTitle = title;
        if (dialog != null) {
            dialog.setTitle(title);
        }
    }

    /**
     * Get the GUI title.
     *
     * @return Title string
     */
    public String getGuiTitle() {
        return guiTitle;
    }

    /**
     * Set the information title.
     *
     * @param title Info title string
     */
    public void setInfoTitle(String title) {
        this.infoTitle = title;
    }

    /**
     * Get the information title.
     *
     * @return Info title string
     */
    public String getInfoTitle() {
        return infoTitle;
    }
}
```

---

**File 2 (EDIT):** `src/org/jlab/coda/afecs/ui/rcgui/util/AListDialog.java`

**Refactor to extend BaseDialogComponent:**

```java
// BEFORE:
public class AListDialog extends JComponent implements Accessible {
    public JDialog dialog;
    private String InfoTitle;
    private String GuiTitle;
    public static final int RET_CANCEL = 0;
    public static final int RET_OK = 1;

    // ... 40+ lines of dialog setup code ...
}

// AFTER:
public class AListDialog extends BaseDialogComponent {
    private JButton okButton;
    private JButton cancelButton;
    private JList<String> list;

    public AListDialog(Component parent, String[] items, String infoTitle, String guiTitle) {
        this.infoTitle = infoTitle;
        this.guiTitle = guiTitle;

        // Create list
        list = new JList<>(items);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Initialize dialog
        initDialog(parent);
    }

    @Override
    protected void setupDialogContent() {
        // Add list in scroll pane
        JScrollPane scrollPane = new JScrollPane(list);
        add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            setReturnValue(RET_OK);
            closeDialog();
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            setReturnValue(RET_CANCEL);
            closeDialog();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    protected JButton getDefaultButton() {
        return okButton;
    }

    /**
     * Get selected item from list.
     *
     * @return Selected item or null if nothing selected
     */
    public String getSelectedItem() {
        return list.getSelectedValue();
    }
}
```

---

#### Verification Steps

**Post-commit checks:**
```bash
# 1. Compile base class
javac -cp "lib/*" -d build/classes \
  src/org/jlab/coda/afecs/ui/rcgui/util/BaseDialogComponent.java

# Expected: Compiles successfully

# 2. Compile refactored AListDialog
javac -cp "lib/*:build/classes" -d build/classes \
  src/org/jlab/coda/afecs/ui/rcgui/util/AListDialog.java

# Expected: Compiles successfully

# 3. Count LOC reduction
wc -l src/org/jlab/coda/afecs/ui/rcgui/util/AListDialog.java
# Expected: Significantly fewer lines than before
```

**Testing:**

**UI Tests (Manual):**

1. **List Dialog Test**
   - Start RcGui
   - Trigger action that shows AListDialog (e.g., select configuration)
   - **Expected:** Dialog appears with list
   - **Test:** Select item and click OK → Returns selection
   - **Test:** Click Cancel → Returns null
   - **Test:** Press Enter → Activates OK button

2. **Dialog Layout Test**
   - **Verify:** Title bar shows correct title
   - **Verify:** Dialog is modal (blocks parent)
   - **Verify:** Dialog centered on parent
   - **Verify:** OK/Cancel buttons aligned right

**Acceptance Criteria:**
- ✅ AListDialog compiles and works
- ✅ Dialog appearance unchanged
- ✅ OK/Cancel behavior preserved
- ✅ No UI regressions

---

#### Rollback Strategy

```bash
# If UI broken:
git revert HEAD
```

**Rollback Risk:** LOW - Single dialog refactored

---

#### API Safety Analysis

✅ **No public API changes** - Internal UI classes
✅ **Dialog behavior preserved** - Same appearance and functionality

---

### COMMIT 10: Refactor remaining dialogs to use BaseDialogComponent

**Objective:** Apply base class to 6 more dialog classes

**Risk Level:** 🟡 **MEDIUM**
**Time Estimate:** 2 hours
**LOC Impact:** -200 lines

---

#### Files Modified (6 files)

```
EDIT src/org/jlab/coda/afecs/ui/rcgui/util/ASliderDialog.java
EDIT src/org/jlab/coda/afecs/ui/rcgui/util/AutoModeForm.java
EDIT src/org/jlab/coda/afecs/ui/rcgui/util/CLRNSetGui.java
EDIT src/org/jlab/coda/afecs/ui/rcgui/util/UserAppDefGui.java
EDIT src/org/jlab/coda/afecs/ui/rcgui/util/RtvTable.java
EDIT src/org/jlab/coda/afecs/ui/rcgui/util/ShowMessage.java
```

---

#### Changes Summary

**Each file follows same pattern:**

1. Change `extends JComponent` → `extends BaseDialogComponent`
2. Remove duplicate fields: `dialog`, `InfoTitle`, `GuiTitle`, `RET_OK`, `RET_CANCEL`
3. Extract dialog setup code to `setupDialogContent()` override
4. Implement `getDefaultButton()` override
5. Replace manual return value handling with `setReturnValue()`
6. Replace manual close with `closeDialog()`

**Example for ASliderDialog.java:**

```java
// BEFORE (80+ lines):
public class ASliderDialog extends JComponent implements Accessible {
    public JDialog dialog;
    private String InfoTitle;
    private String GuiTitle;
    public static final int RET_CANCEL = 0;
    public static final int RET_OK = 1;

    private JSlider slider;
    // ... tons of dialog setup boilerplate ...
}

// AFTER (~40 lines):
public class ASliderDialog extends BaseDialogComponent {
    private JSlider slider;
    private JButton okButton;
    private JButton cancelButton;

    public ASliderDialog(Component parent, int min, int max, int initial, String title) {
        this.guiTitle = title;
        slider = new JSlider(min, max, initial);
        initDialog(parent);
    }

    @Override
    protected void setupDialogContent() {
        setLayout(new BorderLayout());
        add(slider, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            setReturnValue(RET_OK);
            closeDialog();
        });
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            setReturnValue(RET_CANCEL);
            closeDialog();
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    @Override
    protected JButton getDefaultButton() {
        return okButton;
    }

    public int getSliderValue() {
        return slider.getValue();
    }
}
```

---

#### Verification Steps

**Post-commit checks:**
```bash
# 1. Compile all refactored dialogs
for file in \
  ASliderDialog \
  AutoModeForm \
  CLRNSetGui \
  UserAppDefGui \
  RtvTable \
  ShowMessage; do
  javac -cp "lib/*:build/classes" -d build/classes \
    src/org/jlab/coda/afecs/ui/rcgui/util/${file}.java
done

# Expected: All compile successfully

# 2. Count total LOC reduction
# (Compare line counts before/after)
```

**Testing:**

**UI Integration Tests (Manual):**
- Test each dialog in RcGui application
- Verify appearance, behavior, and return values

**Full test plan in separate QA document**

---

#### Rollback Strategy

```bash
# If multiple dialogs broken:
git revert HEAD
```

**Rollback Risk:** MEDIUM - Multiple UI classes affected

---

### COMMIT 11: Extract SupervisorStateManager from SupervisorAgent

**Objective:** Begin decomposing god class

**Risk Level:** 🔴 **HIGH**
**Time Estimate:** 3 hours
**LOC Impact:** +400 lines (new), -300 lines (moved)

---

**NOTE:** This is a complex refactoring. Full details in separate document due to length.

**Summary:**
- Extract state management logic to new class
- Supervisor delegates to state manager
- Maintains backward compatibility

---

### COMMIT 12: Add formatting config and apply

**Objective:** Add .editorconfig and checkstyle.xml

**Risk Level:** 🟢 **LOW**
**Time Estimate:** 30 minutes
**LOC Impact:** +200 lines config, cosmetic changes to code

---

**Files:**
- CREATE `.editorconfig`
- CREATE `checkstyle.xml`
- FORMAT all files modified in commits 1-11

---

## FINAL SUMMARY

**Total Refactoring Impact:**

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Total Java files | 136 | 130 | -6 (deleted) + 3 (created) |
| Total LOC | ~12,000 | ~11,250 | -750 |
| Duplication (logging) | 40 pairs | 0 pairs | -160 LOC |
| Duplication (subscriptions) | 7 patterns | 0 patterns | -120 LOC |
| God classes | 2 | 1 (partial fix) | -300 LOC |

**Commits:**
- 4 commits: Phase 1 (Low risk)
- 4 commits: Phase 2 (Medium risk)
- 4 commits: Phase 3 (Medium-High risk)

**Testing Requirements:**
- Unit tests: Add for new utility classes
- Integration tests: Full platform/container/agent cycle
- UI tests: Manual testing of dialogs
- Regression tests: Verify no behavior changes

---

## APPROVAL REQUIRED

Before proceeding to Step 3 (execution), please review:

1. ✅ Scope of each commit
2. ✅ Risk levels and mitigation strategies
3. ✅ Public API safety guarantees
4. ✅ Rollback strategies
5. ✅ Testing requirements

**Questions to answer:**

**Q1:** Are the plugin stub classes (AEpics, ARc, ASnmp) planned for future use or should they be deleted?

**Q2:** Should we proceed with all 12 commits or split into 2 releases (Phase 1-2 now, Phase 3 later)?

**Q3:** Is InfluxDB integration actively used? (Commit 8 priority)

**Q4:** Who will perform UI testing for dialog refactoring (Commits 9-10)?

**Q5:** What is the test environment for validation before production deployment?

---

*End of Step 2 Refactoring Plan*
