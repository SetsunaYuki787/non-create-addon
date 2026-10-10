$ErrorActionPreference = 'Continue'
$root = 'E:\mc\.minecraft\versions\1.21.11-Fabric 0.19.5'
$jdk  = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot'
$t    = "$root\_work\tools"
$named = "$root\_work\named"
$addon = "$root\_work\addon"
$build = "$addon\build"
$java  = "$jdk\bin\java.exe"
$javac = "$jdk\bin\javac.exe"
$jarc  = "$jdk\bin\jar.exe"
$trcp  = "$t;$t\tiny-remapper-fat.jar"
$loader = 'E:\mc\.minecraft\libraries\net\fabricmc\fabric-loader\0.19.5\fabric-loader-0.19.5.jar'

New-Item -ItemType Directory -Force -Path $build | Out-Null

# ---- 0. geckolib -> named (once) ----
if (!(Test-Path "$named\geckolib-named.jar")) {
  Write-Host "=== [0] remap geckolib intermediary -> named ==="
  & $java -Xmx2G -cp $trcp TRRun "$root\mods\geckolib-fabric-1.21.11-5.4.5.jar" "$named\geckolib-named.jar" "$t\mappings.tiny" intermediary named
}

# --- Mojang / third-party libraries straight from the version json ---
$libRoot = 'E:\mc\.minecraft\libraries'
$vjson = Get-Content -Raw "$root\1.21.11-Fabric 0.19.5.json" | ConvertFrom-Json
$libPaths = New-Object System.Collections.Generic.List[string]
foreach ($lib in $vjson.libraries) {
  $rel = $null
  if ($lib.downloads -and $lib.downloads.artifact -and $lib.downloads.artifact.path) {
    $rel = $lib.downloads.artifact.path
  } elseif ($lib.name) {
    $p = $lib.name.Split(':')
    if ($p.Count -ge 3) { $rel = ($p[0] -replace '\.', '/') + "/$($p[1])/$($p[2])/$($p[1])-$($p[2]).jar" }
  }
  if ($rel) {
    $full = Join-Path $libRoot ($rel -replace '/', '\')
    if (Test-Path $full) { $libPaths.Add($full) }
    else { Write-Host "  (missing lib) $rel" }
  }
}
Write-Host "=== libraries on classpath: $($libPaths.Count) ==="

$cpJars = @(
  "$named\minecraft-named.jar",
  "$named\needsofnature-named.jar",
  "$named\fabric-api-base-named.jar",
  "$named\fabric-lifecycle-events-v1-named.jar",
  "$named\trinkets-named.jar",
  "$named\cardinal-components-base-named.jar",
  "$named\cardinal-components-entity-named.jar",
  "$named\geckolib-named.jar",
  "$named\create-named.jar",
  $loader
)
$cp = ($cpJars + $libPaths) -join ';'
# create-named.jar is only needed for javac: as a TinyRemapper *classpath* entry it makes
# TinyRemapper abort with "unfixable conflicts" (e.g. CrushingWheelControllerBlockEntity
# [Clearable/clear, method_5448]()V -> method_5448).
$remapCp = ((@($cpJars | Where-Object { $_ -notlike '*create-named.jar' }) + $libPaths)) -join ';'

# ---- 1. compile ----
Write-Host "=== [1] javac ==="
$classes = "$addon\classes"
if (Test-Path $classes) { Remove-Item -Recurse -Force $classes }
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$srcs = @(Get-ChildItem -Recurse -Filter *.java "$addon\src" | ForEach-Object { $_.FullName })
& $javac -encoding UTF-8 -nowarn -proc:none -cp $cp -d $classes $srcs
if ($LASTEXITCODE -ne 0) { Write-Host "JAVAC FAILED"; exit 1 }
Get-ChildItem -Recurse -Filter *.class $classes | ForEach-Object { Write-Host "  $($_.FullName.Substring($classes.Length+1))" }

# ---- 2. pack classes ----
Write-Host "=== [2] pack raw jar ==="
$raw = "$build\non-create-addon-raw.jar"
if (Test-Path $raw) { Remove-Item -Force $raw }
Push-Location $classes
& $jarc --create --file $raw .
Pop-Location
if ($LASTEXITCODE -ne 0) { Write-Host "JAR FAILED"; exit 1 }

# ---- 3. remap named -> intermediary ----
Write-Host "=== [3] remap named -> intermediary ==="
$mid = "$build\non-create-addon-intermediary.jar"
& $java -Xmx1G -cp $trcp TRRun $raw $mid "$t\mappings.tiny" named intermediary $remapCp.Split(';')
if ($LASTEXITCODE -ne 0) { Write-Host "REMAP FAILED"; exit 1 }

# ---- 4. add resources ----
Write-Host "=== [4] add resources ==="
$final = "$build\non-create-addon-1.0.0.jar"
Copy-Item $mid $final -Force
& $jarc --update --file $final -C "$addon\res" .
if ($LASTEXITCODE -ne 0) { Write-Host "RESOURCE PACK FAILED"; exit 1 }

Write-Host "`n=== final jar contents ==="
& $jarc --list --file $final
Write-Host "`nsize: $((Get-Item $final).Length) bytes"
Write-Host "path: $final"
