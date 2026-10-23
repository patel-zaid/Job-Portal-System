#!/bin/bash
# Job Portal System (SQLite) - compile and run on Linux / macOS.
# Needs: JDK 17 or newer. No database server is needed.
# The SQLite database (data/job_portal.db) is created automatically on the first start.
cd "$(dirname "$0")" || exit 1

# 1. JDK check
if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
    echo "ERROR: No JDK found. Install JDK 17 or newer (https://adoptium.net) and try again."
    exit 1
fi
JAVAC_VERSION=$(javac -version 2>&1 | grep '^javac' | awk '{print $2}')
JAVA_MAJOR=${JAVAC_VERSION%%[.-]*}
if [ -z "$JAVA_MAJOR" ] || [ "$JAVA_MAJOR" = "1" ] || [ "$JAVA_MAJOR" -lt 17 ] 2>/dev/null; then
    echo "ERROR: Your JDK is too old or unknown ($JAVAC_VERSION). Install JDK 17 or newer."
    exit 1
fi
echo "Found JDK $JAVAC_VERSION"

# 2. Folders
mkdir -p lib out data

# 3. SQLite JDBC driver: use the bundled jar in lib/, download only if missing
if ! ls lib/sqlite-jdbc-*.jar >/dev/null 2>&1; then
    JDBC_JAR="sqlite-jdbc-3.46.1.3.jar"
    JDBC_URL="https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/$JDBC_JAR"
    echo "SQLite JDBC driver not found in lib/ - downloading $JDBC_JAR ..."
    if command -v curl >/dev/null 2>&1; then
        curl -fL -o "lib/$JDBC_JAR.part" "$JDBC_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "lib/$JDBC_JAR.part" "$JDBC_URL"
    fi
    if [ -s "lib/$JDBC_JAR.part" ]; then
        mv "lib/$JDBC_JAR.part" "lib/$JDBC_JAR"
    else
        rm -f "lib/$JDBC_JAR.part"
        echo "ERROR: Could not download the driver. Download it manually and copy it into lib/ :"
        echo "  $JDBC_URL"
        exit 1
    fi
fi

# 4. Compile
echo "Compiling..."
javac -encoding UTF-8 -cp "lib/*" -d out \
    src/jobportal/*.java \
    src/jobportal/model/*.java \
    src/jobportal/exception/*.java \
    src/jobportal/util/*.java \
    src/jobportal/dao/*.java \
    src/jobportal/service/*.java \
    src/jobportal/ui/*.java || { echo "ERROR: Compilation failed."; exit 1; }

# 5. Run (the database is created and filled automatically on the first start)
echo "Starting Job Portal System..."
java -cp "out:lib/*" jobportal.MainApp
