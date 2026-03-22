$utf8NoBom = New-Object System.Text.UTF8Encoding $False
Get-ChildItem -Path "src\main\java\com\example\coffeshopManagement\entity\*.java" | ForEach-Object {
    $text = [System.IO.File]::ReadAllText($_.FullName)
    [System.IO.File]::WriteAllText($_.FullName, $text, $utf8NoBom)
}
Write-Host "BOM removed"
