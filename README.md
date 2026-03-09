# AFECS Gradle Build System - Quick Start Guide

**Created:** 2026-03-04
**Author:** Vardan Gyurjyan
**Purpose:** Quick reference for using Gradle with AFECS

---

## Prerequisites

**None!** The Gradle wrapper (`gradlew`) is included. No need to install Gradle.

**Requirements:**
- Java 8 or later
- All JAR files in `lib/` directory
- cMsg JAR (auto-detected in `../cMsg/java/jars/java8/`)

---

## Quick Commands

### Build & Test

```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Run tests only
./gradlew test

# Check dependencies
./gradlew checkDeps
```

### Run Applications

```bash
# Run Platform
./gradlew runPlatform

# Run Platform with arguments
./gradlew runPlatform --args="test_expid"

# Run Container
./gradlew runContainer --args="test_expid"

# Run GUI
./gradlew runGui

# Run COOL Compiler
./gradlew compileCool --args="path/to/config.cool"
```

### Create Deployable JARs

```bash
# Standard JAR
./gradlew jar
# Output: build/libs/afecs-4.0.0-SNAPSHOT.jar

# Fat JAR (includes all dependencies)
./gradlew fatJar
# Output: build/libs/afecs-all-4.0.0-SNAPSHOT-fat.jar
```

### Distribution

```bash
# Create distribution ZIP/TAR
./gradlew distZip
./gradlew distTar
# Output: build/distributions/
```

---

## Build Structure

```
afecs/
├── build.gradle           # Main build configuration
├── settings.gradle        # Project settings
├── gradle.properties      # Build properties
├── gradlew               # Unix/Linux/Mac wrapper script
├── gradlew.bat           # Windows wrapper script
├── gradle/
│   └── wrapper/          # Gradle wrapper files
├── src/                  # Source code
├── lib/                  # Local JAR dependencies
└── build/                # Build outputs (generated)
    ├── classes/          # Compiled .class files
    ├── libs/             # Built JARs
    ├── reports/          # Test and analysis reports
    └── distributions/    # Distribution archives
```

---

## Common Tasks

### 1. First Time Setup

```bash
# Test that Gradle works
./gradlew buildInfo

# Check all dependencies are present
./gradlew checkDeps

# Build the project
./gradlew build
```

### 2. Daily Development

```bash
# Quick compile check
./gradlew compileJava

# Build and test
./gradlew build

# Clean before building
./gradlew clean build
```

### 3. Deploy to Remote Node

```bash
# Create fat JAR
./gradlew fatJar

# Copy to remote node
scp build/libs/afecs-all-*.jar user@remote-node:/path/to/afecs/

# Or create distribution
./gradlew distZip
scp build/distributions/afecs-*.zip user@remote-node:/path/
```

### 4. IDE Integration

**Eclipse:**
```bash
./gradlew eclipse
# Then import as existing project in Eclipse
```

**IntelliJ IDEA:**
```bash
# Just open the project directory in IntelliJ
# It will auto-detect build.gradle
```

---

## Troubleshooting

### Issue: "Could not find cMsg"

**Solution:**
```bash
# Option 1: Check if cMsg exists in parent directory
ls -l ../cMsg/java/jars/java8/

# Option 2: Copy cMsg JAR to lib/
cp /path/to/cMsg-6.0.jar lib/

# Option 3: Update build.gradle to point to correct location
# Edit line ~52 in build.gradle
```

### Issue: "OutOfMemoryError"

**Solution:**
```bash
# Increase Gradle memory in gradle.properties
echo "org.gradle.jvmargs=-Xmx4096m" >> gradle.properties

# Or run with more memory
./gradlew build -Dorg.gradle.jvmargs=-Xmx4096m
```

### Issue: "Permission denied: ./gradlew"

**Solution:**
```bash
chmod +x gradlew
./gradlew build
```

### Issue: Build fails with dependency errors

**Solution:**
```bash
# Force dependency refresh
./gradlew build --refresh-dependencies

# Check what's missing
./gradlew checkDeps
```

---

## Gradle vs. Manual Compilation

| Aspect | Manual (shell scripts) | Gradle |
|--------|----------------------|--------|
| **Setup** | Set CLASSPATH manually | Automatic |
| **Dependencies** | Manual JAR management | Automatic resolution |
| **Incremental builds** | Rebuild everything | Only changed files |
| **Parallel compilation** | No | Yes (faster) |
| **Testing** | Manual | Integrated |
| **IDE integration** | Manual setup | Automatic |
| **Distribution** | Manual scripting | One command |

---

## Advanced Usage

### Running with Custom JVM Options

```bash
# Increase heap size
./gradlew runPlatform -Dorg.gradle.jvmargs=-Xmx4096m

# Enable debug mode
./gradlew runPlatform -Dorg.gradle.debug=true
```

### Building for Specific Java Version

```bash
# Target Java 11 (edit build.gradle first)
./gradlew build -Djava.sourceCompatibility=11 -Djava.targetCompatibility=11
```

### Running Tests with Coverage

```bash
# With Jacoco plugin (need to add to build.gradle)
./gradlew test jacocoTestReport
```

### Clean Specific Outputs

```bash
# Clean build directory
./gradlew clean

# Also clean Gradle cache
./gradlew clean --no-daemon
rm -rf ~/.gradle/caches/
```

---

## Migration from Shell Scripts

**Old way (shell scripts):**
```bash
# In bin/coda/platform:
export CLASSPATH="$CODA/common/jar/*:..."
$JAVA_HOME/bin/java -Xms200m -Xmx2048m org.jlab.coda.afecs.platform.APlatform $1
```

**New way (Gradle):**
```bash
./gradlew runPlatform --args="$1"
```

**Benefits:**
- ✅ No need to set CLASSPATH
- ✅ No need to find Java manually
- ✅ Consistent builds across machines
- ✅ Easier for new developers

**Shell scripts still work!** Gradle is complementary, not a replacement.

---

## Continuous Integration

**Example Jenkins pipeline:**
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Deploy') {
            steps {
                sh './gradlew fatJar'
                archiveArtifacts artifacts: 'build/libs/*.jar'
            }
        }
    }
}
```

---

## References

- **Gradle Docs:** https://docs.gradle.org/
- **Java Plugin Guide:** https://docs.gradle.org/current/userguide/java_plugin.html
- **Application Plugin:** https://docs.gradle.org/current/userguide/application_plugin.html

---

## Support

**Issues with Gradle build?**

1. Check this guide first
2. Run `./gradlew checkDeps` to verify dependencies
3. Check `build/reports/` for detailed error reports
4. Run with `--stacktrace` for more details:
   ```bash
   ./gradlew build --stacktrace
   ```

**For AFECS-specific issues:**
- See main project documentation
- Check original shell scripts in `bin/` for reference

---

*End of Quick Start Guide*
