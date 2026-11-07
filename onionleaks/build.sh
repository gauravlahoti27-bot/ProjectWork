#!/usr/bin/env bash
# Build OnionLeaks with nothing but the JDK (21+). No Maven, no Gradle, no downloads.
#   ./build.sh          compile + package build/onionleaks.jar
#   ./build.sh test     also compile and run the test suite
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build && mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' > build/sources.txt
javac -Xlint:all -Werror --release 21 -d build/classes @build/sources.txt
cp -r src/main/resources/. build/classes/
jar --create --file build/onionleaks.jar --main-class com.onionleaks.Portal -C build/classes .
echo "built build/onionleaks.jar"

if [[ "${1:-}" == "test" ]]; then
  find src/test/java -name '*.java' > build/test-sources.txt
  javac --release 21 -cp build/classes -d build/test-classes @build/test-sources.txt
  java -ea -cp build/classes:build/test-classes com.onionleaks.AllTests
fi
