@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.maven
set MVN=%MAVEN_HOME%\bin\mvn.cmd

if not exist "%MVN%" (
    echo Installing Maven...
    powershell -Command "[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile '%MAVEN_PROJECTBASEDIR%maven.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_PROJECTBASEDIR%maven.zip' -DestinationPath '%MAVEN_PROJECTBASEDIR%' -Force"
    for /d %%i in ("%MAVEN_PROJECTBASEDIR%apache-maven-*") do ren "%%i" ".maven"
    del "%MAVEN_PROJECTBASEDIR%maven.zip"
)

"%MVN%" %*
