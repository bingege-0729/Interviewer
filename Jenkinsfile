/**
 * ============================================================
 * Jenkins Pipeline - 多微服务自动化部署脚本
 * ============================================================
 * 
 * 功能说明：
 *   1. 从 Git 拉取最新代码
 *   2. Docker 多阶段构建微服务镜像（Maven 编译 → JRE 运行）
 *   3. 镜像打包传输到目标服务器
 *   4. 远程部署并注入环境变量（Nacos 注册 IP、环境标识）
 *   5. 健康检查确认容器运行状态
 * 
 * 使用方式：
 *   Jenkins 界面 → Build with Parameters → 选择服务 + 选择目标服务器 → 开始构建
 * 
 * 部署架构：
 *   支持单台/多台服务器水平扩容，配合阿里云 SLB 负载均衡实现高可用
 * 
 * 端口分配：
 *   gateway:         8000
 *   account-service: 8081
 *   ai-service:      8082
 *   product-service: 8083
 * ============================================================
 */
pipeline {
    agent any

    environment {
        DEPLOY_USER = 'root'                    // 远程服务器登录用户
        IMAGE_TAG = "${BUILD_NUMBER}"           // 镜像标签（使用 Jenkins 构建号）
    }

    parameters {
        choice(
            name: 'SERVICE',
            choices: ['account-service', 'ai-service', 'product-service', 'gateway', 'all'],
            description: '选择要部署的微服务（all=全部部署）'
        )
        choice(
            name: 'DEPLOY_HOST',
            choices: ['all', '172.17.236.188', '172.17.236.187'],  // 新增服务器时在此追加 IP
            description: '选择部署目标（all=全部服务器）'
        )
    }

    // 服务端口映射配置
    stages {

        stage('解析部署目标') {
            steps {
                script {
                    // 获取所有目标服务器 IP 列表
                    def allServers = ['172.17.236.188', '172.17.236.187']
                    // 根据参数决定部署到哪台/哪些服务器
                    if (params.DEPLOY_HOST == 'all') {
                        env.DEPLOY_HOSTS = allServers.join(',')  // 逗号分隔的多台服务器
                    } else {
                        env.DEPLOY_HOSTS = params.DEPLOY_HOST     // 单台服务器
                    }
                    echo "部署目标服务器: ${env.DEPLOY_HOSTS}"
                }
            }
        }

        stage('拉取代码') {
            steps {
                checkout scm
                echo "代码拉取完成，当前分支: ${env.BRANCH_NAME ?: 'main'}"
            }
        }

        stage('构建镜像') {
            steps {
                script {
                    def services = getServicesToBuild(params.SERVICE)
                    for (svc in services) {
                        echo ">>> 开始构建 ${svc} 镜像..."
                        // Docker 多阶段构建：Maven 编译打包 + JRE 轻量运行镜像
                        sh """
                            docker build \
                                -f ${svc}/Dockerfile \
                                -t ${svc}:${IMAGE_TAG} \
                                -t ${svc}:latest \
                                .
                        """
                        echo ">>> ${svc} 镜像构建完成"
                    }
                }
            }
        }

        stage('传输镜像到目标服务器') {
            steps {
                script {
                    def services = getServicesToBuild(params.SERVICE)
                    def hosts = env.DEPLOY_HOSTS.split(',')  // 解析目标服务器列表
                    sshagent(credentials: ['deploy-ssh-key']) {  // 使用 SSH 私钥免密登录
                        for (svc in services) {
                            // 1. 打包镜像为 tar.gz 压缩文件
                            sh "docker save ${svc}:latest | gzip > /tmp/${svc}.tar.gz"
                            // 2. 通过 SCP 传输到每台目标服务器
                            for (host in hosts) {
                                echo ">>> 传输 ${svc} 镜像到 ${host}..."
                                sh "scp -o StrictHostKeyChecking=no /tmp/${svc}.tar.gz ${DEPLOY_USER}@${host}:/tmp/"
                            }
                            // 3. 清理 Jenkins 本地临时文件
                            sh "rm -f /tmp/${svc}.tar.gz"
                            echo ">>> ${svc} 镜像传输完成（${hosts.size()}台服务器）"
                        }
                    }
                }
            }
        }

        stage('远程部署') {
            steps {
                script {
                    def services = getServicesToBuild(params.SERVICE)
                    // 服务端口映射表（需与 application.yaml、Dockerfile EXPOSE 保持一致）
                    def portMap = [
                        'gateway'         : '8000',
                        'account-service' : '8081',
                        'ai-service'      : '8082',
                        'product-service' : '8083'
                    ]
                    def hosts = env.DEPLOY_HOSTS.split(',')
                    sshagent(credentials: ['deploy-ssh-key']) {
                        for (host in hosts) {
                            for (svc in services) {
                                def port = portMap[svc]
                                echo ">>> 部署 ${svc} 到 ${host}:${port}..."
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${host} '
                                        # 加载镜像
                                        docker load < /tmp/${svc}.tar.gz
                                        rm -f /tmp/${svc}.tar.gz
        
                                        # 停止并删除旧容器（如果存在）
                                        docker stop ${svc} 2>/dev/null || true
                                        docker rm ${svc} 2>/dev/null || true
        
                                        # 启动新容器
                                        docker run -d \\
                                            --name ${svc} \\
                                            --restart=always \\
                                            -e SPRING_PROFILES_ACTIVE=prod \\
                                            -e NACOS_REGISTER_IP=${host} \\
                                            -p ${port}:${port} \\
                                            ${svc}:latest
        
                                        # 清理悬空镜像（节省磁盘空间）
                                        docker image prune -f 2>/dev/null || true
                                    '
                                """
                                echo ">>> ${svc} 在 ${host} 部署完成"
                            }
                        }
                    }
                }
            }
        }

        stage('健康检查') {
            steps {
                script {
                    def services = getServicesToBuild(params.SERVICE)
                    def hosts = env.DEPLOY_HOSTS.split(',')
                    sleep(time: 15, unit: 'SECONDS')  // 等待容器启动和 Spring Boot 初始化
                    sshagent(credentials: ['deploy-ssh-key']) {
                        for (host in hosts) {
                            for (svc in services) {
                                // 通过 docker ps 检查容器是否在运行
                                sh """
                                    ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${host} '
                                        if docker ps | grep -q ${svc}; then
                                            echo "✓ ${svc} 在 ${host} 运行正常"
                                        else
                                            echo "✗ ${svc} 在 ${host} 未运行！"
                                            docker logs ${svc} --tail 50  # 输出最后 50 行日志方便排查
                                            exit 1
                                        fi
                                    '
                                """
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "部署成功！服务: ${params.SERVICE}"
        }
        failure {
            echo "部署失败！服务: ${params.SERVICE}，请检查日志"
        }
        always {
            // 清理 Jenkins 本地构建的镜像（节省磁盘空间）
            script {
                try {
                    def services = getServicesToBuild(params.SERVICE)
                    for (svc in services) {
                        sh "docker rmi ${svc}:${env.BUILD_NUMBER} 2>/dev/null || true"
                        sh "docker rmi ${svc}:latest 2>/dev/null || true"
                    }
                } catch (Exception e) {
                    echo "清理镜像跳过: ${e.message}"
                }
            }
        }
    }
}

// ============================================================
// 辅助方法：根据参数获取要构建的服务列表
// ============================================================
def getServicesToBuild(String service) {
    if (service == 'all') {
        return ['gateway', 'account-service', 'ai-service', 'product-service']
    }
    return [service]
}
