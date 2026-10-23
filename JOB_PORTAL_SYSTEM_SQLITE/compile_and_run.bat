@echo off
rem ============================================================================
rem  Job Portal System (SQLite) - compile and run on Windows
rem
rem  Needs : JDK 17 or newer (javac + java). Nothing else: no database server.
rem  Does  : checks the JDK, creates folders, finds (or downloads) the SQLite JDBC
rem          driver, compiles every .java file and starts the application.
rem          The SQLite database (data\job_portal.db) is created automatically.
rem ============================================================================
setlocal EnableExtensions
cd /d "%~dp0"
title Job Portal System

rem ---- 1. Is a compatible JDK installed? ---------------------------------------
where javac >nul 2>&1
if errorlevel 1 goto :nojdk
where java >nul 2>&1
if errorlevel 1 goto :nojdk
set "JAVAC_VERSION="
for /f "tokens=2" %%v in ('javac -version 2^>^&1 ^| findstr /b /c:"javac"') do set "JAVAC_VERSION=%%v"
if not defined JAVAC_VERSION goto :nojdk
for /f "tokens=1 delims=.-" %%m in ("%JAVAC_VERSION%") do set "JAVA_MAJOR=%%m"
if "%JAVA_MAJOR%"=="1" goto :oldjdk
if %JAVA_MAJOR% LSS 17 goto :oldjdk
echo Found JDK %JAVAC_VERSION%

rem ---- 2. Create the folders -----------------------------------------------------
if not exist lib mkdir lib
if not exist out mkdir out
if not exist data mkdir data

rem ---- 3. SQLite JDBC driver: use the bundled jar in lib, download only if missing -
set "HAVE_DRIVER="
for %%f in (lib\sqlite-jdbc-*.jar) do set "HAVE_DRIVER=%%f"
if defined HAVE_DRIVER goto :driver_ok

set "JDBC_JAR=sqlite-jdbc-3.46.1.3.jar"
set "JDBC_URL=https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/%JDBC_JAR%"
echo The SQLite JDBC driver was not found in the lib folder - downloading %JDBC_JAR% ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri '%JDBC_URL%' -OutFile 'lib\%JDBC_JAR%.part'"
if not exist "lib\%JDBC_JAR%.part" goto :nodriver
move /y "lib\%JDBC_JAR%.part" "lib\%JDBC_JAR%" >nul
for %%f in (lib\sqlite-jdbc-*.jar) do set "HAVE_DRIVER=%%f"
if not defined HAVE_DRIVER goto :nodriver
:driver_ok
echo Using SQLite JDBC driver: %HAVE_DRIVER%

rem ---- 4. Compile all Java files ---------------------------------------------------
echo Compiling...
javac -encoding UTF-8 -cp "lib\*" -d out src\jobportal\*.java src\jobportal\model\*.java src\jobportal\exception\*.java src\jobportal\util\*.java src\jobportal\dao\*.java src\jobportal\service\*.java src\jobportal\ui\*.java
if errorlevel 1 goto :compilefail

rem ---- 5. Start (the database is created and filled automatically on the first start)
echo Starting Job Portal System...
java -cp "out;lib\*" jobportal.MainApp
if errorlevel 1 goto :runfail
goto :end

:nojdk
echo.
echo ERROR: No JDK found. Please install a JDK (version 17 or newer), for example
echo        Eclipse Temurin from https://adoptium.net  ^(tick "Add to PATH" in the installer^),
echo        then close this window, open a new one and run this file again.
echo        Check with:  javac -version
goto :fail

:oldjdk
echo.
echo ERROR: Your JDK is too old (found %JAVAC_VERSION%). Please install JDK 17 or newer.
goto :fail

:nodriver
echo.
echo ERROR: The SQLite JDBC driver could not be downloaded (no internet connection?).
echo        Download this file in a browser and copy it into the "lib" folder of the project:
echo        %JDBC_URL%
if exist "lib\%JDBC_JAR%.part" del "lib\%JDBC_JAR%.part"
goto :fail

:compilefail
echo.
echo ERROR: Compilation failed. Read the error messages above.
goto :fail

:runfail
echo.
echo ERROR: The program ended with an error. Read the messages above.
goto :fail

:fail
pause
exit /b 1

:end
pause
