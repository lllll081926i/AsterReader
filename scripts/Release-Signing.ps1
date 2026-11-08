param(
    [ValidateSet('Initialize', 'Info', 'Build', 'SyncGitHub')]
    [string]$Action = 'Info',
    [string]$Repository = 'lllll081926i/AsterReader',
    [string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'AsterReader/Signing')
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if (-not $IsWindows) { throw '本地签名工具需要 Windows；GitHub 使用独立工作流。' }
$projectDirectory = Split-Path $PSScriptRoot -Parent
$configPath = Join-Path $SigningDirectory 'release-signing.json'
$entropy = [Text.Encoding]::UTF8.GetBytes('AsterReader.release-signing.v1')

function Find-JavaTool([string]$Name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin/$Name.exe"
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    return (Get-Command "$Name.exe" -ErrorAction Stop).Source
}

function Protect-SigningDirectory {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $acl = [Security.AccessControl.DirectorySecurity]::new()
    $acl.SetOwner($identity.User)
    $acl.SetAccessRuleProtection($true, $false)
    $inherit = [Security.AccessControl.InheritanceFlags]'ContainerInherit, ObjectInherit'
    foreach ($sid in @($identity.User, [Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
        $rule = [Security.AccessControl.FileSystemAccessRule]::new($sid, 'FullControl', $inherit, 'None', 'Allow')
        $acl.AddAccessRule($rule)
    }
    Set-Acl -LiteralPath $SigningDirectory -AclObject $acl
}

function Read-SigningConfiguration {
    if (-not (Test-Path -LiteralPath $configPath)) { throw '尚未初始化签名；先运行 -Action Initialize。' }
    return Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
}

function Read-Password($Configuration) {
    $plain = [Security.Cryptography.ProtectedData]::Unprotect(
        [Convert]::FromBase64String($Configuration.protectedPassword), $entropy,
        [Security.Cryptography.DataProtectionScope]::CurrentUser)
    try { return [Text.Encoding]::UTF8.GetString($plain) }
    finally { [Array]::Clear($plain, 0, $plain.Length) }
}

function Set-GitHubSecret([string]$Name, [string]$Value) {
    $start = [Diagnostics.ProcessStartInfo]::new((Get-Command gh -ErrorAction Stop).Source)
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    foreach ($argument in @('secret', 'set', $Name, '--repo', $Repository)) { $start.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::Start($start)
    try {
        $process.StandardInput.Write($Value)
        $process.StandardInput.Close()
        $outputText = $process.StandardOutput.ReadToEnd()
        $errorText = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw "GitHub Secret 写入失败：$Name；$errorText$outputText" }
        Write-Output "已注入 GitHub Secret：$Name"
    } finally { $process.Dispose() }
}

if ($Action -eq 'Initialize') {
    $keyPath = Join-Path $SigningDirectory 'aster-reader-release.p12'
    if ((Test-Path -LiteralPath $configPath) -or (Test-Path -LiteralPath $keyPath)) {
        throw '签名已经存在；禁止覆盖长期密钥。使用 Info 检查。'
    }
    $keytool = Find-JavaTool 'keytool'
    New-Item -ItemType Directory -Path $SigningDirectory -Force | Out-Null
    Protect-SigningDirectory
    $random = [byte[]]::new(48)
    [Security.Cryptography.RandomNumberGenerator]::Fill($random)
    $password = [Convert]::ToBase64String($random)
    [Array]::Clear($random, 0, $random.Length)
    $previousPassword = $env:ASTER_RELEASE_GENERATION_PASSWORD
    try {
        $env:ASTER_RELEASE_GENERATION_PASSWORD = $password
        & $keytool -genkeypair -keystore $keyPath -storetype PKCS12 -alias upload `
            -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -validity 10000 `
            -dname 'CN=Aster Reader' -storepass:env ASTER_RELEASE_GENERATION_PASSWORD `
            -keypass:env ASTER_RELEASE_GENERATION_PASSWORD -noprompt
        if ($LASTEXITCODE -ne 0) { throw '生成签名密钥失败；保留现场，禁止自动覆盖。' }
        $certificate = [Security.Cryptography.X509Certificates.X509Certificate2]::new(
            $keyPath, $password, [Security.Cryptography.X509Certificates.X509KeyStorageFlags]::EphemeralKeySet)
        try {
            $fingerprint = $certificate.GetCertHashString([Security.Cryptography.HashAlgorithmName]::SHA256)
            $certificatePath = Join-Path $SigningDirectory 'release-certificate.cer'
            [IO.File]::WriteAllBytes($certificatePath, $certificate.Export([Security.Cryptography.X509Certificates.X509ContentType]::Cert))
        } finally { $certificate.Dispose() }
        $bytes = [Text.Encoding]::UTF8.GetBytes($password)
        try {
            $protected = [Security.Cryptography.ProtectedData]::Protect($bytes, $entropy,
                [Security.Cryptography.DataProtectionScope]::CurrentUser)
            @{
                keyPath = $keyPath
                keyAlias = 'upload'
                protectedPassword = [Convert]::ToBase64String($protected)
                certificateSha256 = $fingerprint
                createdAt = [DateTimeOffset]::UtcNow.ToString('O')
            } | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding utf8
        } finally { [Array]::Clear($bytes, 0, $bytes.Length) }
        Write-Output "签名密钥已创建：$keyPath"
        Write-Output "证书 SHA-256：$fingerprint"
    } finally {
        $env:ASTER_RELEASE_GENERATION_PASSWORD = $previousPassword
        $password = $null
    }
    exit 0
}

$configuration = Read-SigningConfiguration
if ($Action -eq 'Info') {
    Write-Output "签名密钥：$($configuration.keyPath)"
    Write-Output "别名：$($configuration.keyAlias)"
    Write-Output "证书 SHA-256：$($configuration.certificateSha256)"
    Write-Output '口令由 Windows 当前账户 DPAPI 保护，不在仓库中保存。'
    exit 0
}

$password = Read-Password $configuration
try {
    if ($Action -eq 'SyncGitHub') {
        Set-GitHubSecret 'ANDROID_RELEASE_KEYSTORE_BASE64' ([Convert]::ToBase64String([IO.File]::ReadAllBytes($configuration.keyPath)))
        Set-GitHubSecret 'ANDROID_RELEASE_STORE_PASSWORD' $password
        Set-GitHubSecret 'ANDROID_RELEASE_KEY_PASSWORD' $password
        & gh variable set ANDROID_RELEASE_CERT_SHA256 --repo $Repository --body $configuration.certificateSha256
        if ($LASTEXITCODE -ne 0) { throw '证书指纹变量写入失败。' }
        Write-Output '已写入 GitHub 证书指纹变量。'
    } elseif ($Action -eq 'Build') {
        $previousPath = $env:KEYSTORE_PATH
        $previousStore = $env:STORE_PASSWORD
        $previousKey = $env:KEY_PASSWORD
        Push-Location $projectDirectory
        try {
            $env:KEYSTORE_PATH = $configuration.keyPath
            $env:STORE_PASSWORD = $password
            $env:KEY_PASSWORD = $password
            & ./gradlew.bat :app:bundleRelease --offline --console=plain --no-configuration-cache --no-daemon
            if ($LASTEXITCODE -ne 0) { throw '正式 AAB 构建失败。' }
            & ./gradlew.bat :app:assembleRelease '-Paster.splitApks=true' --offline --console=plain --no-configuration-cache --no-daemon
            if ($LASTEXITCODE -ne 0) { throw '正式签名构建失败。' }
        } finally {
            $env:KEYSTORE_PATH = $previousPath
            $env:STORE_PASSWORD = $previousStore
            $env:KEY_PASSWORD = $previousKey
            Pop-Location
        }
    }
} finally { $password = $null }
