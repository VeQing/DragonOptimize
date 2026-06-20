#!/usr/bin/env bash

# DragonOptimize - gradle wrapper 启动脚本
# 首次执行会自动下载 gradle-4.1-bin.zip（约 70MB），并缓存到 ~/.gradle/wrapper/dists/
# 用法：
#   ./gradlew build          # 构建发布 jar（输出到 build/libs/）
#   ./gradlew setupDecompWorkspace idea      # IDEA 用户第一次初始化
#   ./gradlew setupDecompWorkspace eclipse   # Eclipse 用户第一次初始化

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$JAR" ]; then
    echo "[DragonOptimize] 没有找到 gradle-wrapper.jar，尝试通过本地 gradle 生成……"
    if command -v gradle >/dev/null 2>&1; then
        gradle wrapper --gradle-version 4.1
    else
        echo "[DragonOptimize] 未检测到系统 gradle。请先："
        echo "  1) 手动把 Forge MDK 中随附的 gradle/wrapper/gradle-wrapper.jar 复制到 $DIR/gradle/wrapper/"
        echo "  2) 或安装 gradle 4.x，然后 'gradle wrapper --gradle-version 4.1'"
        exit 1
    fi
fi

DEFAULT_JVM_OPTS='"-Xmx1024m" "-Dorg.gradle.appname=gradlew"'
APP_HOME="$( cd "$( dirname "$0" )" && pwd )"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec java -classpath "$CLASSPATH" \
    -Dorg.gradle.appname=gradlew \
    -Xmx1024m \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
