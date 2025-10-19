@echo off
setlocal enabledelayedexpansion

:: Paramètres
set SRC_DIR=src\sprint2
set BUILD_DIR=build
set PACKAGE_NAME=sprint2
set APP_NAME=framework
set JAR_FILE=%APP_NAME%.jar
set TEST_LIB_DIR=..\test-framework\lib

echo ==========================================
echo   🧩 Compilation, création du JAR et copie
echo ==========================================

:: Nettoyage du dossier build
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"

echo.
echo 🔧 Compilation de tous les fichiers Java...
for /r "%SRC_DIR%" %%f in (*.java) do (
    echo Compilation de %%f
    javac -d "%BUILD_DIR%" "%%f"
    
    if errorlevel 1 (
        echo ❌ Erreur lors de la compilation de %%f
        pause
        exit /b 1
    )
)

:: Vérifier si des fichiers .class ont été créés
dir "%BUILD_DIR%" /b >nul 2>&1
if errorlevel 1 (
    echo ❌ Aucun fichier compilé trouvé
    echo Vérifiez le chemin des sources: %SRC_DIR%
    pause
    exit /b 1
)

echo 🔧 Création du JAR...
jar cf "%JAR_FILE%" -C "%BUILD_DIR%" .

:: Suppression du dossier build après création du JAR
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"

echo 🔧 Copie du JAR vers le dossier lib de test-framework...
if not exist "%TEST_LIB_DIR%" mkdir "%TEST_LIB_DIR%"
copy "%JAR_FILE%" "%TEST_LIB_DIR%"

:: Supprimer le JAR du dossier framework
if exist "%JAR_FILE%" del "%JAR_FILE%"

echo ✅ JAR généré et copié avec succès !
pause
