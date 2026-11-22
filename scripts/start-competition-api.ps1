# 启动竞赛调研流程 API（默认 :8088）+ WHU DeepSeek LLM
# 用法:
#   $env:COMPETITION_LLM_API_KEY = "<your-key>"
#   .\scripts\start-competition-api.ps1
if (-not $env:COMPETITION_LLM_API_KEY) {
  Write-Host "请先设置环境变量 COMPETITION_LLM_API_KEY（不要把 Key 写进仓库）"
  exit 1
}
$env:COMPETITION_LLM_BASE_URL = if ($env:COMPETITION_LLM_BASE_URL) { $env:COMPETITION_LLM_BASE_URL } else { "https://maas.whu.edu.cn/v1" }
$env:COMPETITION_LLM_MODEL = if ($env:COMPETITION_LLM_MODEL) { $env:COMPETITION_LLM_MODEL } else { "deepseek-v4-flash" }

Write-Host "LLM: model=$($env:COMPETITION_LLM_MODEL) base=$($env:COMPETITION_LLM_BASE_URL) key=***"

Set-Location "$PSScriptRoot\..\rsi-flow"
mvn -pl flow-test -am install "-DskipTests" "-Djacoco.skip=true"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
mvn -pl flow-test exec:java "-Djacoco.skip=true" `
  "-Dexec.classpathScope=test" `
  "-Dexec.mainClass=cc.eamon.open.flow.competition.CompetitionApiServer"
