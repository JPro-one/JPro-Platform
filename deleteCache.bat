CHCP 65001
@echo off
setLocal

SET DIR=%~dp0
cd /D "%DIR%"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File deleteCache.ps1 %*

endLocal