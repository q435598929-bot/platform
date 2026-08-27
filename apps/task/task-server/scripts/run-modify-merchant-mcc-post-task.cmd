@echo off
setlocal

set "ROOT=%~dp0.."
set "JDK=D:\dev\env\jdk\jdk-21.0.10\bin"
set "CLASSES=%ROOT%\target\classes"
set "SOURCE=%ROOT%\src\main\java\com\lab\taskexecutor\controller\quhulian\ModifyMerchantMccPostTask.java"
set "MAIN_CLASS=com.platform.task.controller.quhulian.ModifyMerchantMccPostTask"

if not exist "%CLASSES%" mkdir "%CLASSES%"

"%JDK%\javac.exe" -encoding UTF-8 -d "%CLASSES%" "%SOURCE%"
if errorlevel 1 exit /b %errorlevel%

"%JDK%\java.exe" -Dfile.encoding=UTF-8 -cp "%CLASSES%" %MAIN_CLASS% %*
