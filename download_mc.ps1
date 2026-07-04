param([string]$url, [string]$out)
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$wc = New-Object System.Net.WebClient
$wc.DownloadFile($url, $out)
Write-Output "Downloaded to $out"
