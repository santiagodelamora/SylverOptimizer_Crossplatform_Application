@echo off
cd /d "%~dp0"
if exist "bin\Release\net10.0-windows\WinOptimizerPro.exe" (
    start "" "bin\Release\net10.0-windows\WinOptimizerPro.exe"
) else (
    dotnet run --configuration Release
)
