@echo off
title Build TrekMate
set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.12_windows-x64_bin\jdk-17.0.12"
set "PATH=C:\Program Files\Java\jdk-17.0.12_windows-x64_bin\jdk-17.0.12\bin;C:\Program Files\Apache\Maven\apache-maven-3.9.16\bin;%PATH%"

echo Compiling and packaging TrekMate JAR...
mvn clean package -DskipTests
echo Build complete.
pause