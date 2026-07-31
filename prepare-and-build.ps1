$ErrorActionPreference = "Stop"

$GradleVersion = "8.1.1"
$TempRoot = Join-Path $env:TEMP "civicnations-gradle-$GradleVersion"
$ZipPath = Join-Path $TempRoot "gradle-$GradleVersion-bin.zip"
$GradleHome = Join-Path $TempRoot "gradle-$GradleVersion"
$GradleBat = Join-Path $GradleHome "bin\gradle.bat"

New-Item -ItemType Directory -Force -Path $TempRoot | Out-Null

if (-not (Test-Path $GradleBat)) {
    Write-Host "Downloading Gradle $GradleVersion..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $ZipPath
    Expand-Archive -Path $ZipPath -DestinationPath $TempRoot -Force
}

Write-Host "Building Civic Nations for Minecraft 1.20.1 / NeoForge 47.x..."
& $GradleBat --no-daemon build
$BuildExitCode = $LASTEXITCODE

if ($BuildExitCode -ne 0) {
    Write-Host "Build failed with exit code $BuildExitCode." -ForegroundColor Red
    exit $BuildExitCode
}

Write-Host "Build complete." -ForegroundColor Green
Write-Host "JAR: build\libs\civicnations-neoforge-1.20.1.jar" -ForegroundColor Green
exit 0
