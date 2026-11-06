@echo off
cd /d "%~dp0"
if exist "bin\Release\net10.0-windows\SylverOptimizer.exe" (
    start "" "bin\Release\net10.0-windows\SylverOptimizer.exe"
) else (
    dotnet run --configuration Release
)
