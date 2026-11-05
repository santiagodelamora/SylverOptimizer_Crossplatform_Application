@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [1/2] Publicando aplicación autocontenida (Single-File Win-x64)...
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -p:EnableCompressionInSingleFile=true -o publish
if %ERRORLEVEL% NEQ 0 (
    echo Error al publicar el proyecto .NET.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Compilando instalador con Inno Setup 6...
if exist "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" (
    "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" "Installer\WinOptimizerPro.iss"
) else if exist "%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe" (
    "%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe" "Installer\WinOptimizerPro.iss"
) else (
    echo No se encontró Inno Setup 6. El ejecutable portable está listo en la carpeta "publish\WinOptimizerPro.exe".
    pause
    exit /b 1
)

echo.
echo Instalador generado correctamente en: Installer\Output\WinOptimizerPro_Setup_v1.0.0.exe
pause
