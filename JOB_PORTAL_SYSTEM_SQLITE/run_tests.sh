#!/bin/bash
# Compiles the project and runs the automatic tests (temporary databases; your data/job_portal.db is not touched).
cd "$(dirname "$0")" || exit 1
mkdir -p out_test
FAILED=0
CP="out_test:lib/*"

echo "=== Compiling ==="
javac -encoding UTF-8 -cp "lib/*" -d out_test $(find src test -name '*.java') || { echo "ERROR: Compilation failed."; exit 1; }

echo; echo "=== 1. Database, DAO, REST API tests ==="
java -cp "$CP" jobportal.JobPortalTests || FAILED=1

echo; echo "=== 2. Persistence test (two separate program runs) ==="
PERSIST_URL="jdbc:sqlite:out_test/persist_test.db"
rm -f out_test/persist_test.db
java -Djobportal.db.url=$PERSIST_URL -cp "$CP" jobportal.JobPortalTests persist-write || FAILED=1
java -Djobportal.db.url=$PERSIST_URL -cp "$CP" jobportal.JobPortalTests persist-verify || FAILED=1
rm -f out_test/persist_test.db

echo; echo "=== 3. Screen test ==="
if [ -z "$DISPLAY" ] && command -v xvfb-run >/dev/null 2>&1; then
    xvfb-run -a java -cp "$CP" jobportal.UiSmokeTest || FAILED=1
else
    java -cp "$CP" jobportal.UiSmokeTest || FAILED=1
fi

echo
if [ $FAILED -eq 0 ]; then echo "ALL TESTS PASSED"; else echo "SOME TESTS FAILED - read the FAIL lines above"; fi
exit $FAILED
