@echo off
rem Compiles the project and runs the automatic tests (they use temporary databases,
rem your real data\job_portal.db is NOT touched).
setlocal EnableExtensions
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (
    echo ERROR: No JDK found. Install JDK 17 or newer.
    pause
    exit /b 1
)
if not exist out_test mkdir out_test
set "FAILED=0"

echo === Compiling ===
javac -encoding UTF-8 -cp "lib\*" -d out_test src\jobportal\*.java src\jobportal\model\*.java src\jobportal\exception\*.java src\jobportal\util\*.java src\jobportal\dao\*.java src\jobportal\service\*.java src\jobportal\ui\*.java test\jobportal\*.java
if errorlevel 1 (
    echo ERROR: Compilation failed.
    pause
    exit /b 1
)

echo.
echo === 1. Database, DAO, REST API tests ===
java -cp "out_test;lib\*" jobportal.JobPortalTests
if errorlevel 1 set "FAILED=1"

echo.
echo === 2. Persistence test (two separate program runs) ===
set "PERSIST_URL=jdbc:sqlite:out_test/persist_test.db"
if exist out_test\persist_test.db del /q out_test\persist_test.db
java -Djobportal.db.url=%PERSIST_URL% -cp "out_test;lib\*" jobportal.JobPortalTests persist-write
if errorlevel 1 set "FAILED=1"
java -Djobportal.db.url=%PERSIST_URL% -cp "out_test;lib\*" jobportal.JobPortalTests persist-verify
if errorlevel 1 set "FAILED=1"
if exist out_test\persist_test.db del /q out_test\persist_test.db

echo.
echo === 3. Screen test (opens and clicks through the real windows - do not touch the mouse) ===
java -cp "out_test;lib\*" jobportal.UiSmokeTest
if errorlevel 1 set "FAILED=1"

echo.
if "%FAILED%"=="0" (echo ALL TESTS PASSED) else (echo SOME TESTS FAILED - read the FAIL lines above)
pause
exit /b %FAILED%
