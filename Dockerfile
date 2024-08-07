# 使用官方的Java运行时作为父镜像 (Ubuntu 版本)
FROM openjdk:8-jdk-buster

# 设置工作目录
WORKDIR /app

# 将JAR包添加到镜像中，注意这里的路径需要根据实际情况调整
ADD ./target/localTest-*.jar localTests.jar

# 设置环境变量，指定JVM的启动参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.security.egd=file:/dev/./urandom"

# 声明应用需要监听的端口
EXPOSE 48001

# 设置 JVM 时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 设置容器启动时需要执行的命令
ENTRYPOINT ["java", "-jar", "localTests.jar", "${JAVA_OPTS}"]