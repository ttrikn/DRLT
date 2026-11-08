# 业务台账助手

基于 Spring Boot 的 Excel 业务台账工具，用于汇总贷款及基金业务数据，减少人工筛选项目、统计合同余额和填写月度台账的重复工作。

当前已完成两部分：**向已有台账填入数据**，以及**通过网页查询客户经理名下的贷款项目**。查询、勾选、生成底表、填数、下载这一完整流程尚未打通。

## 已实现功能

### 贷款项目查询与展示

- 输入客户经理姓名和年度，从发放、回收、余额三张表获取项目并取并集。
- 发放、回收按请求年度筛选；余额表使用当前存量快照，因此可保留本年无资金发生的存量项目，也能通过回收记录找到已结清项目。
- 发放表按“客户经理”匹配，回收及余额表仅按“项目经办经理A”匹配，不使用 B 角。姓名去除首尾空白后精确匹配。
- 优先按项目号合并多合同、跨表记录；同一项目号客户相同但名称不同时，名称优先级为发放表 → 回收表 → 余额表。同一来源内取首条名称。不同客户对应相同项目号时仍报错。
- 缺少项目号时使用客户名称和项目名称匹配；无法唯一对应时明确报错。
- 发放日期与金额同时为空的记录跳过；有金额但没有日期仍报错。
- 网页展示序号、项目号、客户名称、项目名称、客户经理、客户处；包含加载、空结果和失败提示。
- 客户处取发放表“处室名称”和回收、余额表“A所属处室”，多个值去重后用分号连接，缺失显示 `-`。
- 返回查询 ID、项目选择 ID；服务端暂存查询快照 30 分钟，最多保留最近 100 次查询，重启或淘汰后失效。已有内部勾选校验方法，尚未开放勾选提交接口。

### 已有台账数据填充

- 按项目、月份统计发放、贷款回收、基金回收及计划回收数据，写入已有输出工作簿。
- 人民币贷款余额按项目汇总每条余额明细，支持同一项目多个合同。
- 从结息表识别六类基金产品，以本金余额汇总基金余额。
- 从结息表提取贷款及基金当前执行利率，数值去重；多利率以中文分号连接。
- 向台账写入业务分类、余额、利率；缺失值写 `-`，真实零值保留为零。
- 余额及月度金额从元转换为万元。基础信息写入保留原单元格样式，仅调整数值格式，并跳过项目名称列内的合并表头、小结。

基金余额产品编码：`J0120001`、`K0820001`、`J0122001`、`K0822001`、`J0125001`、`K0825001`，对应发展基金、基础设施基金、新型政策性金融工具的股权投资及股东借款。基金回收目前另按三类股东借款产品名称筛选，两者规则不同。

## 技术栈与结构

| 部分 | 当前实现 |
| --- | --- |
| 后端 | Java 17、Spring Boot 4.1.0、Spring MVC |
| Excel | EasyExcel 3.3.4 读取、Apache POI 写入 |
| 前端 | 原生 HTML、CSS、JavaScript，由 Spring Boot 同源提供 |
| 构建与测试 | Maven、JUnit、MockMvc |
| 存储 | 本地 Excel、进程内查询快照；尚无数据库 |

```text
src/main/java/com/example/demo/
├── config/          ExcelProperties：文件路径与台账布局
├── controller/      项目查询、已有台账填充接口
├── dto/             查询请求与响应
├── entity/          Excel 行实体、项目基础信息
└── service/         查询、聚合、计算、读取和写入
    └── impl/        Reader、公用读取基类及各处理实现
src/main/resources/
├── application.properties
└── static/          index.html、app.css、app.js
src/test/java/       单元测试、接口测试、可选真实文件测试
docs/                接口说明、底表流程草案、代码 review
```

项目查询和填表使用独立 Controller。发放、回收、余额共享原有实体及 Reader，`AbstractExcelDataReader` 复用读取和表头校验；`LoanProjectReader` 负责查询筛选和转换；`ProjectQueryService` 负责合并及快照管理。`ExcelProcessServiceImpl` 保留统一的月度金额处理流程。

## 本地运行

1. 安装 JDK 17 和 Maven，确认 `java -version`、`mvn -version` 正常。
2. 修改 `src/main/resources/application.properties` 中的文件路径。仓库当前配置是开发机器的绝对路径，其他机器必须调整。
3. 在项目根目录执行：

```shell
mvn spring-boot:run
```

打开 <http://localhost:8080/>，输入姓名和年度即可查询。无需 npm 或独立前端服务。原演示欢迎接口位于 `/hello`。

也可以打包运行：

```shell
mvn clean package
java -jar target/demo1-0.0.1-SNAPSHOT.jar
```

### 文件配置

以下为示例路径，需替换为实际文件位置：

```properties
excel.source.dispatch.path=C:/ledger/data/dispatchdata.xlsx
excel.source.recycle.path=C:/ledger/data/recycledata.xlsx
excel.source.schedule.path=C:/ledger/data/scheduledata.xlsx
excel.source.fundRecycle.path=C:/ledger/data/fundrecycledata.xlsx
excel.source.surplus.path=C:/ledger/data/surplusdata_1.xlsx
excel.source.interest.path=C:/ledger/data/interest.xlsx
excel.target.path=C:/ledger/data/output.xlsx
```

配置绑定要求六个数据源路径和目标路径均非空，但不在启动时检查文件存在。仅调用查询时读取发放、回收、余额三表。结息读取固定名为 `Sheet1` 的工作表，其他源表读取第一张表，第一行为表头。

