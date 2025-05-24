@echo off
setlocal EnableDelayedExpansion

echo Chargement des variables d'environnement depuis .env...
for /f "tokens=1,* delims==" %%a in (.env) do (
    set "%%a=%%b"
)

echo Configuration chargee avec succes
echo.

REM Vérifier si le conteneur Redis existe et est en cours d'exécution
powershell -Command "$running = docker ps --filter 'name=redis' --format '{{.Names}}'; $exists = docker ps -a --filter 'name=redis' --format '{{.Names}}'; if($running) { Write-Host 'Redis est en cours d''execution' } elseif($exists) { Write-Host 'Redis existe mais n''est pas en cours d''execution'; exit 1 } else { Write-Host 'Redis n''existe pas'; exit 2 }"

if !errorlevel! == 1 (
    echo Redemarrage du conteneur Redis existant...
    powershell -Command "docker start redis"
    if !errorlevel! == 0 (
        echo Conteneur Redis redemarre avec succes
        timeout /t 5 /nobreak > nul
    ) else (
        echo Erreur lors du redemarrage de Redis
        pause
        exit /b 1
    )
) else if !errorlevel! == 2 (
    echo Creation d'un nouveau conteneur Redis...
    powershell -Command "docker run --name redis -d -p 6379:6379 redis:latest"
    if !errorlevel! == 0 (
        echo Nouveau conteneur Redis cree avec succes
        timeout /t 5 /nobreak > nul
    ) else (
        echo Erreur lors de la creation du conteneur Redis
        pause
        exit /b 1
    )
)
@REM     )
@REM )

@REM REM Vérifier si Keycloak est en cours d'exécution
@REM powershell -Command "if ((Get-Process kc.bat -ErrorAction SilentlyContinue) -eq $null) { Write-Host 'Keycloak n''est pas en cours d''execution' } else { Write-Host 'Keycloak est en cours d''execution' }"

@REM if not !errorlevel! == 0 (
@REM     echo Demarrage de Keycloak...
@REM     start "Keycloak Server" /d "%KEYCLOAK_HOME%\bin" kc.bat start-dev
@REM     timeout /t 10 /nobreak > nul
@REM )

echo.
echo Nettoyage et compilation du projet...
call mvn clean install

if !errorlevel! neq 0 (
    echo Erreur lors de la compilation!
    pause
    exit /b !errorlevel!
)

echo.
echo Verification des services avant le demarrage...
echo.

REM Vérifier une dernière fois que tous les services sont en cours d'exécution
powershell -Command "Write-Host 'Redis: '; $redis = docker ps --filter 'name=redis' --format '{{.Names}}'; if($redis -eq $null) { Write-Host 'NON' -ForegroundColor Red } else { Write-Host 'OK' -ForegroundColor Green }"
@REM @REM powershell -Command "Write-Host 'Keycloak: '; if ((Get-Process kc.bat -ErrorAction SilentlyContinue) -eq $null) { Write-Host 'NON' -ForegroundColor Red } else { Write-Host 'OK' -ForegroundColor Green }"

echo.
echo Demarrage de l'application API Gateway...
echo.

set JAVA_OPTS=-Xmx512m -Dspring.profiles.active=dev
call mvn spring-boot:run

pause
