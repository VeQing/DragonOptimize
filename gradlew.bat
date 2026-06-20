@rem DragonOptimize - gradle wrapper 启动脚本 (Windows)
@rem 首次执行会自动下载 gradle-4.1-bin.zip（约 70MB），并缓存到 %USERPROFILE%\.gradle\wrapper\dists\
@rem 用法：
@rem   gradlew.bat build          构建发布 jar（输出到 build/libs/）
@rem   gradlew.bat setupDecompWorkspace idea      IDEA 用户第一次初始化
@rem   gradlew.bat setupDecompWorkspace eclipse   Eclipse 用户第一次初始化

@if "%DEBUG%" == "" @echo off
@setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%JAR%" (
    echo [DragonOptimize] 没有找到 gradle-wrapper.jar。
    echo   请把 Forge MDK 中随附的 gradle/wrapper/gradle-wrapper.jar 复制到 %APP_HOME%gradle\wrapper\
    echo   或安装 gradle 4.x 后执行: gradle wrapper --gradle-version 4.1
    exit /b 1
)

set DEFAULT_JVM_OPTS=-Xmx1024m
java -Xmx1024m -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
@endlocal