填表要求目标文件已存在且布局匹配。当前零基索引：项目名称 C 列为 2、业务分类 E 列为 4、利率 F 列为 5、余额 H 列为 7。`amount-start-col-index=4` 当前还被用于推导发放起始 I 列（+4）及回收起始 V 列（+17），不是独立月度起始列配置。

**旧填表接口会覆盖配置指向的输出文件，使用前应保留副本并关闭 Excel 对该文件的占用。网页查询不会修改 Excel。**

## 接口

| 方法 | 地址 | 当前行为 |
| --- | --- | --- |
| POST | `/api/v1/project-queries` | 查询贷款候选项目，不修改文件 |
| GET | `/excel/process/dispatch` | 向已有台账写入月度发放金额 |
| GET | `/excel/process/recycle` | 依次处理贷款回收、基金回收、计划回收 |
| GET | `/excel/process/baseInfo` | 写入业务分类、贷款/基金余额、利率 |

Postman 查询示例：Body 选择 raw / JSON，Content-Type 为 application/json。

```json
{
  "managerName": "客户经理姓名",
  "year": 2026
}
```

响应包含 `queryId`、`expiresAt`、`managerName`、`year`、`projectType`、`total` 和 `projects`。项目包含 `selectionId`、`projectCode`、`customerName`、`projectName`、`managerName`、`department`、`sources`。无匹配项目返回空数组；参数错误为 400，源表读取或身份冲突为 422。

旧填表接口尚未采用这一错误协议：捕获异常后返回字符串，HTTP 状态仍可能为 200。详情见 [项目查询接口](docs/project-query-api.md)。

## 测试

```shell
mvn test
```

默认测试使用临时 Excel 和模拟请求，覆盖合同余额汇总、六类基金、利率去重、缺失值、格式保护、查询合并、经理 A、客户处及名称优先级等。`ExcelTest.main` 是早期手工调试入口，不是自动测试。

真实项目查询测试需本机具备测试类中约定路径的文件，显式启用：

```shell
mvn "-Dtest=ProjectQueryTests" "-Dproject.query.real=true" "-Dproject.query.manager=客户经理姓名" test
```

基础信息真实文件测试通过 `baseinfo.source`、`baseinfo.interest`、`baseinfo.target` 三个系统属性指定文件；该测试复制目标到临时目录后操作。默认不启用两类真实文件测试。现有测试尚不能证明所有月度金额与计划回收组合的正确性。

## 已知限制与优化重点

1. **项目身份尚未贯穿填表。** 查询按项目号合并，但金额、基础信息填表仍按项目名称匹配。同一项目跨表异名会漏匹配，不同客户同名项目可能被错误汇总。应先统一内部项目标识并保留源表名称对应关系，再接入生成流程。
2. **填表年度写死为 2026。** 网页年度目前只影响查询，不能控制旧填表接口。
3. **回收多次写入存在覆盖。** 实际回收、基金回收、计划回收写同一区域；同项目同月份重叠时后写值覆盖前值。需要明确实际与计划的业务口径后统一计算、一次写入。
4. **重复执行可能保留旧月度数据。** 未出现在新数据中的项目/月不会清除旧值，应按完整目标区间重算并验证重复执行结果一致。
5. **文件写入保护不统一。** 基础信息使用临时文件替换，月度写入直接覆盖；多请求同时处理同一目标还可能丢失更新。应每次任务生成独立文件，并统一提交策略。
6. **读取校验不完整。** 查询有必需表头校验，但发放/回收的默认填表读取未声明必需列；计划表字段级日期转换还需补充 Excel 数值日期覆盖测试。
7. **工程维护。** 月度金额仍为 Double；建议改为 BigDecimal 并明确舍入规则。固定 Lombok 版本、统一构造器注入与日志、清理早期示例代码、格式化单行 CSS。
8. **部署范围。** 当前为本地工具，没有用户认证、文件上传、任务隔离或多实例共享快照；不应视为已经具备多人生产部署能力。

完整 review 及代码定位见 [代码 review](docs/code-review-2026-11-08.md)。

## 后续开发计划

### 第一阶段：稳定数据处理

- 统一项目号、客户和名称别名模型，贯穿查询、聚合、填表。
- 统一年度参数，确认实际回收与计划回收口径。
- 完善日期和必需列校验、全月份重算、重复执行及异常写入测试。
- 统一 JSON 错误响应，把修改文件的接口改为 POST。

### 第二阶段：完成贷款台账流程

- 前端支持项目勾选、全选和选择数量展示。
- 根据 queryId、selectionId 在后端校验勾选结果。
- 根据所选项目生成底表：标题、客户分组、合并单元格、样式、月度列、小计和合计公式。
- 在新生成的底表中填入基础信息及月度数据，提供下载与处理结果说明。
- 保留源表和模板，按任务输出独立文件。

### 第三阶段：扩展与体验

- 在经理分配规则确认后加入基金项目查询、选择及制表。
- 增加输入文件上传、模板选择与数据校验报告。
- 依据实际数据量引入缓存、后台任务、进度提示；需要多人使用时再加入认证与任务归属管理。
- 补充前端自动化测试和 CI。

[底表接口设计草案](docs/ledger-api-design.md) 是规划材料，其中尚未实现的接口不能直接调用；当前可用行为以上述接口说明及代码为准。
