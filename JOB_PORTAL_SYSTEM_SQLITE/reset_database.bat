@echo off
rem Deletes the SQLite database so that a NEW one (with the original sample data)
rem is created the next time the program starts. Close the program first!
setlocal EnableExtensions
cd /d "%~dp0"
echo This will DELETE all data (users, jobs, applications ...) in data\job_portal.db
echo and restore the original sample data at the next start.
echo Make sure the Job Portal window is closed.
set /p ANSWER=Type YES to continue: 
if /i not "%ANSWER%"=="YES" goto :cancel
if exist data\job_portal.db del /q data\job_portal.db
if exist data\job_portal.db-journal del /q data\job_portal.db-journal
if exist data\job_portal.db-wal del /q data\job_portal.db-wal
if exist data\job_portal.db-shm del /q data\job_portal.db-shm
if exist data\job_portal.db (
    echo ERROR: The database file could not be deleted. Is the program still running?
    pause
    exit /b 1
)
echo Done. Start the program with compile_and_run.bat to create a fresh database.
pause
exit /b 0
:cancel
echo Cancelled - nothing was deleted.
pause
