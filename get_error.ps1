$ErrorActionPreference = "Continue"
Write-Host "Running Maven..."
.\mvnw spring-boot:run > boot_log2.txt 2>&1
Write-Host "Maven crashed. Extracting error log..."
Get-Content boot_log2.txt -Encoding Unicode | Select-String -Pattern "Caused by:|Exception:|Field error" -Context 0,5 | Select-Object -First 20
