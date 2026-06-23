# Script de Inicializacion Automatizada - Ecossistema de Microsservicos
Write-Host "Inicializando o Ecossistema de Microsservicos..." -ForegroundColor Cyan

# 1. Abre o Frontend (Node.js) em um novo terminal
Write-Host "Inicializando o Frontend na Porta 3000..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd ./Frontend; npm install; node server.js"

# 2. Abre o UserService (Backend Java 8081)
Write-Host "Inicializando o UserService na Porta 8081..." -ForegroundColor Blue
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd ./UserService/secrets; cmd.exe /c mvnw.cmd spring-boot:run"

# 3. Abre o EmailService (Backend Java 8082)
Write-Host "Inicializando o EmailService na Porta 8082..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd ./EmailService/email; cmd.exe /c mvnw.cmd spring-boot:run"

Write-Host "Todos os servicos foram disparados em terminais isolados!" -ForegroundColor Green