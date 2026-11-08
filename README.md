# 业务台账助手

一个基于 Spring Boot 的 Excel 台账处理工具，用于查询客户经理名下的项目、汇总业务数据，逐步实现台账自动生成。

## 已实现

- 通过网页查询贷款项目，合并发放、回收和余额表中的记录。
- 展示项目号、客户名称、项目名称、客户经理和客户处。
- 向已有台账填写业务分类、贷款及基金余额、执行利率和月度发放回收数据。

## 技术栈

Java 17 · Spring Boot · Maven · EasyExcel / Apache POI · HTML / CSS / JavaScript

## 运行方式

修改 `src/main/resources/application.properties` 中的 Excel 文件路径，然后启动：

```shell
mvn spring-boot:run
```

浏览器访问 <http://localhost:8080/>，无需单独启动前端。查询不会修改文件，填表功能会更新配置中的输出文件。

## 后续计划

- 完善项目匹配、年度筛选和文件写入逻辑。
- 支持前端勾选项目，自动生成带格式和公式的台账底表。
- 打通数据填充与文件下载流程。
- 扩展基金项目查询及制表功能。

详细说明见 [查询接口文档](docs/project-query-api.md) 和 [代码 review](docs/code-review-2026-11-08.md)。
