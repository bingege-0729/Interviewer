# ================================================
# 多阶段构建：构建阶段（Docker内编译，无需服务器安装Maven/JDK）
# 构建上下文为项目根目录
# gateway 不依赖 common 模块，只依赖父POM
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
RUN mvn dependency:go-offline -B -pl gateway 2>/dev/null || true

# 只复制需要的模块源码并构建
COPY gateway/src ./gateway/src
RUN mvn clean package -DskipTests -pl gateway

# ================================================
# 运行阶段（轻量级JRE镜像）
# ================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/gateway/target/*.jar app.jar

# 时区配置
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JVM参数配置（网关服务内存适当调小）
ENV JAVA_OPTS="-Xms512m -Xmx1g \
    -XX:MetaspaceSize=128m \
    -XX:MaxMetaspaceSize=256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/dump \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0"

EXPOSE 8000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
