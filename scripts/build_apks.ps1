param (
    [string]$BuildType = "debug"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  DoramaTV - APK Build & Export Pipeline  " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$RootDir = Split-Path -Parent $PSScriptRoot
$AndroidDir = Join-Path $RootDir "android-app"
$ApkOutputDir = Join-Path $RootDir "apks"

if (-not (Test-Path $ApkOutputDir)) {
    New-Item -ItemType Directory -Path $ApkOutputDir -Force | Out-Null
}

$TaskName = if ($BuildType -eq "release") { "assembleRelease" } else { "assembleDebug" }
$ApkSuffix = if ($BuildType -eq "release") { "release" } else { "debug" }

Write-Host "`n[1/3] Compiling targets (:app and :app-mobile)..." -ForegroundColor Yellow
Push-Location $AndroidDir
try {
    & .\gradlew.bat ":app:$TaskName" ":app-mobile:$TaskName"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Gradle build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

Write-Host "`n[2/3] Locating built APKs..." -ForegroundColor Yellow

$TvSource = Join-Path $AndroidDir "app\build\outputs\apk\$BuildType\app-$ApkSuffix.apk"
$MobileSource = Join-Path $AndroidDir "app-mobile\build\outputs\apk\$BuildType\app-mobile-$ApkSuffix.apk"

$TvDest = Join-Path $ApkOutputDir "DoramaTV-AndroidTV-v1.0.0.apk"
$MobileDest = Join-Path $ApkOutputDir "DoramaTV-Mobile-v1.0.0.apk"

if (Test-Path $TvSource) {
    Copy-Item -Path $TvSource -Destination $TvDest -Force
    $tvSize = [math]::Round((Get-Item $TvDest).Length / 1MB, 2)
    Write-Host " -> Exported TV APK: $TvDest ($tvSize MB)" -ForegroundColor Green
} else {
    Write-Warning "TV APK not found at $TvSource"
}

if (Test-Path $MobileSource) {
    Copy-Item -Path $MobileSource -Destination $MobileDest -Force
    $mobileSize = [math]::Round((Get-Item $MobileDest).Length / 1MB, 2)
    Write-Host " -> Exported Mobile APK: $MobileDest ($mobileSize MB)" -ForegroundColor Green
} else {
    Write-Warning "Mobile APK not found at $MobileSource"
}

Write-Host "`n[3/3] Build pipeline complete!" -ForegroundColor Cyan
Write-Host "Check the 'apks/' directory for your ready-to-install binaries.`n" -ForegroundColor Cyan
