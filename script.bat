@echo off

REM Compile les servlets, crée le JAR et nettoie build
set SERVLET_API_JAR=..\test-framework\lib
set APP_NAME=Framework
set SRC_DIR=src
set BUILD_DIR=build
set JAR_FILE=%APP_NAME%.jar

REM Nettoyage
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"

REM Compilation de tous les fichiers .java
echo Compilation de tous les fichiers Java...
setlocal enabledelayedexpansion
set FILES=

for /r "%SRC_DIR%" %%f in (*.java) do (
    set FILES=!FILES! "%%f"
)

javac -cp "%SERVLET_API_JAR%\servlet-api.jar;%SRC_DIR%" -d "%BUILD_DIR%" %FILES%

if errorlevel 1 (
    echo Erreur lors de la compilation des sources
    pause
    exit /b 1
)

REM Vérifier si des fichiers .class ont été créés
dir "%BUILD_DIR%" /b >nul 2>&1
if errorlevel 1 (
    echo Aucun fichier compilé trouvé
    echo Vérifiez le chemin des sources: %SRC_DIR%
    pause
    exit /b 1
)

REM Création du JAR
jar cf "%JAR_FILE%" -C "%BUILD_DIR%" .

REM Suppression du dossier build après création du JAR
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"

REM Copier le JAR généré dans le lib de test-framework-main
copy "%JAR_FILE%" "%SERVLET_API_JAR%"

REM Supprimer le JAR du dossier framework
if exist "%JAR_FILE%" del "%JAR_FILE%"

echo Compilation et création du JAR terminées avec succès.
pause
