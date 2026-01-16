# 导出路线代码脚本
# 用途：将用户的路线代码导出，供另一个人综合修改

param(
    [string]$OutputDir = "route-code-export"  # 输出目录
)

Write-Host "=== 导出路线代码脚本 ===" -ForegroundColor Green
Write-Host ""

# 1. 检查当前分支
Write-Host "1. 检查当前分支..." -ForegroundColor Yellow
$currentBranch = git branch --show-current
Write-Host "   当前分支: $currentBranch" -ForegroundColor Cyan

if ($currentBranch -ne "main") {
    Write-Host "   警告: 当前不在main分支" -ForegroundColor Yellow
    $response = Read-Host "   是否切换到main分支？(y/n)"
    if ($response -eq "y") {
        git checkout main
        git pull origin main
    }
}

# 2. 创建输出目录
Write-Host "`n2. 创建输出目录..." -ForegroundColor Yellow
if (Test-Path $OutputDir) {
    Remove-Item -Recurse -Force $OutputDir
}
New-Item -ItemType Directory -Path $OutputDir | Out-Null
New-Item -ItemType Directory -Path "$OutputDir/frontend/src/views" | Out-Null
New-Item -ItemType Directory -Path "$OutputDir/frontend/src/services" | Out-Null
New-Item -ItemType Directory -Path "$OutputDir/backend/src/main/java/com/redseeker/route" | Out-Null
Write-Host "   ✓ 输出目录已创建: $OutputDir" -ForegroundColor Green

# 3. 复制路线相关文件
Write-Host "`n3. 复制路线相关文件..." -ForegroundColor Yellow

$routeFiles = @(
    @{src="frontend/src/views/RouteView.vue"; dst="$OutputDir/frontend/src/views/RouteView.vue"},
    @{src="frontend/src/services/routeService.js"; dst="$OutputDir/frontend/src/services/routeService.js"}
)

# 复制前端文件
foreach ($file in $routeFiles) {
    if (Test-Path $file.src) {
        Copy-Item $file.src $file.dst -Force
        Write-Host "   ✓ 已复制: $($file.src)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ 文件不存在: $($file.src)" -ForegroundColor Yellow
    }
}

# 复制后端文件
$backendRouteDir = "backend/src/main/java/com/redseeker/route"
if (Test-Path $backendRouteDir) {
    Get-ChildItem -Path $backendRouteDir -Filter "*.java" | ForEach-Object {
        $dstPath = "$OutputDir/backend/src/main/java/com/redseeker/route/$($_.Name)"
        Copy-Item $_.FullName $dstPath -Force
        Write-Host "   ✓ 已复制: $($_.FullName)" -ForegroundColor Green
    }
} else {
    Write-Host "   ⚠ 后端目录不存在: $backendRouteDir" -ForegroundColor Yellow
}

# 4. 创建说明文件
Write-Host "`n4. 创建说明文件..." -ForegroundColor Yellow
$readmeContent = @"
# 路线规划模块代码

这是从main分支导出的路线规划模块代码，供综合修改使用。

## 文件列表

### 前端
- frontend/src/views/RouteView.vue
- frontend/src/services/routeService.js

### 后端
- backend/src/main/java/com/redseeker/route/*.java

## 使用说明

1. 请将这部分代码与你的路线代码进行综合
2. 保留两个版本的优点
3. 完成后提交并push到你的分支
4. 然后通知我合并你的综合版本

## 注意事项

- 确保API接口兼容
- 确保数据库结构一致
- 测试所有功能正常
"@

$readmeContent | Out-File -FilePath "$OutputDir/README.md" -Encoding UTF8
Write-Host "   ✓ 说明文件已创建" -ForegroundColor Green

# 5. 创建压缩包（可选）
Write-Host "`n5. 创建压缩包..." -ForegroundColor Yellow
$zipFile = "$OutputDir.zip"
if (Test-Path $zipFile) {
    Remove-Item $zipFile -Force
}

# 使用PowerShell压缩
Compress-Archive -Path $OutputDir -DestinationPath $zipFile -Force
Write-Host "   ✓ 压缩包已创建: $zipFile" -ForegroundColor Green

Write-Host "`n=== 导出完成 ===" -ForegroundColor Green
Write-Host ""
Write-Host "导出的文件位置:" -ForegroundColor Yellow
Write-Host "   - 目录: $OutputDir" -ForegroundColor Cyan
Write-Host "   - 压缩包: $zipFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "请将压缩包发送给另一个人进行综合修改" -ForegroundColor Yellow
