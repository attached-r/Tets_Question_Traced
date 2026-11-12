# =====================================================================
# 企业内部问题登记小助手 · 接口验收测试（可重复执行）
#
# 用法：
#   1) 后端已启动（http://localhost:8080），数据库已按 schema.sql 初始化
#   2) powershell -NoProfile -ExecutionPolicy Bypass -File tests\acceptance.ps1
#
# 设计要点：
#   - 每次运行用带时间戳的 request_id，不依赖上一次的数据，可以反复跑；
#   - 只读检查种子数据（R001-R008），不改动它们；
#   - 输出 PASS/FAIL 明细，并写入同目录 result.txt；有失败时退出码为 1。
# =====================================================================

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$run = Get-Date -Format 'HHmmss'

$script:pass = 0
$script:fail = 0
$script:lines = New-Object System.Collections.Generic.List[string]

function Add-Log([string]$text) {
    $script:lines.Add($text)
    Write-Host $text
}

function Check([string]$id, [string]$desc, $actual, $expect) {
    if ("$actual" -eq "$expect") {
        $script:pass++
        Add-Log ("PASS | {0} | {1}" -f $id, $desc)
    } else {
        $script:fail++
        Add-Log ("FAIL | {0} | {1} | expect={2} actual={3}" -f $id, $desc, $expect, $actual)
    }
}

