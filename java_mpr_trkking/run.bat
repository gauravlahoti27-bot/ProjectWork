@echo off
title TrekMate Web Application
set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.12_windows-x64_bin\jdk-17.0.12"
set "PATH=C:\Program Files\Java\jdk-17.0.12_windows-x64_bin\jdk-17.0.12\bin;C:\Program Files\Apache\Maven\apache-maven-3.9.16\bin;%PATH%"

echo ==============================================================
echo              TrekMate - 2nd Year Mini Project
echo ==============================================================
echo Launching Spring Boot server on http://localhost:8080 ...
echo Demo Trekker: trekker@example.com / pass123
echo Demo Host:    host@example.com / admin123
echo ==============================================================

mvn spring-boot:run
pause