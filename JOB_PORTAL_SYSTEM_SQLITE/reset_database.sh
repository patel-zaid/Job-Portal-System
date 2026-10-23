#!/bin/bash
# Deletes the SQLite database so a NEW one (with the original sample data) is created at the next start.
# Close the program first!
cd "$(dirname "$0")" || exit 1
echo "This will DELETE all data in data/job_portal.db (the original sample data comes back at the next start)."
read -r -p "Type YES to continue: " ANSWER
if [ "$ANSWER" != "YES" ]; then echo "Cancelled - nothing was deleted."; exit 0; fi
rm -f data/job_portal.db data/job_portal.db-journal data/job_portal.db-wal data/job_portal.db-shm
echo "Done. Start the program with ./compile_and_run.sh to create a fresh database."
