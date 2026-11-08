# 业务台账生成：后端开发步骤与接口草案

版本：v0.1，2026-10-19。本文用于讨论和前后端对接，以下 `/api/v1` 接口尚未实现。请求、响应中的客户和项目均为虚构示例。

## 1. 目标与边界

用户输入客户经理姓名，后端从贷款余额表、基金余额表查询其名下客户及项目。前端展示查询结果并允许勾选。后端根据选中的项目生成带基础资料、样式、合并单元格和公式的底表，再填入发放、回收等数据，提供下载。

首版建议采用同步处理、本地 Excel 数据源和本地文件存储，不引入数据库或任务队列。先通过 Postman 验证接口，再连接前端。处理耗时超过实际请求超时限制时，再设计异步任务接口。

本阶段只确定方案及接口，不修改现有业务实现。

## 2. 已确认内容与待确认规则

### 已确认

- 贷款、基金来自两份结构一致的余额表，通过数据源区分业务板块。
- 从余额表获取客户名称、项目名称、业务分类和贷款余额，按客户经理姓名筛选。
- 前端先展示候选项目，用户勾选后才生成底表。
- 底表采用现有 47 列布局，基金在前、贷款在后，包含小结、总计和按月公式。
- 项目清单以余额表为主；首版候选范围先限定为这两张余额表，其他来源补充项目需另定规则。

### 编码前需要确认

| 问题 | 草案建议／当前事实 |
| --- | --- |
| 两份余额表的位置 | 当前目录只有一份明确命名的 `surplusdata.xlsx`；需要指定其属于贷款还是基金，以及另一份文件路径 |
| 客户经理匹配 | 建议去掉首尾空白后完整匹配“项目经办经理A”，不做包含匹配；如需经理B、多人姓名拆分，应另定规则 |
| 同名经理 | 当前按姓名查询会合并同名人员的结果；响应展示所属处室。若需区分同名人员，增加处室或员工编号条件 |
| 同一项目多行 | 先确认每行代表合同、分笔余额还是重复记录，再决定余额求和及去重。不能仅凭行内容相同就删除合法分笔 |
| 项目识别 | 优先使用业务项目编号；当前样表没有编号，暂用“来源板块＋客户名称＋项目名称”作为查询内分组键，存在歧义时提示并拒绝自动合并 |
| 贷款类型 | 当前余额表没有此字段，不能把“业务分类描述”直接当作贷款类型；需补充来源或分类映射 |
| 执行利率、授信额度 | 当前余额表没有这两项，建议首版留空，不填 0；如同项目存在多个利率，应另定展示方式 |
| 回收统计口径 | 实际回收、基金回收、剩余计划回收如何组合？是否按月份切换？是否有重叠记录？需先确认，不能按执行顺序覆盖 |
| 项目与明细匹配 | 明细表如果仅有项目名称，需检查跨客户或跨板块同名项目；有歧义时返回错误，不能将同一金额重复填入两行 |
| 金额显示 | 建议 JSON 金额使用人民币元的十进制字符串；Excel 统一万元、显示两位小数，新增“单位：万元”说明，具体位置待确认 |
| 缺失余额 | 建议返回 `null` 并提示；合法 0 保留为 0。缺失值是否允许生成台账需确认 |

这些待确认项不应被当成已批准的业务规则。可以先实现查询与展示，待统计口径确认后再实现填数。

## 3. 后端开发顺序

### 第一步：配置、读取与数据模型

1. 在 `ExcelProperties` 中分别配置贷款余额表、基金余额表，以及输出目录。
2. 建议新增 `excel.source.loan-balance.path`、`excel.source.fund-balance.path`、`excel.output.directory`。现有 `surplus` 配置如何迁移应与旧接口一起处理，不重复维护两套含义相同的配置。
3. 补齐余额行的 `customerName`，将经理、处室、业务分类字段命名明确化，金额使用 `BigDecimal`，避免将缺失金额直接转换成 0。
4. 检查必需表头、文件可读性和数值格式。记录来源板块、工作表和行号，以便定位问题。
5. 同结构的两份余额表复用同一 Reader，以读取参数标记 `FUND` 或 `LOAN`。

