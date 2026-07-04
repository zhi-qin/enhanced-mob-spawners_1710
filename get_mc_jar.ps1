[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$wc = New-Object System.Net.WebClient

Write-Output "Step 1: Downloading version manifest..."
$manifest = $wc.DownloadString('https://piston-meta.mojang.com/mc/game/version_manifest.json')
$manifestJson = $manifest | ConvertFrom-Json
$v170 = $manifestJson.versions | Where-Object { $_.id -eq '1.7.10' } | Select-Object -First 1
Write-Output "1.7.10 version URL: $($v170.url)"

Write-Output "Step 2: Downloading 1.7.10 version JSON..."
$versionJson = $wc.DownloadString($v170.url)
$versionJson | Out-File -FilePath "$env:TEMP\1.7.10.json" -Encoding ASCII
$vJson = $versionJson | ConvertFrom-Json
$clientUrl = $vJson.downloads.client.url
$serverUrl = $vJson.downloads.server.url
Write-Output "Client JAR URL: $clientUrl"
Write-Output "Server JAR URL: $serverUrl"

Write-Output "Step 3: Downloading Minecraft 1.7.10 client jar..."
$jarCacheDir = "$env:USERPROFILE\.gradle\caches\minecraft\net\minecraft\minecraft\1.7.10"
New-Item -ItemType Directory -Path $jarCacheDir -Force | Out-Null
$jarPath = "$jarCacheDir\minecraft-1.7.10.jar"
$wc.DownloadFile($clientUrl, $jarPath)
Write-Output "Downloaded $jarPath"

$fileInfo = Get-ChildItem $jarPath
Write-Output "File size: $($fileInfo.Length) bytes"
