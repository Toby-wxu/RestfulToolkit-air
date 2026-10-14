# RestfulToolkit-air

> 一套 IntelliJ IDEA 的 RESTful 服务开发辅助工具集 —— RestfulToolkit 的维护分支，
> 支持 IntelliJ IDEA 2025.1 ~ 2026.2，并修复了原版在新版本 IDE 上的不兼容与崩溃问题。

原项目：[mrmanzhaow/RestfulToolkit](https://github.com/mrmanzhaow/RestfulToolkit)

## 目录

- [兼容性](#兼容性)
  - [版本号规则](#版本号规则)
- [安装](#安装)
- [功能一览](#功能一览)
- [详细功能](#详细功能)
  - [1. 按 URL 导航到方法定义](#1-按-url-导航到方法定义go-to-service)
  - [2. Services Tree 工具窗口](#2-services-tree-工具窗口)
  - [3. 内置 HTTP 请求工具](#3-内置-http-请求工具)
  - [4. Controller 方法上的复制生成动作](#4-controller-方法上的复制生成动作)
  - [5. 实体类转换与 JSON 格式化](#5-实体类转换与-json-格式化)
- [框架与语言支持](#框架与语言支持)
- [端口与上下文路径的读取规则](#端口与上下文路径的读取规则)
- [已知限制](#已知限制)
- [与上游的差异](#与上游的差异)
- [本地开发与构建](#本地开发与构建)
- [项目结构](#项目结构)
- [许可证与致谢](#许可证与致谢)

## 兼容性

| 项目 | 要求 |
| --- | --- |
| IDE | IntelliJ IDEA 2025.1 (build 251) ~ 2026.2 (build 262.\*)，Ultimate / Community 均可 |
| JDK | 构建与运行需 **JDK 21**（由 `gradle/gradle-daemon-jvm.properties` 按版本锁定，不绑定本机路径） |
| Gradle | Wrapper 9.0.0 |
| 插件平台 | JetBrains IntelliJ Platform Gradle Plugin 2.13.0 |
| 依赖插件 | Java、Kotlin、Properties、YAML（IDEA 内置，无需额外安装） |

插件 ID `com.fengjianliang.restful.toolkit`，当前版本 `1.0.0`。Java 源码包名沿用上游的 `com.zhaow.*`。

### 版本号规则

本插件自 `1.0.0` 起使用**独立版本线**，不再跟随上游的 2.x 编号，按语义化版本递增：

| 位 | 何时递增 |
| --- | --- |
| 主版本（`1.x.x`） | 不兼容变更：适配新的 IDE 平台大版本线、移除或重做现有功能 |
| 次版本（`x.1.x`） | 向后兼容的新功能：新的注解识别、新的生成动作 |
| 修订号（`x.x.1`） | 向后兼容的问题修复 |

`1.0.0` 对应上游 RestfulToolkit `2.1.6`（及其修复分支）之后的维护版本；`plugin.xml` 的 `<change-notes>`
保留了上游 1.0.3 ~ 2.1.6 的完整原始记录，本分支的改动从 1.0.0 起单独记账。

## 安装

**方式一：插件市场**

`Settings/Preferences ▸ Plugins ▸ Marketplace` 搜索 **RestfulToolkit-air**，安装后重启 IDE。
（市场页面地址待上架后补充）

**方式二：本地构建后安装**

```bash
./gradlew buildPlugin          # 产物在 build/distributions/RestfulToolkit-air-1.0.0.zip
```

然后 `Settings ▸ Plugins ▸ ⚙ ▸ Install Plugin from Disk...` 选择该 zip。

**方式三：源码直接调试**

```bash
./gradlew runIde               # 启动带本插件的沙箱 IDE
```

## 功能一览

| 功能 | 入口 | 快捷键 |
| --- | --- | --- |
| 按 URL 跳转到方法定义 | `Navigate ▸ Go to ▸ Service` | `Ctrl + \` 或 `Ctrl + Alt + N` |
| Services Tree 工具窗口 | 右侧工具窗口条 `RestfulToolkit` | — |
| RESTful 动作快捷面板 | `Edit ▸ Popup RESTful Toolkit ...` | `Alt + Shift + R` |
| 复制完整 URL / 相对 URL / 请求体 JSON / 查询参数 | 编辑器右键 ▸ `RestfulToolkit` 子菜单 | — |
| Convert to JSON / (Compressed) / Bulk Value | 编辑器右键 | — |
| 格式化 JSON | 服务详情面板文本区 | `Ctrl + Enter`（macOS `Command + Enter`） |

## 详细功能

### 1. 按 URL 导航到方法定义（Go to Service）

按 `Ctrl + \`（或 `Ctrl + Alt + N`）打开搜索框，输入 URL 即可直达对应的 Controller 方法。

- **剪贴板自动填充**：编辑器内无选区时，若剪贴板内容以 `http` 开头，会自动截取路径部分填入搜索框（最长 120 字符）。
- **自动清洗输入**：粘贴完整地址时会自动去掉协议、host、端口和 `?` 之后的查询串，只保留路径参与匹配。
- **Ant 风格路径匹配**：声明式路径里的占位符可直接命中真实 URL —— `/product/{productId}` 匹配 `/product/42`；
  `*` 匹配单段内任意字符，`**` 跨段匹配，还支持 `{name:[a-z-]+}` 这种内嵌正则（由内置的 `AntPathMatcher` 提供）。
- **模糊匹配**：同时使用 IDE 的 `MinusculeMatcher` 做大小写不敏感的子串/缩写匹配，输入 `prodlist` 也能找到 `/product/list`。
- **HTTP 方法过滤**：搜索框下方提供方法过滤标签（GET/POST/…），隐藏与高亮状态按项目持久化在 `.idea/workspace.xml`。
- **Only This Module**：勾选后只在当前模块范围内搜索，该选项同样会被记住。

### 2. Services Tree 工具窗口

IDE 右侧新增 `RestfulToolkit` 工具窗口，展示当前项目全部 REST 接口。

- **树结构**：`Found N services` 根节点 ▸ 模块 ▸ 接口（HTTP 方法图标 + URL），叶子节点 tooltip 显示 `Class#method`。
- **扫描在后台线程执行**（`ReadAction.nonBlocking` + Smart Mode），不会阻塞 EDT；节点采用 `AsyncTreeModel` 懒加载。
- **工具栏**：`Refresh Services in Project`、`Go to Service`。
- **交互**：单击选中并在下方展示详情；双击/回车跳转到源码；右键菜单提供 `Copy Full Url`（多选时以空行分隔）和 `Jump to Source`。
- **自动刷新**：工具窗口从隐藏状态重新可见时会重新扫描；选中项对应的 PSI 元素已失效时也会自动重建树。
- **上下分栏**：上半部分是服务树，下半部分是请求详情面板（可编辑 HTTP 方法、URL、参数、请求体、响应）。

### 3. 内置 HTTP 请求工具

在工具窗口里选中一个接口，直接在详情面板发起请求：

- 支持 `GET / POST / PUT / DELETE / HEAD / OPTIONS / TRACE / PATCH`。
- `RequestParams` 里的参数会替换 URL 中的 `{pathVar}` 占位符，剩余参数自动拼接为查询串。
- `RequestBody` 页签非空时以 `Content-Type: application/json; charset=utf-8` 发送。
- **中文不乱码**：响应体按 `byte[]` 读取，再从 `Content-Type` 解析字符集（容错处理 `charset=`/`chars=`），解析不到或声明非法时回退 UTF-8。
- **非 JSON 响应原样展示**：合法 JSON 会美化缩进，HTML/纯文本/错误页保留原始内容；空响应显示 `200 GET (empty response)`。
- 超时保护：连接 5s、单请求 30s，请求期间不阻塞界面且可取消。
- 双击任一文本面板可复制其全部内容；`Ctrl/Cmd + Enter` 格式化 JSON。

### 4. Controller 方法上的复制生成动作

光标位于 Spring / JAX-RS 接口方法内时，编辑器右键菜单 ▸ `RestfulToolkit` 子菜单（或 `Alt + Shift + R`）提供四个动作，执行后写入剪贴板并弹出提示。

假设存在下面这个方法：

```java
@RestController
@RequestMapping("/product")
public class ProductController {

    @GetMapping({"/list", "/query"})
    public Product list(@RequestParam Integer id,
                        @RequestParam String name,
                        @RequestBody ProductForm form) { ... }
}
```

| 动作 | 输出内容 |
| --- | --- |
| **Generate && Copy Full URL** | `http://localhost:8080/product/list?id=1&name=demoData` —— 带 host:port + 上下文路径的绝对地址 |
| **Generate && Copy Relation URL** | `/product/list?id=1&name=demoData` 与 `/product/query?id=1&name=demoData`，**多路径按行分隔全部输出**（去重） |
| **Generate && Copy RequestBody (JSON)** | 由 `@RequestBody` 参数类型生成的 JSON 串（美化缩进）；方法没有该参数时提示"该方法没有 @RequestBody 参数" |
| **Generate && Copy Query Param (Key Value)** | `id=1&name=demoData&ok=true&date=2026-10-14 10:32:11` |

关于**多路径映射**：`@RequestMapping({"/a", "/b"})`、类级路径 × 方法级的组合都会展开；`Full URL` 只取第一条路径，`Relation URL` 会把所有组合逐行列出，便于一次复制多个环境的地址。
`@RequestMapping(params = "source=web")` 这类条件参数也会拼接成查询串出现在生成结果里。

识别到的方法参数注解为 `@PathVariable`、`@RequestParam`、`@RequestBody`；未标注解的参数按参数名纳入；
`HttpServletRequest / HttpServletResponse / HttpSession`（javax 与 jakarta 两种包名）会被自动跳过。

### 5. 实体类转换与 JSON 格式化

- **Convert to JSON**：在类文件里直接生成该 POJO 的 JSON 结构，用于快速构造测试数据。字段取值规则：
  - 基础类型/包装类/String/`BigDecimal` → 类型默认值，字符串为 `demoData`；
  - `Date` / `LocalDateTime` → 当前时间，格式 `yyyy-MM-dd HH:mm:ss`；
  - 枚举 → 第一个常量；数组 → 含一个元素的数组；
  - `List<T>` 泛型已完善：`List<String>` 生成 `["demoData"]`，`List<Entity>` 生成对应对象；
  - **自关联类型**只再展开一层即停止，不会无限递归或栈溢出。
- **Convert to JSON (Compressed)**：同上，输出单行紧凑 JSON。
- **Convert to Bulk Value**：按 `field: 默认值` 逐行输出（CRLF 分隔），会遍历整条父类链并跳过 `serialVersionUID`，适合填批量插入语句或造数脚本。
- Java 类与 Kotlin 类（`KtClassOrObject`）都支持。

## 框架与语言支持

| 体系 | 已支持的注解 |
| --- | --- |
| Spring MVC / Boot | `@Controller`、`@RestController`；`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping` |
| JAX-RS | `javax.ws.rs.Path` 及 `@GET` / `@POST` / `@PUT` / `@DELETE` / `@HEAD` / `@PATCH` |
| 语言 | Java、Kotlin（Kotlin 控制器的 `arrayOf(...)`、`[...]` 多路径注解同样可解析） |

类级 `@RequestMapping` 作为基础路径参与拼接，且会沿父类链继承；无类级注解时按 `/` 处理，路径重复斜杠会被规范化。

## 端口与上下文路径

生成完整 URL 时的 host 前缀来自项目配置文件，规则如下：

- **读取项**：仅 `server.port` 与 `server.context-path`，缺省回退 `http://localhost:8080`。
- **扫描范围**：当前模块内名为 `application` / `bootstrap` 的 `.properties` / `.yml` / `.yaml`，
  优先级 `application.properties > application.yml > application.yaml > bootstrap.*`。
- **Profile 回落**：先读 `spring.profiles.active` 指向的 `application-<profile>.<ext>`，该文件中缺失的键回落到基础配置。
- **占位符**：`${VAR:default}` 取 `default` 值。
- **YAML 多文档**：支持 `---` 分段，逐段解析并展平为点号键（列表展平为 `[i]`），**后面的文档覆盖前面的**；单段解析失败只告警，不影响其余分段。
- 解析结果按文件缓存，不会在每次生成 URL 时重复读盘。

## 已知限制

以下行为是当前实现的边界，使用时请注意：

- 仅读取 `server.port` / `server.context-path`，**不识别** `server.servlet.context-path` 和 `server.address`（Spring Boot 2 以上的常见写法），需要时请自行改用旧键名。
- 不识别 `@RequestHeader`、`@MatrixVariable` 及 JAX-RS 的 `@QueryParam`，这些参数不会进入生成结果。
- 请求工具**没有自定义 Header 编辑界面**，只能改方法、URL、参数和 JSON 请求体。
- JAX-RS 仅覆盖 `javax.ws.rs`，`jakarta.ws.rs` 注解暂时无法识别。
- POJO 转 JSON 时 `List<实体>` 输出的是单个对象而非长度为 1 的数组（`@RequestBody List<X>` 参数则正确输出数组）；多层嵌套泛型未处理。
- Ant 风格匹配只用于"按 URL 导航"方向，生成的 URL 不做反向通配。
- YAML 多文档中的 `spring.config.activate.on-profile` 条件选择器不参与判断。
- 相比上游，本分支**移除了** JSON→POJO 反向生成、Live Template、Spring Boot 代码生成器、Kotlin 类转 JSON 独立动作等模块，聚焦在路由解析与 URL 生成主链路上。

## 与上游的差异

- 迁移到 IntelliJ Platform Gradle Plugin 2.x + Gradle 9 + JDK 21，适配 IDEA 2025.1 ~ 2026.2 的新 API，原版在新 IDE 上的不兼容报错已消除。
- 组件注册改为 `@Service` 注解与 `projectService`，替换已弃用的 `project-components` / `application-components`。
- 服务树扫描从 EDT 移到后台 ReadAction；修复 Spring / Kotlin 解析过程中的 `ClassCastException`、`IndexOutOfBoundsException`、NPE。
- 补齐并修复 Kotlin Spring 路由解析，兼容新版 Kotlin PSI API。
- 支持一个映射注解声明多路径的 URL 生成与逐行复制。
- 修复 `@RequestMapping(params = ...)` 条件参数未参与 URL 生成的问题。
- 响应改为按字节读取并解析字符集，修复中文乱码；非 JSON 响应内容不再被丢弃。
- YAML 配置读取支持 `---` 多文档分段。
- 完善 `List` 泛型对象解析，补齐请求体 JSON 组装逻辑。
- HTTP 请求增加连接/读取超时；请求参数面板适配深色主题。

## 本地开发与构建

```bash
# 环境要求：JDK 21（本机路径不要写死到构建脚本里）
./gradlew runIde        # 启动沙箱 IDE 调试插件
./gradlew test          # 运行单元测试
./gradlew buildPlugin   # 打包 zip 到 build/distributions/
./gradlew verifyPlugin  # 校验 plugin.xml 兼容性
```

单元测试为纯逻辑测试（不依赖 IDE 运行期），覆盖 Ant 路径匹配、`RequestPath` 路径拼接、URL 输入清洗等：

```
src/test/java/com/zhaow/restful/common/spring/AntPathMatcherTest
src/test/java/com/zhaow/restful/method/RequestPathTest
src/test/java/com/zhaow/utils/ToolkitUtilTest
```

首次构建会下载 IntelliJ IDEA 发行版到 `.intellijPlatform/`，体积较大；本地机器相关的 IDE/JDK 路径配置放在 `local.properties`，已被 git 忽略，不要提交。

## 项目结构

```
src/main/java/com/zhaow/
├── restful/
│   ├── action/           # 实体类转 JSON / 压缩 JSON / Bulk Value
│   ├── annotations/      # Spring、JAX-RS 注解常量定义
│   ├── common/
│   │   ├── jaxrs/        # JAX-RS 注解解析
│   │   ├── resolver/     # 服务解析器（SpringResolver / JaxrsResolver）
│   │   └── spring/       # @RequestMapping 解析、AntPathMatcher
│   ├── method/           # URL/参数模型与四个"复制生成"动作
│   ├── navigation/       # Go to Service 导航与过滤
│   ├── navigator/        # Services Tree 工具窗口与请求面板
│   └── popup/action/     # Alt+Shift+R 快捷动作面板
└── utils/                # JSON、资源 bundle、URL 工具
src/main/resources/META-INF/plugin.xml   # 扩展点、动作与快捷键注册
```

## 许可证与致谢

本项目基于 [Apache-2.0 LICENSE](LICENSE) 发布，沿用上游 [RestfulToolkit](https://github.com/mrmanzhaow/RestfulToolkit)
（作者 [mrmanzhaow](https://github.com/mrmanzhaow)）的代码与授权条款，并经其修复分支 RestfulToolkit-fix 演进而来。

按 Apache-2.0 的要求，第三方来源文件的版权声明与作者署名**全部原样保留**，未被改写或删除：

| 文件 | 来源 | 保留内容 |
| --- | --- | --- |
| `restful/common/spring/AntPathMatcher.java` | Spring Framework | `Copyright 2002-2017 the original author or authors` + Apache 2.0 头 + `@author` 列表（Arendsen / Hoeller / Harrop / Poutsma / Stoyanchev / Brannen） |
| `restful/common/spring/StringUtils.java` | Spring Framework | 同上 |
| `restful/navigation/action/RestServiceChooseByNamePopup.java`、`restful/navigator/RestServiceProjectsManager.java` | JetBrains s.r.o. | `Copyright 2000-2017 JetBrains s.r.o.` + Apache 2.0 头 |

源码 Java 包名沿用上游的 `com.zhaow.*`，未做重命名（重命名会破坏与上游的对照，也与上述署名无关）。
`plugin.xml` 中的 `<vendor>` 已改为本项目维护者信息，因为 Marketplace 的发布者必须与实际上传账号一致；
上游的 `<change-notes>` 版本历史（1.0.3 ~ 2.1.6）完整保留，本分支的改动单独记为 1.0.0（独立版本线，见「版本号规则」）。
