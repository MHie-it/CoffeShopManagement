$ErrorActionPreference = "Stop"
$source = "C:\Users\mainh\Desktop\CoffeShopManagement\src\main\java\com\example\model"
$dest = "C:\Users\mainh\Desktop\CoffeShopManagement\src\main\java\com\example\coffeshopManagement\entity"

Write-Host "Moving files..."
Get-ChildItem -Path $source -Filter "*.java" | Where-Object { $_.Name -ne "User.java" -and $_.Name -ne "Role.java" } | Move-Item -Destination $dest -Force

Write-Host "Updating packages..."
Get-ChildItem -Path $dest -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content -replace "package com\.example\.model;", "package com.example.coffeshopManagement.entity;"
    $content = $content -replace "import com\.example\.model\.", "import com.example.coffeshopManagement.entity."
    [System.IO.File]::WriteAllText($_.FullName, $content, [System.Text.Encoding]::UTF8)
}

Write-Host "Cleaning up model folder..."
Remove-Item -Path $source -Recurse -Force
Write-Host "Migration complete."
