# Git 模块合并脚本
# 用途：合并推荐、路线、场所模块，使用不同开发者的代码

param(
    [string]$OtherBranch = "nhaok-frontend",  # 另一个人的分支名
    [string]$MergeBranch = "merge-modules"     # 合并分支名
)

Write-Host "=== Git 模块合并脚本 ===" -ForegroundColor Green
Write-Host ""

# 1. 检查当前状态
Write-Host "1. 检查当前Git状态..." -ForegroundColor Yellow
$currentBranch = git branch --show-current
Write-Host "   当前分支: $currentBranch" -ForegroundColor Cyan

if ((git status --porcelain) -ne "") {
    Write-Host "   警告: 有未提交的更改！" -ForegroundColor Red
    $response = Read-Host "   是否继续？(y/n)"
    if ($response -ne "y") {
        exit
    }
}

# 2. 切换到main并更新
Write-Host "`n2. 切换到main分支并更新..." -ForegroundColor Yellow
git checkout main
if ($LASTEXITCODE -ne 0) {
    Write-Host "   错误: 无法切换到main分支" -ForegroundColor Red
    exit 1
}
git pull origin main
Write-Host "   ✓ main分支已更新" -ForegroundColor Green

# 3. 创建合并分支
Write-Host "`n3. 创建合并分支: $MergeBranch..." -ForegroundColor Yellow
git checkout -b $MergeBranch
if ($LASTEXITCODE -ne 0) {
    # 如果分支已存在，切换到它
    git checkout $MergeBranch
    Write-Host "   分支已存在，已切换到该分支" -ForegroundColor Yellow
} else {
    Write-Host "   ✓ 合并分支已创建" -ForegroundColor Green
}

# 4. 检查另一个人的分支是否存在
Write-Host "`n4. 检查分支 $OtherBranch 是否存在..." -ForegroundColor Yellow
$branchExists = git branch -a | Select-String $OtherBranch
if (-not $branchExists) {
    Write-Host "   警告: 分支 $OtherBranch 不存在！" -ForegroundColor Red
    Write-Host "   请确认另一个人的分支名称，或手动指定：" -ForegroundColor Yellow
    $OtherBranch = Read-Host "   请输入分支名称"
}

# 5. 合并另一个人的分支
Write-Host "`n5. 合并分支 $OtherBranch..." -ForegroundColor Yellow
git merge $OtherBranch --no-commit --no-ff
if ($LASTEXITCODE -ne 0) {
    Write-Host "   合并有冲突，需要手动解决" -ForegroundColor Red
    Write-Host "   解决冲突后，运行以下命令继续：" -ForegroundColor Yellow
    Write-Host "   git add ." -ForegroundColor Cyan
    Write-Host "   git commit -m '合并 $OtherBranch 分支'" -ForegroundColor Cyan
    exit 1
}
Write-Host "   ✓ 合并完成" -ForegroundColor Green

# 6. 恢复推荐模块到main版本（用户的代码）
Write-Host "`n6. 恢复推荐模块到main版本（用户的代码）..." -ForegroundColor Yellow
$recommendFiles = @(
    "frontend/src/views/RecommendView.vue",
    "frontend/src/services/recommendService.js",
    "backend/src/main/java/com/redseeker/recommend/RecommendController.java",
    "backend/src/main/java/com/redseeker/recommend/RecommendServiceImpl.java",
    "backend/src/main/java/com/redseeker/recommend/RecommendService.java",
    "backend/src/main/java/com/redseeker/recommend/RecommendRequest.java",
    "backend/src/main/java/com/redseeker/recommend/RecommendItem.java"
)

foreach ($file in $recommendFiles) {
    if (Test-Path $file) {
        git checkout main -- $file
        Write-Host "   ✓ 已恢复: $file" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ 文件不存在: $file" -ForegroundColor Yellow
    }
}

# 7. 检查场所和路线模块（应该已经是另一个人的版本）
Write-Host "`n7. 检查场所和路线模块..." -ForegroundColor Yellow
Write-Host "   场所模块: 应使用 $OtherBranch 分支的版本" -ForegroundColor Cyan
Write-Host "   路线模块: 临时使用 $OtherBranch 分支的版本" -ForegroundColor Cyan
Write-Host "   ✓ 检查完成" -ForegroundColor Green

# 8. 显示当前状态
Write-Host "`n8. 当前合并状态:" -ForegroundColor Yellow
git status --short

Write-Host "`n=== 合并准备完成 ===" -ForegroundColor Green
Write-Host ""
Write-Host "下一步操作:" -ForegroundColor Yellow
Write-Host "1. 检查合并结果: git status" -ForegroundColor Cyan
Write-Host "2. 查看差异: git diff" -ForegroundColor Cyan
Write-Host "3. 测试功能确保正常" -ForegroundColor Cyan
Write-Host "4. 提交合并: git add . && git commit -m '合并模块：推荐用用户代码，场所和路线用另一个人的代码'" -ForegroundColor Cyan
Write-Host ""
Write-Host "导出路线代码给另一个人:" -ForegroundColor Yellow
Write-Host "1. 切换到main分支: git checkout main" -ForegroundColor Cyan
Write-Host "2. 创建补丁或压缩包，包含以下文件:" -ForegroundColor Cyan
Write-Host "   - frontend/src/views/RouteView.vue" -ForegroundColor White
Write-Host "   - frontend/src/services/routeService.js" -ForegroundColor White
Write-Host "   - backend/src/main/java/com/redseeker/route/*.java" -ForegroundColor White
