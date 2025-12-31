# 项目介绍
1. 一个可以帮助没有adb、开发者权限的安卓环境，收集运行堆栈信息、阻塞日志、崩溃日志，保证详细


## 方案1
使用输出logcat
```java
import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogcatCatcher {

    private var logThread: Thread? = null
    private lateinit var logFile: File

    fun start(context: Context) {
        // 1. 初始化日志文件路径
        val logDir = context.getExternalFilesDir("app_logs")
        if (logDir != null && !logDir.exists()) {
            logDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        logFile = File(logDir, "logcat_$timestamp.txt")

        // 2. 设置全局崩溃捕获器
        setupUncaughtExceptionHandler()

        // 3. 启动后台线程来执行 logcat 命令并读取输出
        logThread = Thread {
            try {
                // 清除旧的日志缓存 (可选)
                Runtime.getRuntime().exec("logcat -c")
                
                // -v time: 输出带时间的日志
                // --pid=...: 只看当前自己App的日志
                val process = Runtime.getRuntime().exec("logcat -v time --pid=${android.os.Process.myPid()}")
                
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val writer = FileWriter(logFile, true) // true for append mode

                Log.i("LogcatCatcher", "Logcat capturing started. Output to: ${logFile.absolutePath}")
                writer.append("--- Logcat capture started ---\n")

                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    writer.append(line).append("\n")
                }
                
                writer.flush()
                writer.close()
                bufferedReader.close()

            } catch (e: Exception) {
                Log.e("LogcatCatcher", "Error during logcat capturing", e)
                writeCrashLog("LogcatCatcher itself failed", e)
            }
        }
        logThread?.start()
    }

    private fun setupUncaughtExceptionHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LogcatCatcher", "FATAL CRASH DETECTED!", throwable)
            
            // 写入崩溃日志
            writeCrashLog("FATAL CRASH in thread: ${thread.name}", throwable)

            // 如果有原始处理器，调用它，让系统正常处理崩溃（比如弹出“应用已停止运行”对话框）
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(message: String, throwable: Throwable) {
        try {
            val writer = FileWriter(logFile, true)
            writer.append("\n\n--- CRASH DETECTED ---\n")
            writer.append(message).append("\n")
            writer.append(Log.getStackTraceString(throwable))
            writer.append("\n--- END OF CRASH ---\n\n")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

```

## 方案2

```java
package com.example.logdemo;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1) StrictMode: 捕获磁盘/网络/Binder 等阻塞点，输出到日志上传器
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()    // 系统 logcat；我们同时拦截并上传
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build());

        // 2) 全局未捕获异常：完整堆栈上传
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LogUploader.uploadException("Uncaught exception in thread: " + thread.getName(), throwable);
            // 可选：崩溃前写入缓冲并尝试同步
            LogUploader.flushBufferSync(getApplicationContext());
        });

        // 3) 启动标识
        LogUploader.uploadInfo("App start", DeviceInfo.buildDeviceSessionInfo(this));
    }
}
```

# 项目落地
- 写一个web后端，支持安卓上报数据，使用maven springboot jdk21 写
- 方案里的任何日志形式最后要换成前后端文件上报的形式，实时把数据上报到后端
- 后端接收往一个txt里追加就行了

- 把多种方案，做一个变量开关，比如变量type ：1是方案1 2是方案2
- 保证兼容java kt的形式，兼容各个安卓版本
- 该项目做成android的sdk形式，用java写