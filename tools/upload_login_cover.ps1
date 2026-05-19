# 上传登录页封面到 MinIO（覆盖 site/img/login-cover.*）
# 前置：后端、MinIO 已启动；管理员已登录，拿到浏览器 Cookie 里的 JSESSIONID。
#
# PowerShell 示例：
#   $env:CM_SESSION = "JSESSIONID=xxxxxxxx"
#   .\tools\upload_login_cover.ps1 -ImagePath "F:\毕业设计\封面图\登录界面\封面.jpg"
#
# 若已安装 curl，也可直接：
#   curl -X POST "http://localhost:8888/admin/site/login-cover" -H "Cookie: JSESSIONID=xxx" -F "file=@F:/毕业设计/封面图/登录界面/封面.jpg"

param(
  [Parameter(Mandatory = $true)]
  [string] $ImagePath,
  [string] $BaseUrl = "http://localhost:8888"
)

if (-not (Test-Path -LiteralPath $ImagePath)) {
  Write-Error "文件不存在: $ImagePath"
  exit 1
}

if (-not $env:CM_SESSION) {
  Write-Error "请先设置: `$env:CM_SESSION='JSESSIONID=你的会话值'"
  exit 1
}

$curl = Get-Command curl.exe -ErrorAction SilentlyContinue
if (-not $curl) {
  Write-Error "未找到 curl.exe，请安装 Windows curl 或使用 Postman 调用 POST $BaseUrl/admin/site/login-cover (multipart field: file)"
  exit 1
}

& curl.exe -s -S -X POST "$BaseUrl/admin/site/login-cover" -H "Cookie: $env:CM_SESSION" -F "file=@$($ImagePath.Replace('\','/'))"
Write-Host "`n上传完成后刷新登录页。"
