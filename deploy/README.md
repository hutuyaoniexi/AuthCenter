# 一、Jenkins自动化部署
## 1、必要文件
- docker/Dockerfile
- .dockerignore
- docker-compose.yml
- Jenkinsfile

## 2、配置/actuator/health接口
### 2.1、修改pom文件
```pom
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2.2、配置yml
```yml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
```

### 2.3、将/actuator/health写进白名单

## 3 配置box-common 私有仓库引入
```pom
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/hutuyaoniexi/box-common</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## 4、上传配置文件
- config
- nginx/xxxx.config

## 5、创建最简单 Pipeline 任务
- 新建任务
- 输入任务名
- 选择Pipeline
- 配置Pipeline
    - 选择 Pipeline script from SCM
    - 选择 git
    - Repository URL：写ssh的git地址
    - 用户类型：SSH Username with private key
    - 分支：*/main 或 */master
    - 脚本路径：
        - 在根路径：Jenkinsfile
        - 在自定义路径：deploy/jenkins/Jenkinsfile


# 二、手动
# 配置.dockerignore

# 生成jar包
mvn clean package -DskipTests

# 构建镜像
docker build --platform linux/amd64 -f docker/Dockerfile -t auth-center .

# 生成镜像文件
docker save -o docker/auth-center.tar auth-center

# 加载镜像
cd ~/projects/auth-center/
docker load -i auth-center.tar

# 运行
docker compose down
docker compose up -d




