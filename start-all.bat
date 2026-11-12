@echo off
rem =====================================================================
rem  CaiShen project one-click starter
rem  Starts: backend (8080), user front (5173), admin front (5174)
rem
rem  AI keys are NOT stored in the database or in source code.
rem  They are injected from ai-keys.bat (local only, never commit / ship it).
rem  First run: copy ai-keys.example.bat to ai-keys.bat and fill in the keys.
rem =====================================================================

setlocal
set ROOT=%~dp0

rem ---- Resolve a Java 21+ runtime -------------------------------------
rem The backend jar targets Java 21. Prefer JAVA_HOME, then the Oracle
rem javapath shim, then a local JDK 21 install.
set "JAVA_BIN=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if exist "%ROOT%jdk-21\bin\java.exe" set "JAVA_BIN=%ROOT%jdk-21\bin\java.exe"
if not defined JAVA_HOME if exist "C:\Program Files\Common Files\Oracle\Java\javapath\java.exe" set "JAVA_BIN=C:\Program Files\Common Files\Oracle\Java\javapath\java.exe"

rem ---- Load local AI keys --------------------------------------------
if exist "%ROOT%ai-keys.bat" (
  call "%ROOT%ai-keys.bat"
  if defined AI_API_KEY (
    echo [AI] keys loaded from ai-keys.bat
  ) else (
    echo [AI] WARNING: ai-keys.bat found but AI_API_KEY is empty
    echo [AI]          text AI ^(ranking / creation^) stays disabled
  )
  if defined AI_VISION_API_KEY (
    echo [AI] vision key loaded - photo recognition enabled
  ) else (
    echo [AI] WARNING: AI_VISION_API_KEY is empty
    echo [AI]          photo recognition stays disabled unless it reuses AI_API_KEY
  )
  echo [AI] text model   : %AI_MODEL%
  echo [AI] vision model : %AI_VISION_MODEL%
) else (
  echo [AI] ai-keys.bat NOT found - AI features will be disabled.
  echo [AI] copy ai-keys.example.bat to ai-keys.bat and fill in your keys.
)
echo.

echo [1/3] Starting backend on port 8080 ...
start "CaiShen-Backend" cmd /k "cd /d ""%ROOT%backend"" && ""%JAVA_BIN%"" -jar target\foodlab-1.0.0.jar --server.port=8080"

echo [2/3] Starting user frontend on port 5173 ...
start "CaiShen-Front" cmd /k "cd /d ""%ROOT%front"" && npm run dev"

echo [3/3] Starting admin frontend on port 5174 ...
start "CaiShen-Admin" cmd /k "cd /d ""%ROOT%admin-web"" && npm run dev"

echo.
echo All services launching in separate windows.
echo   Backend  : http://localhost:8080  (wait ~15s for Spring Boot)
echo   Front    : http://localhost:5173
echo   Admin    : http://localhost:5174
echo.
echo AI readiness: open http://localhost:5174 - System Config - Test AI Connection
echo Close the spawned windows to stop the services.
pause
