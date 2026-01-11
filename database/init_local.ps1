<#
.SYNOPSIS
    在 `red_tourism.db` 上运行数据库迁移并生成测试用户（选项 A 自动化脚本）。

.DESCRIPTION
    本脚本会：
      1. 在 repository 的 `database/` 目录下执行 `migrations/002_users_and_ratings.sql`，
         将 users/ratings 表创建到 `red_tourism.db`（合并方案，推荐）。
      2. 安装 Node 依赖（如未安装），并调用 `scripts/create_test_users.js` 向
         `red_tourism.db` 写入测试用户。

.PARAMETER Count
    要生成的测试用户数量（默认 10）。

.EXAMPLE
    # 从仓库根目录执行
    .\database\init_local.ps1 -Count 10
#>

param(
    [int]$Count = 10
)

Write-Host "[init_local] 开始初始化：将在 red_tourism.db 上运行迁移并生成 $Count 个测试用户。"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $scriptDir

if (-not (Get-Command sqlite3 -ErrorAction SilentlyContinue)) {
    Write-Warning "未检测到 sqlite3 命令；请先安装 sqlite3 或使用仓库提供的 setup_tools.ps1 来获得可用环境。脚本将仍尝试继续（若无 sqlite3 将跳过 .read 步骤）。"
} else {
    Write-Host "[init_local] 运行迁移：按名称顺序依次应用 migrations/*.sql -> red_tourism.db"
    $migrations = Get-ChildItem -Path .\migrations -Filter *.sql | Sort-Object Name
    foreach ($m in $migrations) {
        Write-Host "[init_local] 应用: $($m.Name)"
        sqlite3 red_tourism.db ".read migrations/$($m.Name)"
    }
}

if (-not (Test-Path "node_modules")) {
    Write-Host "[init_local] 安装 Node 依赖 (database/package.json)..."
    npm install
} else {
    Write-Host "[init_local] node_modules 已存在，跳过 npm install。"
}

Write-Host "[init_local] 运行 create_test_users.js，目标 DB: red_tourism.db"
node scripts/create_test_users.js --db red_tourism.db --count $Count

Write-Host "[init_local] 初始化完成：red_tourism.db 已应用迁移并生成测试用户（如无错误）。"
