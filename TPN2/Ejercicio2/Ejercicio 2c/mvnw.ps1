$projectBaseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperJar = Join-Path $projectBaseDir ".mvn/wrapper/maven-wrapper.jar"

if (-not (Test-Path $wrapperJar)) {
    Write-Error "Could not find $wrapperJar"
    exit 1
}

& java "-Dmaven.multiModuleProjectDirectory=$projectBaseDir" -cp "$wrapperJar" "org.apache.maven.wrapper.MavenWrapperMain" @args
exit $LASTEXITCODE
