# Sube el proyecto MesaTech a GitHub SIN llaves .pem ni archivos .env
# Ejecutar en PowerShell dentro de C:\Users\copra\OneDrive\Escritorio\mesatech-ep2
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path .git)) { git init | Out-Null }
git add .

$peligrosos = git ls-files --cached | Select-String -Pattern '\.pem$|\.key$|(^|/)\.env$'
if ($peligrosos) {
    Write-Host "DETENIDO: estos archivos sensibles estan en el indice:" -ForegroundColor Red
    $peligrosos
    git reset | Out-Null
    exit 1
}

Write-Host "OK: ninguna llave ni .env sera subido. Archivos a subir:" -ForegroundColor Green
git ls-files --cached | Measure-Object | ForEach-Object { "  $($_.Count) archivos" }

git commit -m "MesaTech EP2: microservicios en EC2, RabbitMQ con Docker Compose, RDS y lanzador local con Entra ID"
git branch -M main
if (-not (git remote | Select-String origin)) {
    git remote add origin https://github.com/MarGuerra-ux/MesaTech-RabbitMQ.git
}
git push -u origin main
