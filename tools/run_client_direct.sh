#!/usr/bin/env bash
# Runs the scripted client smoke test with a chosen java executable, bypassing Gradle's JavaExec.
#
# Why: on a machine without a GPU the client needs a software OpenGL (Mesa llvmpipe). On Windows
# the opengl32.dll has to sit next to java.exe, so a private JDK copy is used, and Gradle will not
# launch its run task with a JDK other than its toolchain's.
#
#   tools/run_client_direct.sh <path/to/java.exe> <smoke script name> [timeout seconds]
#   tools/run_client_direct.sh "$LOCALAPPDATA/Programs/jdk-21-mesa/bin/java.exe" m4_tour
#
# Gradle still prepares everything (classes, argument files); only the final launch is ours.
set -u
cd "$(dirname "$0")/.." || exit 1
JAVA="$1"
SCRIPT="$2"
LIMIT="${3:-900}"

./gradlew writeClientSmokeClasspath -Psmoke="$SCRIPT" --console=plain -q || exit 1

ROOT_W="$(cygpath -m "$(pwd)")"
mkdir -p run-smoke
cd run-smoke || exit 1
# Software rendering is slow: skip the first-launch onboarding screen and turn off the expensive
# effects (menu blur, fancy graphics, clouds, long render distance). Written once; edit freely.
if [ ! -f options.txt ]; then
    cat > options.txt <<'OPTIONS'
onboardAccessibility:false
menuBackgroundBlurriness:0
graphicsMode:0
renderDistance:4
simulationDistance:5
renderClouds:"false"
particles:1
maxFps:60
enableVsync:false
ao:false
entityShadows:false
biomeBlendRadius:0
mipmapLevels:0
narrator:0
tutorialStep:none
skipMultiplayerWarning:true
joinedFirstServer:true
pauseOnLostFocus:false
OPTIONS
fi
# llvmpipe: use all cores; ask for a 4.5 core context like a real driver would give.
export GALLIUM_DRIVER=llvmpipe
export LP_NUM_THREADS="${LP_NUM_THREADS:-8}"
timeout "$LIMIT" "$JAVA" "@$ROOT_W/build/moddev/clientSmokeRunVmArgs.txt" \
    -Dgolemancy.smoke="$ROOT_W/tools/smoke/$SCRIPT.txt" \
    "@$ROOT_W/build/moddev/clientSmokeClasspathArg.txt" net.neoforged.devlaunch.Main \
    "@$ROOT_W/build/moddev/clientSmokeRunProgramArgs.txt"
