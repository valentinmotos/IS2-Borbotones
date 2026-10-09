@echo off
cd /d "%~dp0"
set "MAVEN_OPTS=-Xms32m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m"
set "JAVA_TOOL_OPTIONS=-Xms32m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m"
call mvnw.cmd spring-boot:run
pause
