# 使用官方OpenJDK运行环境作为基础镜像
FROM openjdk:17-jdk-slim

# 设置维护者信息
LABEL maintainer="customer-service-agent"

# 设置工作目录
WORKDIR /app

# 复制Maven构建的jar文件到容器中
COPY target/customer-service-agent-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV DASHSCOPE_API_KEY=""
ENV SERVER_PORT=8080

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]