验收：两份文件均可独立读取；缺表头、非法金额等问题能定位；金额不会因浮点计算或空值兜底失真。

### 第二步：客户经理项目查询

1. 规范化姓名输入并筛选两份余额表。
2. 按确认的项目键分组，处理多行余额和分类冲突。
3. 建立查询快照：保存候选项目、来源文件内容指纹、查询时间。
4. 生成 `queryId` 和查询内唯一的 `projectId`；项目 ID 不使用 Excel 行号，也不直接使用项目名称。
5. 返回平铺项目列表，前端可按板块、客户进行分组展示和勾选。

建议查询快照有效期 30 分钟，可配置；使用有容量限制且定期清理的存储。首版单实例可内存保存，应用重启后旧快照失效。

验收：准确匹配经理；无结果返回空列表；跨板块同名项目不混淆；前端能够保留勾选 ID。

### 第三步：生成底表

1. 校验查询快照有效，所有项目 ID 均来自该快照，且余额文件内容未变化。
2. 使用服务端快照中的资料生成底表，前端只提交 ID，不提交可篡改的余额或项目资料。
3. 建议排序为基金、贷款；每个板块内按客户首次出现顺序、项目首次出现顺序排列。前端勾选顺序不决定合并结构。
4. 生成三行表头、项目行、客户名称纵向合并、基金小结、贷款小结及总计。
5. 只选一个板块时，建议仅生成该板块及其小结，保留总计；不生成空明细范围或非法合并区域。
6. 将已有的业务分类、余额写入底表；未提供来源的字段按约定留空。
7. 使用 `ledgerId` 独立目录保存底表，禁止继续共用一个全局 `output.xlsx`。输出先写临时文件，成功后发布为可下载文件。

布局定义应集中管理：项目名称 C 列（索引 2），业务分类 E 列（4），执行利率 F 列（5），授信额度 G 列（6），贷款余额 H 列（7），发放 I 列起（8），回收 V 列起（21），新增 AI 列起（34），明细第 4 行起（3）。

小结、总计行号和借款人合并范围根据选中项目动态计算。修正样表 G32、H32 少包含最后三行的问题；业务分类用文本格式；保留余额小结公式。

验收：选中项目与生成行一一对应；不跨板块合并客户；样式、行列尺寸、公式及月份位置正确；不同请求文件互不覆盖。

### 第四步：填入业务数据

1. 将现有处理方法调整为接收目标台账、所选项目及统计年份，不依赖全局输出路径或写死的 2026 年。
2. 分别读取发放、实际回收、基金回收和计划回收数据。
3. 按确认的口径在内存中形成最终“项目＋月份”金额，再一次性写入对应单元格。
4. 每次从该台账的原始底表重新构建完成版，避免重复填数叠加、旧月份残留和先后覆盖。
5. 记录各源文件指纹、匹配／未匹配记录数量和警告；文件读取失败与“合法但无匹配记录”分别处理。
6. 计算或验证公式结果并保存，明确所采用公式引擎的验证范围。

验收：重复执行不重复累计；数据删除后旧值消失；同名项目不串数据；最终汇总与独立手算样例一致。

### 第五步：下载与前端联调

1. 前端完成姓名输入、项目勾选、底表预览／下载、填数和完成版下载。
2. 先用 Postman 验证成功和错误路径，再接前端。
3. 如果前后端端口不同，按实际前端地址配置 CORS；下载需向浏览器暴露 `Content-Disposition`。
4. 后续如部署为多用户服务，查询快照和台账必须绑定登录用户；仅凭经理姓名不构成访问授权。首版定位本机单用户开发工具。

## 4. 接口总览

