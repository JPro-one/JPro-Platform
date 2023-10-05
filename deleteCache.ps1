Get-ChildItem -Path $env:USERPROFILE\.m2, $env:USERPROFILE\.gradle -Recurse |
        Where-Object { $_.FullName -match 'one.jpro.platform' } |
        ForEach-Object { Remove-Item -Path $_.FullName -Recurse -Force }
