# CLI 跑一条竞赛调研流程（结构化画像）
# 用法: .\scripts\run-competition-cli.ps1
# 或:   .\scripts\run-competition-cli.ps1 --grade 大三 --motive 冲推免 --skills 算法 --hours 10-20
param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$CliArgs
)
$argLine = if ($CliArgs -and $CliArgs.Count -gt 0) { ($CliArgs -join ' ') } else { "" }
Set-Location "$PSScriptRoot\..\rsi-flow"
mvn -pl flow-test -am install "-DskipTests" "-Djacoco.skip=true"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($argLine) {
  mvn -pl flow-test exec:java "-Djacoco.skip=true" `
    "-Dexec.classpathScope=test" `
    "-Dexec.mainClass=cc.eamon.open.flow.competition.CompetitionCli" `
    "-Dexec.args=$argLine"
} else {
  mvn -pl flow-test exec:java "-Djacoco.skip=true" `
    "-Dexec.classpathScope=test" `
    "-Dexec.mainClass=cc.eamon.open.flow.competition.CompetitionCli"
}