Base URL：`http://localhost:8080/api/v1`

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/project-queries` | 按客户经理查询候选项目，建立快照 |
| POST | `/ledgers` | 按勾选项目生成底表 |
| POST | `/ledgers/{ledgerId}/populate` | 填入业务数据，生成完成版 |
| GET | `/ledgers/{ledgerId}/file?stage=template` | 下载底表 |
| GET | `/ledgers/{ledgerId}/file?stage=completed` | 下载完成版 |

最后两行是同一个下载接口，通过 `stage` 区分文件。需要明确传入 `stage`，避免前端下载到错误阶段。

JSON 请求使用 `Content-Type: application/json`。JSON 响应统一为：

```json
{
  "code": "OK",
  "message": "处理成功",
  "data": {},
  "requestId": "req_example"
}
```

下载成功直接返回 XLSX 字节，不套 JSON。错误使用相应 HTTP 状态码，不再全部返回 HTTP 200 和字符串。

金额用十进制字符串（例如 `"12500000.00"`），缺失使用 `null`；日期时间采用带时区的 ISO 8601 格式。`FUND` 表示基金，`LOAN` 表示贷款。所有示例 ID 都是占位值，实际须取自前一接口响应。

## 5. 查询候选项目

`POST /project-queries`

```json
{
  "managerName": "张三"
}
```

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| managerName | string | 必填，去首尾空白后长度建议为 1～50；按完整姓名匹配 |

成功：HTTP 201，创建了查询快照。

```json
{
  "code": "OK",
  "message": "查询成功",
  "data": {
    "queryId": "qry_example",
    "managerName": "张三",
    "createdAt": "2026-10-19T21:00:00+08:00",
    "expiresAt": "2026-10-19T21:30:00+08:00",
    "currency": "CNY",
    "amountUnit": "YUAN",
    "projectCount": 2,
    "projects": [
      {
        "projectId": "prj_fund_example",
        "businessType": "FUND",
        "customerName": "示例交通集团",
        "projectName": "示例公路基金项目",
        "businessCategory": "示例业务分类A",
        "loanBalance": "12500000.00",
        "managerName": "张三",
        "department": "示例业务处",
        "loanType": null,
        "selectable": true,
        "issues": []
      },
      {
        "projectId": "prj_loan_example",
        "businessType": "LOAN",
        "customerName": "示例交通集团",
        "projectName": "示例道路贷款项目",
        "businessCategory": "示例业务分类B",
        "loanBalance": "38000000.00",
        "managerName": "张三",
        "department": "示例业务处",
        "loanType": null,
        "selectable": true,
        "issues": []
      }
    ],
    "warnings": []
  },
  "requestId": "req_query_example"
}
```

- 无匹配结果仍返回 HTTP 201，`projectCount=0`、`projects=[]`，前端提示“未找到项目”。
- `projectId` 只在对应 `queryId` 中使用，重新查询后不可复用旧 ID。
- `selectable=false` 表示项目存在阻断问题；`issues` 为 `{code, message}` 数组，前端展示原因并禁选，后端仍需校验。
- 可定位到项目的分类冲突等问题可列在 `issues`；无法可靠解析项目身份或文件结构时整个请求失败，不能静默漏项目。
- 顶层 `warnings` 为 `{code, message}` 数组，用于非阻断提示；未提供可选字段不冒充数据错误。

## 6. 生成底表

`POST /ledgers`

```json
{
  "queryId": "qry_example",
  "projectIds": ["prj_fund_example", "prj_loan_example"],
  "year": 2026
}
```

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| queryId | string | 必填，有效的查询快照 ID |
| projectIds | string[] | 必填，至少一项，不允许重复，必须全部属于该快照且可选 |
| year | integer | 必填，首版建议允许 2000～2100；用于表头和后续年度筛选 |

成功：HTTP 201。

```json
{
  "code": "OK",
  "message": "底表生成成功",
  "data": {
    "ledgerId": "ledger_example",
    "status": "TEMPLATE_READY",
    "year": 2026,
    "projectCount": 2,
    "fundProjectCount": 1,
    "loanProjectCount": 1,
    "currency": "CNY",
    "amountUnit": "WAN_YUAN",
    "templateDownloadUrl": "/api/v1/ledgers/ledger_example/file?stage=template",
    "warnings": []
  },
  "requestId": "req_generate_example"
}
```

生成时校验余额表指纹。源文件变化返回 409，提示重新查询和勾选；不直接用变化后的余额替换用户确认的数据。创建成功后，台账保留自己的基础资料快照，不依赖原查询快照继续存活。

每次成功调用创建独立 `ledgerId`。首版前端应防止重复点击；网络超时后重复提交可能创建另一份台账，不宣称此接口幂等。

## 7. 填入业务数据

`POST /ledgers/{ledgerId}/populate`

不需要请求体。年份、项目范围从该台账读取，数据源路径由后端配置管理。

成功：HTTP 200。

```json
{
  "code": "OK",
  "message": "台账数据填充完成",
  "data": {
    "ledgerId": "ledger_example",
    "status": "COMPLETED",
    "year": 2026,
    "projectCount": 2,
    "completedAt": "2026-10-19T21:05:00+08:00",
    "completedDownloadUrl": "/api/v1/ledgers/ledger_example/file?stage=completed",
    "warnings": []
  },
  "requestId": "req_populate_example"
}
```

- 同一台账的填数串行处理；已有请求在执行时返回 409 `LEDGER_BUSY`。
- 从保存的底表重建完成版，同样的输入数据应产生相同的业务数值，不做累加式更新。
- 重跑会使用本次读取的业务明细，保留源文件指纹及运行时间；源数据改变时结果可以改变。
- 原始底表保留；只有完成版成功写入后才发布。重跑失败时保留上一次完成版及其原完成时间，不把旧文件标为本次成功。
- 各明细源在一次处理中使用一致的已读取快照；检测到读取过程中源文件变化时失败并要求重试。
- 是否把无匹配记录视为 0、哪些情况阻断生成，需要按照第 2 节确认后的规则实现。不能通过返回成功掩盖文件缺失、表头错误或项目匹配歧义。

## 8. 下载台账

`GET /ledgers/{ledgerId}/file?stage=template` 或 `stage=completed`

成功：HTTP 200。

```http
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="ledger-2026.xlsx"; filename*=UTF-8''...
```

响应体是文件二进制。`filename*` 的实际值为 UTF-8 百分号编码的下载文件名，示例省略编码内容。

- `template` 下载生成时的原始底表，`completed` 下载最近一次成功填数的版本。
- 尚未完成填数就下载完成版：409 `LEDGER_NOT_COMPLETED`。
- 服务器通过 `ledgerId` 查找文件，客户端不能传入任意本地路径或文件名。
- 首版台账文件建议保留至手动清理；进程重启后元数据是否恢复须明确实现。可在每个台账目录保存元数据文件，重启后重新加载。不存在或已清理的台账返回 404。

## 9. 错误响应

```json
{
  "code": "SOURCE_CHANGED",
  "message": "余额表已更新，请重新查询并选择项目",
  "data": null,
  "requestId": "req_error_example"
}
```

| HTTP 状态 | code | 场景 |
| --- | --- | --- |
| 400 | INVALID_ARGUMENT | 姓名为空、年份越界、项目列表为空／重复、stage 非法 |
| 404 | QUERY_NOT_FOUND | 查询 ID 不存在，或应用重启后内存快照丢失 |
| 410 | QUERY_EXPIRED | 快照已过期；清理后无法识别曾存在的 ID 时可返回 404 |
| 409 | SOURCE_CHANGED | 用户确认前余额表已变化，或读取期间文件发生变化 |
| 422 | INVALID_PROJECT_SELECTION | ID 不属于该快照，或项目被标记为不可选 |
| 422 | SOURCE_DATA_INVALID | 必需表头缺失、非法数值或无法可靠解析数据 |
| 422 | AMBIGUOUS_PROJECT | 项目与明细存在无法消除的匹配歧义 |
| 404 | LEDGER_NOT_FOUND | 台账不存在或已清理 |
| 409 | LEDGER_BUSY | 同一台账正在填数 |
| 409 | LEDGER_NOT_COMPLETED | 请求下载尚未生成的完成版 |
| 503 | SOURCE_UNAVAILABLE | 配置的数据源文件缺失、不可读或被独占锁定 |
| 500 | LEDGER_GENERATION_FAILED | 底表／完成版写入失败 |
| 500 | INTERNAL_ERROR | 未预期的服务端异常 |

错误明细写服务端日志，响应不暴露堆栈和本机绝对路径。前端通过 `requestId` 协助定位。

## 10. 建议的代码职责

| 组件 | 责任 |
| --- | --- |
| ProjectQueryController | 接收经理查询参数，返回项目列表 |
| LedgerController | 生成、填数、下载接口 |
| BalanceProjectService | 读取两类余额、筛选、项目分组及冲突检查 |
| ProjectQueryStore | 查询快照、候选 ID、有效期与容量限制 |
| LedgerService | 编排生成和填数流程，维护台账状态 |
| LedgerTemplateService | 布局、样式、动态合并、小结与公式 |
| LedgerPopulationService | 按已确认规则汇总并填入业务数据 |
| LedgerFileStore | 独立目录、元数据、临时文件及下载定位 |
| GlobalExceptionHandler | 参数及业务异常转换为统一 HTTP／JSON 响应 |

DTO 与 EasyExcel 行实体分开。先按职责拆分普通类，需要替换实现的地方再引入接口。现有 Reader 和金额汇总逻辑可以复用，旧 Controller 的直接串行覆盖写入方式不作为新流程的业务规则。

## 11. Postman 联调顺序

建立集合变量：

| 变量 | 初始值 |
| --- | --- |
| baseUrl | `http://localhost:8080` |
| managerName | 实际要查询的经理姓名 |
| queryId | 留空，由查询响应取得 |
| projectIds | 留空，手动选取响应中的项目 ID，保存为 JSON 数组文本 |
| year | `2026` |
| ledgerId | 留空，由生成响应取得 |

