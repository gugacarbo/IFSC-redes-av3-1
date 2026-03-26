param(
    [string]$MavenWrapper = ".\mvnw.cmd"
)

$ErrorActionPreference = 'Stop'

$jarName = 'udp_chat-1.0-SNAPSHOT-shaded.jar'
$targetDir = Join-Path -Path (Get-Location) -ChildPath 'target'
$outputPath = Join-Path -Path $targetDir -ChildPath $jarName

Write-Host "Gerando executavel UDP Chat..."
Write-Host "Executando $MavenWrapper clean package"
& $MavenWrapper clean package

if (-not (Test-Path -Path $outputPath)) {
    Write-Error "O artefato sombreado nao foi encontrado em $outputPath."
    exit 1
}

Write-Host "Executavel pronto: $outputPath"
