#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building BBSmartBridge for BlackBerry Curve 9300 (OS 5.0) ==="

# Ensure directories
mkdir -p build/classes build/output

# Clean previous build artifacts
rm -rf build/classes/* build/classes.jar build/preverified.jar
rm -f BBSmartBridge.cod BBSmartBridge.jad BBSmartBridge.cso BBSmartBridge.csl BBSmartBridge.debug BBSmartBridge.jar

NET_RIM_API="$SCRIPT_DIR/jde5_lib/net_rim_api.jar"
RAPC_JAR="$SCRIPT_DIR/jde5_lib/rapc.jar"

# Prefer Java 8 for CLDC 1.1 / Java 1.3 target compliance
JAVAC_CMD="javac"
JAVA_CMD="java"
if [ -x "/usr/lib/jvm/java-8-openjdk-amd64/bin/javac" ]; then
    JAVAC_CMD="/usr/lib/jvm/java-8-openjdk-amd64/bin/javac"
    JAVA_CMD="/usr/lib/jvm/java-8-openjdk-amd64/bin/java"
fi

if [ ! -f "$NET_RIM_API" ]; then
    echo "Error: $NET_RIM_API not found!"
    exit 1
fi

echo "[1/4] Compiling Java sources for CLDC 1.1 / Java 1.3 compliance..."
"$JAVAC_CMD" -source 1.3 -target 1.3 -bootclasspath "$NET_RIM_API" -d build/classes $(find src -name "*.java")

echo "[2/4] Packaging compiled classes..."
python3 -c "
import zipfile, os
z = zipfile.ZipFile('build/classes.jar', 'w', zipfile.ZIP_DEFLATED)
for root, _, files in os.walk('build/classes'):
    for f in files:
        full_path = os.path.join(root, f)
        arcname = os.path.relpath(full_path, 'build/classes')
        z.write(full_path, arcname)
if os.path.exists('icon.png'):
    z.write('icon.png', 'icon.png')
    z.write('icon.png', 'res/icon.png')
z.close()
"

echo "[3/4] Preverifying bytecode for BlackBerry OS 5.0 CLDC 1.1 runtime..."
proguard -microedition -dontshrink -dontoptimize -dontobfuscate \
    -injars build/classes.jar \
    -outjars build/preverified.jar \
    -libraryjars "$NET_RIM_API" > /dev/null 2>&1

echo "[4/4] Invoking BlackBerry RAPC compiler to produce COD..."
"$JAVA_CMD" -jar "$RAPC_JAR" "import=$NET_RIM_API" codename=BBSmartBridge BBSmartBridge.rapc build/preverified.jar

cat << 'EOF' > BBSmartBridge.alx
<loader version="1.0">
    <application id="BBSmartBridge">
        <name>SmartBridge</name>
        <description>Android Bluetooth Companion for BlackBerry Curve 9300 with HFP Audio Routing</description>
        <version>1.4.0</version>
        <vendor>Hamza</vendor>
        <copyright>Copyright (c) 2024 Hamza</copyright>
        <fileset Java="1.54" _blackberryVersion="[5.0.0)">
            <directory></directory>
            <files>BBSmartBridge.cod</files>
        </fileset>
    </application>
</loader>
EOF

echo "=== Build Successful ==="
ls -lh BBSmartBridge.cod BBSmartBridge.jad BBSmartBridge.alx