1. **查询**：`POST {{baseUrl}}/api/v1/project-queries`，Body → raw → JSON：

   ```json
   { "managerName": "{{managerName}}" }
   ```

   在响应中核对项目，再保存 `queryId`。可以在 Post-response 中执行：

   ```javascript
   pm.test("查询成功", () => pm.response.to.have.status(201));
   if (pm.response.code === 201) {
     const body = pm.response.json();
     pm.collectionVariables.set("queryId", body.data.queryId);
   }
   ```

2. **模拟勾选**：手动把想要的项目 ID 写入集合变量 `projectIds`，例如 `["prj_fund_example","prj_loan_example"]`，实际使用响应里的 ID，不自动全选。

3. **生成**：`POST {{baseUrl}}/api/v1/ledgers`：

   ```json
   {
     "queryId": "{{queryId}}",
     "projectIds": {{projectIds}},
     "year": {{year}}
   }
   ```

   `projectIds` 和 `year` 外面不能再套引号。Post-response：

   ```javascript
   pm.test("底表生成成功", () => pm.response.to.have.status(201));
   if (pm.response.code === 201) {
     pm.collectionVariables.set("ledgerId", pm.response.json().data.ledgerId);
   }
   ```

4. **下载底表**：`GET {{baseUrl}}/api/v1/ledgers/{{ledgerId}}/file?stage=template`，保存响应为 `.xlsx`，在 Excel 中检查三层表头、勾选项目、客户合并及小结公式。
5. **填数**：`POST {{baseUrl}}/api/v1/ledgers/{{ledgerId}}/populate`，Body 选择 none，检查返回状态及 warnings。
6. **下载完成版**：`GET {{baseUrl}}/api/v1/ledgers/{{ledgerId}}/file?stage=completed`，核对手算金额与公式。

错误用例至少覆盖：姓名空白、无结果、只选基金、只选贷款、跨客户多选、重复 ID、其他快照的 ID、快照过期、余额表被更新、源文件缺失、同名项目歧义、填数前下载、连续两次填数。

## 12. 第一轮实现建议

先完成“读取两份余额表＋按经理查询候选项目”，在 Postman 中核对项目列表与余额。确认分组和多行余额规则后，再实现底表生成。最后确定回收口径并迁移填数逻辑，避免把现有覆盖问题带入新接口。
