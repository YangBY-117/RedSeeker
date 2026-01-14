# fix-user.md

## What changed
- Replaced the local BCrypt implementation with Spring Security's BCrypt encoder to prevent runtime errors during hashing.
- Updated user registration to retrieve the new user ID via `SELECT last_insert_rowid()` for SQLite.
- Added a frontend registration UI test script using Playwright.

## Files touched
- backend/pom.xml
- backend/src/main/java/com/redseeker/user/UserServiceImpl.java
- frontend/package.json
- frontend/package-lock.json
- scripts/test-register.js

## How to run the registration test
1) Start the backend on port 8081 (example: `mvn -DskipTests -Dspring-boot.run.arguments=--server.port=8081 spring-boot:run`).
2) Start the frontend on port 5173 with `VITE_API_BASE_URL=http://localhost:8081/api`.
3) Run the test script:
   - `node scripts/test-register.js`

Notes:
- The script prints JSON with the generated username and any error text from the UI.
- You can override the frontend URL with `FRONTEND_URL`.
