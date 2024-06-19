# 使用官方的Java运行时作为父镜像
FROM openjdk:8-jdk-alpine

# 将JAR包添加到镜像中，注意这里的路径需要根据实际情况调整
ADD ./target/localTest-*.jar /localTests.jar

# 设置容器启动时需要执行的命令
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/localTests.jar"]