# Windows PowerShell 5.1 的 Invoke-RestMethod 会用 ASCII 编码请求体，
# 中文会变成问号。统一按 UTF-8 字节收发，测的才是后端而不是客户端编码。
function Api {
    param($Method, $Path, $Token, $Body)

    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    $params = @{
        Uri             = "$base$Path"
        Method          = $Method
        Headers         = $headers
        ContentType     = 'application/json; charset=utf-8'
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $json = ConvertTo-Json -InputObject $Body -Compress -Depth 5
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes($json)
    }

    $response = Invoke-WebRequest @params
    $text = [System.Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
    return ($text | ConvertFrom-Json)
}

# ---------------------------------------------------------------------
# 准备：登录三类账号
# ---------------------------------------------------------------------
Add-Log "=== 登录 ==="
$emp = Api POST '/api/auth/login' $null @{ username = 'xli'; password = '123456' }
$empOther = Api POST '/api/auth/login' $null @{ username = 'xchen'; password = '123456' }
$sup = Api POST '/api/auth/login' $null @{ username = 'support01'; password = '123456' }

$empToken = $emp.data.token
$empOtherToken = $empOther.data.token
$supToken = $sup.data.token

Check 'LOGIN' '员工登录成功' $emp.code 200
Check 'LOGIN' '技术支持登录成功' $sup.code 200
Check 'LOGIN' '角色识别正确' $sup.data.user.roleCode 'SUPPORT'

# ---------------------------------------------------------------------
# TC-01 提交并保留原文（含换行、缩进、全角标点）
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-01 录入与保留原文 ==="
$reqA = "REQ-A-$run"
$content1 = "今天系统登录页面一直转圈，我进不去。`n日志片段：`n  2026-11-12 10:00 ERROR timeout"
$created = Api POST '/api/feedback' $empToken @{ requestId = $reqA; typeCode = 'SYSTEM_FAULT'; content = $content1 }
Check 'TC-01' '提交成功' $created.code 200
Check 'TC-01' '初始状态为待处理' $created.data.status 0

$detailA = Api GET "/api/feedback/$($created.data.id)" $empToken
Check 'TC-01' '原文逐字符一致（换行缩进未丢）' ([bool]($detailA.data.content -ceq $content1)) 'True'
Check 'TC-01' '类型标签正确' $detailA.data.typeLabel '系统故障'
Check 'TC-01' '时间线有提交记录' $detailA.data.logs.Count 1

# ---------------------------------------------------------------------
# TC-02 类型五选一
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-02 类型必选且受枚举约束 ==="
$typeList = @('PERMISSION', 'FEATURE', 'CONSULT', 'UNDETERMINED')
$typeIndex = 0
foreach ($typeCode in $typeList) {
    $typeIndex++
    $r = Api POST '/api/feedback' $empToken @{
        requestId = "REQ-T-$run-$typeIndex"
        typeCode  = $typeCode
        content   = "类型校验样例 $typeCode"
    }
    Check 'TC-02' "类型 $typeCode 可提交" $r.data.typeCode $typeCode
}
$badType = Api POST '/api/feedback' $empToken @{ requestId = "REQ-BAD-$run"; typeCode = 'FAULT'; content = '非法类型' }
Check 'TC-02' '非法类型被拒（400）' $badType.code 400
$missingType = Api POST '/api/feedback' $empToken @{ requestId = "REQ-MISS-$run"; content = '缺少类型' }
Check 'TC-02' '缺少类型被拒（400）' $missingType.code 400

# ---------------------------------------------------------------------
# TC-03 相同 request_id 幂等
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-03 相同 request_id 不产生第二条记录 ==="
$dup = Api POST '/api/feedback' $empToken @{ requestId = $reqA; typeCode = 'FEATURE'; content = '重复提交测试' }
Check 'TC-03' '重复编号返回 40901' $dup.code 40901

$myList = Api GET '/api/feedback/my?page=1&size=100' $empToken
$sameRequestCount = @($myList.data.records | Where-Object { $_.requestId -eq $reqA }).Count
Check 'TC-03' '该编号只有 1 条记录' $sameRequestCount 1

$detailA2 = Api GET "/api/feedback/$($created.data.id)" $empToken
Check 'TC-03' '原文未被覆盖' ([bool]($detailA2.data.content -ceq $content1)) 'True'

# ---------------------------------------------------------------------
# TC-04 纯空白内容
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-04 纯空白不能保存 ==="
$fullWidthSpace = [string]([char]0x3000) + [string]([char]0x3000)
$zeroWidthSpace = [string]([char]0x200B) + [string]([char]0x200B)

$w1 = Api POST '/api/feedback' $empToken @{ requestId = "REQ-W1-$run"; typeCode = 'CONSULT'; content = '   ' }
$w2 = Api POST '/api/feedback' $empToken @{ requestId = "REQ-W2-$run"; typeCode = 'CONSULT'; content = $fullWidthSpace }
$w3 = Api POST '/api/feedback' $empToken @{ requestId = "REQ-W3-$run"; typeCode = 'CONSULT'; content = "`n`t`r" }
$w4 = Api POST '/api/feedback' $empToken @{ requestId = "REQ-W4-$run"; typeCode = 'CONSULT'; content = $zeroWidthSpace }

Check 'TC-04' '半角空格被拒' $w1.code 400
Check 'TC-04' '全角空格被拒' $w2.code 400
Check 'TC-04' '换行制表符被拒' $w3.code 400
Check 'TC-04' '零宽空格被拒' $w4.code 400
Add-Log ("提示语：" + $w2.message)

# ---------------------------------------------------------------------
# TC-05 编号格式与长度
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-05 编号格式校验 ==="
$badId1 = Api POST '/api/feedback' $empToken @{ requestId = 'REQ 带空格'; typeCode = 'CONSULT'; content = '编号非法' }
$badId2 = Api POST '/api/feedback' $empToken @{ requestId = 'a'; typeCode = 'CONSULT'; content = '编号太短' }
Check 'TC-05' '含空格编号被拒' $badId1.code 400
Check 'TC-05' '过短编号被拒' $badId2.code 400

$tooLong = Api POST '/api/feedback' $empToken @{
    requestId = "REQ-LONG-$run"
    typeCode  = 'CONSULT'
    content   = ('x' * 2001)
}
Check 'TC-05' '2001 字内容被拒' $tooLong.code 400

# ---------------------------------------------------------------------
# TC-06 并发状态更新（乐观锁）
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-06 并发更新只有一次生效 ==="
$concurrent = Api POST '/api/feedback' $empToken @{
    requestId = "REQ-CC-$run"
    typeCode  = 'SYSTEM_FAULT'
    content   = '并发更新测试'
}
$concurrentId = $concurrent.data.id

$patchScript = {
    param($Base, $Token, $Id)
    try {
        $p = @{
            Uri             = "$Base/api/feedback/$Id/status"
            Method          = 'Patch'
            Headers         = @{ Authorization = "Bearer $Token" }
            ContentType     = 'application/json; charset=utf-8'
            UseBasicParsing = $true
            Body            = [System.Text.Encoding]::UTF8.GetBytes('{"targetStatus":1,"version":0}')
        }
        $resp = Invoke-WebRequest @p
        ([System.Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())) | ConvertFrom-Json
    } catch {
        @{ code = -1; message = $_.Exception.Message }
    }
}
$job1 = Start-Job -ScriptBlock $patchScript -ArgumentList $base, $supToken, $concurrentId
$job2 = Start-Job -ScriptBlock $patchScript -ArgumentList $base, $supToken, $concurrentId
Wait-Job $job1, $job2 | Out-Null
$res1 = (Receive-Job $job1)[-1]
$res2 = (Receive-Job $job2)[-1]
Remove-Job $job1, $job2

$codes = (@($res1.code, $res2.code) | Sort-Object) -join ','
Check 'TC-06' '一次成功一次 40903（版本冲突）' $codes '200,40903'

$afterConcurrent = Api GET "/api/feedback/$concurrentId" $empToken
$startLogs = @($afterConcurrent.data.logs | Where-Object { $_.operateType -eq 2 }).Count
Check 'TC-06' '状态变更日志只有 1 条' $startLogs 1
Check 'TC-06' '最终状态为处理中' $afterConcurrent.data.status 1

# ---------------------------------------------------------------------
# TC-06b 过期版本（确定性，不依赖并发时序）
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-06b 过期版本号被拒 ==="
$noteV0 = $afterConcurrent.data.version
$n1 = Api PATCH "/api/feedback/$concurrentId/note" $supToken @{ note = '第一次写备注'; version = $noteV0 }
Check 'TC-06b' '第一次写备注成功' $n1.code 200
Check 'TC-06b' '版本号递增' $n1.data.version ($noteV0 + 1)
$n2 = Api PATCH "/api/feedback/$concurrentId/note" $supToken @{ note = '用过期版本再写'; version = $noteV0 }
Check 'TC-06b' '过期版本被拒（40903）' $n2.code 40903
$n3 = Api GET "/api/feedback/$concurrentId" $supToken
Check 'TC-06b' '备注未被覆盖' $n3.data.handlerNote '第一次写备注'

# ---------------------------------------------------------------------
# TC-07 状态机非法流转
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-07 状态机只允许 待处理→处理中→已完成 ==="
$flow = Api POST '/api/feedback' $empToken @{
    requestId = "REQ-FLOW-$run"
    typeCode  = 'CONSULT'
    content   = '状态流转测试'
}
$flowId = $flow.data.id

$step1 = Api PATCH "/api/feedback/$flowId/status" $supToken @{ targetStatus = 1; version = 0 }
Check 'TC-07' '待处理→处理中 允许' $step1.code 200
$step2 = Api PATCH "/api/feedback/$flowId/status" $supToken @{ targetStatus = 2; version = 1 }
Check 'TC-07' '处理中→已完成 允许' $step2.code 200
Check 'TC-07' '完成时间已写入' ([bool]($null -ne $step2.data.finishTime)) 'True'

$step3 = Api PATCH "/api/feedback/$flowId/status" $supToken @{ targetStatus = 1; version = 2 }
Check 'TC-07' '已完成→处理中 被拒（40902）' $step3.code 40902
Add-Log ("提示语：" + $step3.message)

$skip = Api POST '/api/feedback' $empToken @{ requestId = "REQ-SKIP-$run"; typeCode = 'CONSULT'; content = '咨询类直接结案' }
$skipDone = Api PATCH "/api/feedback/$($skip.data.id)/status" $supToken @{ targetStatus = 2; version = 0 }
Check 'TC-07' '待处理→已完成（咨询类直结）允许' $skipDone.code 200

# ---------------------------------------------------------------------
# TC-08 员工越权改状态
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-08 提交者不能改状态 ==="
$denied = Api PATCH "/api/feedback/$($created.data.id)/status" $empToken @{ targetStatus = 2; version = 0 }
Check 'TC-08' '员工改状态被拒（403）' $denied.code 403

# ---------------------------------------------------------------------
# TC-09 员工读他人记录
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-09 提交者只能看自己的记录 ==="
$otherDetail = Api GET "/api/feedback/$($created.data.id)" $empOtherToken
Check 'TC-09' '越权读他人记录被拒（403）' $otherDetail.code 403
Check 'TC-09' '响应不含他人原文' ([bool]($null -eq $otherDetail.data)) 'True'

$otherList = Api GET '/api/feedback?page=1&size=10' $empToken
Check 'TC-09' '员工访问看板被拒（403）' $otherList.code 403

# ---------------------------------------------------------------------
# TC-10 内部备注字段边界
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-10 内部备注只对技术支持可见 ==="
$noteTest = Api POST '/api/feedback' $empToken @{
    requestId = "REQ-NOTE-$run"
    typeCode  = 'SYSTEM_FAULT'
    content   = '备注可见性测试'
}
$noteTestId = $noteTest.data.id
$null = Api PATCH "/api/feedback/$noteTestId/note" $supToken @{ note = '内部备注：疑似网关证书过期'; version = 0 }

$empView = Api GET "/api/feedback/$noteTestId" $empToken
$supView = Api GET "/api/feedback/$noteTestId" $supToken

$empKeys = @($empView.data.PSObject.Properties.Name)
Check 'TC-10' '员工响应中没有 handlerNote 字段' ([bool](-not ($empKeys -contains 'handlerNote'))) 'True'
Check 'TC-10' '技术支持可以读到备注' ([bool]($supView.data.handlerNote -like '*网关证书*')) 'True'

$empRemarks = @($empView.data.logs | Where-Object { $null -ne $_.remark }).Count
$supRemarks = @($supView.data.logs | Where-Object { $null -ne $_.remark }).Count
Check 'TC-10' '员工侧时间线不含内部说明' $empRemarks 0
Check 'TC-10' '技术支持侧时间线含内部说明' ([bool]($supRemarks -ge 1)) 'True'

# ---------------------------------------------------------------------
# TC-11 认证
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-11 未登录与伪造凭证 ==="
$noToken = Api GET '/api/feedback/my' $null
$fakeToken = Api GET '/api/feedback/my' 'abc.def.ghi'
Check 'TC-11' '无 token 返回 401' $noToken.code 401
Check 'TC-11' '伪造 token 返回 401' $fakeToken.code 401

$wrongPassword = Api POST '/api/auth/login' $null @{ username = 'xli'; password = 'wrong' }
Check 'TC-11' '错误密码返回 400' $wrongPassword.code 400

# ---------------------------------------------------------------------
# TC-12 类型建议器
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== TC-12 类型建议器（只建议，不落库）==="
$s1 = Api POST '/api/meta/type-suggest' $empToken @{ content = '今天系统登录页面一直转圈，我进不去。' }
$s2 = Api POST '/api/meta/type-suggest' $empToken @{ content = '帮我申请一下项目资料的查看权限，现在提示无权限。' }
$s3 = Api POST '/api/meta/type-suggest' $empToken @{ content = '希望查询页面能保存上次的筛选条件，每次都得重新选。' }
$s4 = Api POST '/api/meta/type-suggest' $empToken @{ content = '请问怎么重置自己的登录密码？' }
$s5 = Api POST '/api/meta/type-suggest' $empToken @{ content = '那个先不用了。' }

Check 'TC-12' 'R001 → 系统故障' $s1.data.typeCode 'SYSTEM_FAULT'
Check 'TC-12' 'R002 → 权限申请' $s2.data.typeCode 'PERMISSION'
Check 'TC-12' 'R003 → 功能建议' $s3.data.typeCode 'FEATURE'
Check 'TC-12' 'R005 → 使用咨询' $s4.data.typeCode 'CONSULT'
Check 'TC-12' 'R008 指向不明 → 待确认（不猜）' $s5.data.typeCode 'UNDETERMINED'
Check 'TC-12' 'R008 标记为不自信' $s5.data.confident 'False'

# ---------------------------------------------------------------------
# 种子数据只读检查
# ---------------------------------------------------------------------
Add-Log ""
Add-Log "=== 种子数据检查（只读）==="
$board = Api GET '/api/feedback?page=1&size=200' $supToken
$seedIds = @('R001', 'R002', 'R003', 'R004', 'R005', 'R006', 'R007', 'R008')
$existingIds = @($board.data.records | ForEach-Object { $_.requestId })
$missing = @($seedIds | Where-Object { -not ($existingIds -contains $_) })
Check 'SEED' '8 条样例反馈都在' $missing.Count 0

$r008 = $board.data.records | Where-Object { $_.requestId -eq 'R008' } | Select-Object -First 1
Check 'SEED' 'R008 类型为待确认' $r008.typeLabel '待确认'

$filtered = Api GET '/api/feedback?page=1&size=50&status=0&typeCode=SYSTEM_FAULT' $supToken
$allMatch = @($filtered.data.records | Where-Object { $_.status -ne 0 -or $_.typeCode -ne 'SYSTEM_FAULT' }).Count
Check 'SEED' '看板筛选结果符合条件' $allMatch 0

$byRequest = Api GET "/api/feedback/by-request/R004" $empToken
Check 'SEED' '按编号回查可用' $byRequest.code 200
$byRequestOther = Api GET "/api/feedback/by-request/R004" $empOtherToken
Check 'SEED' '他人按编号回查被拒（403）' $byRequestOther.code 403

# ---------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------
Add-Log ""
Add-Log ("TOTAL pass={0} fail={1}" -f $script:pass, $script:fail)

try {
    $outFile = Join-Path $PSScriptRoot 'result.txt'
    $script:lines | Set-Content -Path $outFile -Encoding UTF8
    Add-Log "结果已写入 $outFile"
} catch {
    Write-Host "写入 result.txt 失败（不影响测试结论）：$($_.Exception.Message)"
}

if ($script:fail -gt 0) { exit 1 } else { exit 0 }
