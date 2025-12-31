## 项目说明：bv 埋点 / 日志采集 SDK

本项目按照 `DEMAND.md` 的要求，实现了：

- 后端：基于 Spring Boot（JDK 21、Maven）的简易日志接收服务
  - 提供 `POST /log/upload` 接口，接收安卓 SDK 上报的日志文本
  - 将每次请求 body 原样追加写入本地按日期分文件的 `.txt` 日志中
- 安卓端：Java 实现的 Android SDK（兼容 Java / Kotlin）
  - 方案 1：使用 `logcat` 捕获当前进程日志并上报
  - 方案 2：使用 `StrictMode` + 全局未捕获异常捕获，收集阻塞点和崩溃堆栈并上报
  - 通过 `type` 变量切换两种方案

---

### 一、后端使用说明（backend）

#### 方式一：使用 Docker Compose（推荐）

```yaml
version: '3.8'

services:
  android-log-backend:
    image: bettermankind/android-log-backend:latest
    container_name: android-log-backend
    ports:
      - "${SERVER_PORT:-8080}:8080"
    environment:
      - SERVER_PORT=${SERVER_PORT:-8080}
      - ANDROID_LOG_FILE_BASE_DIR=${ANDROID_LOG_FILE_BASE_DIR:-logs}
      - ANDROID_LOG_FILE_FILE_NAME_PATTERN=${ANDROID_LOG_FILE_BASE_DIR:-android-log-%s.txt}
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
```

在项目根目录下使用 Docker Compose 启动：

```bash
docker-compose up -d
```

**配置说明：**

可以通过环境变量或 `.env` 文件配置以下参数：

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `SERVER_PORT` | 服务监听端口 | `8080` |
| `BVLOG_FILE_BASE_DIR` | 日志文件存储目录 | `logs` |
| `BVLOG_FILE_FILE_NAME_PATTERN` | 日志文件名模式（%s 为日期 yyyy-MM-dd） | `android-log-%s.txt` |

**示例：创建 `.env` 文件自定义配置**

```bash
# .env
SERVER_PORT=9090
BVLOG_FILE_BASE_DIR=/data/logs
BVLOG_FILE_FILE_NAME_PATTERN=my-log-%s.txt
```

然后启动：

```bash
docker-compose up -d
```

**查看日志：**

日志文件会挂载到宿主机的 `./logs` 目录，可以直接查看：

```bash
ls logs/
cat logs/android-log-2024-01-01.txt
```

**停止服务：**

```bash
docker-compose down
```

#### 方式二：本地运行

进入 `backend` 目录：

```bash
mvn clean package
java -jar target/bv-log-backend-0.0.1-SNAPSHOT.jar
```

默认配置：

- 端口：`8080`
- 上报接口：`POST /log/upload`
- 日志输出目录：项目根目录下的 `logs/`
- 日志文件名：`android-log-YYYY-MM-DD.txt`

发送测试请求示例：

```bash
curl -X POST http://127.0.0.1:8080/log/upload -d "hello from android sdk"
```

然后查看 `logs/android-log-YYYY-MM-DD.txt` 即可看到追加的内容。

---

### 二、Android SDK 使用说明（android-sdk）

#### 1. 引入模块

- 将 `android-sdk` 当作一个 Android Library module 导入你的 Android 工程
- 在你的应用 `settings.gradle` 中包含：

```groovy
include ':android-sdk'
```

- 在 app 模块的 `build.gradle` 中添加依赖：

```groovy
dependencies {
    implementation project(':android-sdk')
}
```

#### 2. 初始化 SDK

在你的 `Application` 中初始化（Java 示例）：

```java
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        BvLoggerConfig config = new BvLoggerConfig.Builder()
                .setServerUrl("http://<your-server-ip>:8080/log/upload")
                .setType(BvLoggerConfig.TYPE_LOGCAT)      // 1：logcat；2：StrictMode + 崩溃捕获
                .setEnable(true)
                .build();

        BvLogger.init(this, config);
    }
}
```

Kotlin 示例：

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = BvLoggerConfig.Builder()
            .setServerUrl("http://<your-server-ip>:8080/log/upload")
            .setType(BvLoggerConfig.TYPE_STRICTMODE)
            .setEnable(true)
            .build()

        BvLogger.init(this, config)
    }
}
```

#### 3. `type` 变量说明

- `BvLoggerConfig.TYPE_LOGCAT`（1）：
  - 启动 `LogcatCatcher`，在后台线程执行 `logcat -v time --pid=<当前进程>` 读取本进程 logcat 日志
  - 每一行被封装为 `LogEvent(Type.LOGCAT, ...)` 并实时上报后端
- `BvLoggerConfig.TYPE_STRICTMODE`（2）：
  - 启用 `StrictMode` 的线程策略与 VM 策略，捕获磁盘 IO、网络等违规并打印到 logcat
  - 安装 `Thread.setDefaultUncaughtExceptionHandler`，在应用发生未捕获异常时采集完整堆栈
  - 生成 `LogEvent(Type.CRASH/STRICTMODE, ...)` 并上报后端

#### 4. 日志文件格式

- SDK 将事件转换为一行文本：

```text
[timestamp] [TYPE] [threadName] message...
```

- 后端不会解析内容，而是原样写入 `.txt`，前面额外再加一次服务端时间戳，方便排查：

```text
[serverTs] [clientTs] [TYPE] [threadName] message...
```

---

### 三、联调与测试建议

1. 启动后端 `backend`，确认 `curl` 能正确写入日志文件。
2. 在示例 App 中集成 `android-sdk`，配置 `TYPE_LOGCAT`：
   - 在 Activity 中打印一些 `Log.d/Log.e` 日志，观察后端日志文件是否有对应行。
3. 切换为 `TYPE_STRICTMODE`：
   - 主线程执行磁盘 IO 或网络请求触发 StrictMode 违规；
   - 人为抛出未捕获异常（例如点击按钮直接 `throw RuntimeException("test")`）；
   - 查看后端日志中是否包含详细堆栈信息。


