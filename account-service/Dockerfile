# ================================================
# 多阶段构建：构建阶段（Docker内编译，无需服务器安装Maven/JDK）
# 构建上下文为项目根目录
# ================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 复制父POM和所有模块POM（Maven需要完整的模块结构）
COPY pom.xml .
COPY common/pom.xml ./common/
COPY account-service/pom.xml ./account-service/
COPY ai-service/pom.xml ./ai-service/
COPY product-service/pom.xml ./product-service/
COPY gateway/pom.xml ./gateway/
RUN mvn dependency:go-offline -B -pl account-service -am 2>/dev/null || true

# 只复制需要的模块源码并构建
COPY common/src ./common/src
COPY account-service/src ./account-service/src
RUN mvn clean package -DskipTests -pl account-service -am

# ================================================
# 运行阶段（轻量级JRE镜像）
# ================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/account-service/target/*.jar app.jar

# ------------------------------------------------
# 时区配置（设置为亚洲/上海时区）
# 通过符号链接和配置文件修改容器时区
# ------------------------------------------------
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# ================================================
# JVM参数配置（内存管理与GC优化）
# 关键参数说明：
#   -Xms/-Xmx          : 堆内存初始/最大值
#   -XX:MetaspaceSize  : 元空间初始大小
#   -UseG1GC           : 启用G1垃圾回收器
#   -MaxGCPauseMillis  : 目标最大GC停顿时间
#   -HeapDumpOn...     : OOM时生成堆转储
#   -UseContainerSupport : 容器内存感知
#   -MaxRAMPercentage  : 最大内存占用比例
# ================================================
ENV JAVA_OPTS="-Xms2g -Xmx4g \
    -XX:MetaspaceSize=256m \
    -XX:MaxMetaspaceSize=512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:ParallelGCThreads=4 \
    -XX:ConcGCThreads=2 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/dump \
    -Xlog:gc*:file=/app/gc.log \
    -XX:+UseStringDeduplication \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0"

# ------------------------------------------------
# 声明容器暴露端口（HTTP服务默认端口）
# ------------------------------------------------
EXPOSE 8081

# ================================================
# 容器启动入口命令
# 使用sh -c执行命令以支持环境变量扩展
# 最终启动命令：java [JVM参数] -jar app.jar
# ================================================
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]