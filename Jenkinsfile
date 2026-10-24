def sonarScan(String dirPath, String key, String name, String extraArgs) {
    dir(dirPath) {
        withSonarQubeEnv('SonarQube-Server') {
            sh "${tool 'sonar-scanner'}/bin/sonar-scanner " +
               "-Dsonar.projectKey=${key} -Dsonar.projectName='${name}' ${extraArgs}"
        }
    }
    timeout(time: 5, unit: 'MINUTES') {
        waitForQualityGate abortPipeline: true
    }
}

def servicesList() {
    return [
        [id: 'discovery-server', dir: 'backend/discovery-server', type: 'maven', imageRepo: 'discovery-server-image', sonarKey: 'discovery-server', sonarName: 'Discovery Server'],
        [id: 'api-gateway',      dir: 'backend/api-ggateway',      type: 'maven', imageRepo: 'api-gateway-image',      sonarKey: 'api-gateway',      sonarName: 'API Gateway'],
        [id: 'user-service',     dir: 'backend/user-service',     type: 'maven', imageRepo: 'user-service-image',     sonarKey: 'user-service',     sonarName: 'User Service'],
        [id: 'product-service',  dir: 'backend/product-service',  type: 'maven', imageRepo: 'product-service-image',  sonarKey: 'product-service',  sonarName: 'Product Service'],
        [id: 'media-service',    dir: 'backend/media-service',    type: 'maven', imageRepo: 'media-service-image',    sonarKey: 'media-service',    sonarName: 'Media Service'],
        [id: 'frontend',         dir: 'frontend',                 type: 'node',  imageRepo: 'frontend-image',         sonarKey: 'frontend',         sonarName: 'Frontend']
    ]
}

pipeline {
    agent any

    environment {
        REPO_URL             = 'https://github.com/rachid-serraf/Buy-01.git'
        COMPOSE_PROJECT_NAME = 'buy01'
        IMAGE_TAG            = "${BUILD_NUMBER}"
        HEALTH_RETRIES       = '30'
        HEALTH_INTERVAL      = '10'
        SLACK_CHANNEL        = '#project-buy-01'

        DISCOVERY_SERVER_IMAGE = "discovery-server-image:${BUILD_NUMBER}"
        API_GATEWAY_IMAGE      = "api-gateway-image:${BUILD_NUMBER}"
        PRODUCT_SERVICE_IMAGE  = "product-service-image:${BUILD_NUMBER}"
        USER_SERVICE_IMAGE     = "user-service-image:${BUILD_NUMBER}"
        MEDIA_SERVICE_IMAGE    = "media-service-image:${BUILD_NUMBER}"
        FRONTEND_IMAGE         = "frontend-image:${BUILD_NUMBER}"
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    def services = servicesList()

                    sh 'git fetch --unshallow || true'

                    def changedFiles
                    if (env.CHANGE_TARGET) {
                        sh "git fetch origin ${env.CHANGE_TARGET}"
                        changedFiles = sh(
                            script: "git diff --name-only origin/${env.CHANGE_TARGET}...HEAD",
                            returnStdout: true
                        ).trim()
                    } else {
                        def base = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: 'HEAD~1'
                        changedFiles = sh(
                            script: "git diff --name-only ${base} HEAD || find . -type f",
                            returnStdout: true
                        ).trim()
                    }

                    echo "Changed files:\n${changedFiles}"

                    env.INFRASTRUCTURE_CHANGED = (changedFiles.contains('docker-compose.yml') ||
                                                   changedFiles.contains('Jenkinsfile') ||
                                                   changedFiles.contains('.env')) ? 'true' : 'false'

                    def changedList = []
                    services.each { svc ->
                        def flag = svc.id.toUpperCase().replace('-', '_') + '_CHANGED'
                        if (changedFiles.contains("${svc.dir}/")) {
                            env[flag] = 'true'
                            changedList << svc.id
                        } else {
                            env[flag] = 'false'
                        }
                    }
                    env.CHANGED_SERVICES = changedList.join(' ')
                    echo "Changed services: ${env.CHANGED_SERVICES ?: 'none'}"
                    echo "Infrastructure changed: ${env.INFRASTRUCTURE_CHANGED}"
                }
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    def services = servicesList()
                    def jobs = [:]
                    services.each { svc ->
                        def flag = svc.id.toUpperCase().replace('-', '_') + '_CHANGED'
                        def changed = env[flag] == 'true'
                        if (changed || env.INFRASTRUCTURE_CHANGED == 'true') {
                            jobs[svc.id] = {
                                stage(svc.id) {
                                    if (svc.type == 'maven') {
                                        dir(svc.dir) {
                                            sh './mvnw clean test'
                                        }
                                    } else if (svc.type == 'node') {
                                        dir(svc.dir) {
                                            sh 'npm ci'
                                            sh 'npm run test:ci'
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (jobs.isEmpty()) {
                        echo "No services to test."
                    } else {
                        parallel jobs
                    }
                }
            }
            post {
                always {
                    junit(
                        testResults: '''
                            backend/*/target/surefire-reports/*.xml,
                            backend/*/*/target/surefire-reports/*.xml
                        ''',
                        allowEmptyResults: true
                    )
                }
            }
        }

        stage('SonarQube Analysis & Quality Gate') {
            steps {
                script {
                    def services = servicesList()
                    def javaArgs = '-Dsonar.sources=src/main -Dsonar.java.binaries=target/classes ' +
                                    '-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                    def nodeArgs = '-Dsonar.sources=src -Dsonar.exclusions=**/node_modules/**,**/dist/**'

                    services.each { svc ->
                        def flag = svc.id.toUpperCase().replace('-', '_') + '_CHANGED'
                        def changed = env[flag] == 'true'
                        if (changed || env.INFRASTRUCTURE_CHANGED == 'true') {
                            echo "Sonar: ${svc.sonarName}"
                            if (svc.type == 'maven') {
                                sonarScan(svc.dir, svc.sonarKey, svc.sonarName, javaArgs)
                            } else if (svc.type == 'node') {
                                sonarScan(svc.dir, svc.sonarKey, svc.sonarName, nodeArgs)
                            }
                        }
                    }
                }
            }
        }

        stage('Build Images') {
            when { branch 'main' }
            steps {
                script {
                    def services = servicesList()
                    def jobs = [:]
                    services.each { svc ->
                        def flag = svc.id.toUpperCase().replace('-', '_') + '_CHANGED'
                        def changed = env[flag] == 'true'
                        if (changed || env.INFRASTRUCTURE_CHANGED == 'true') {
                            jobs[svc.id] = {
                                stage(svc.id) {
                                    script {
                                        def imageEnv = svc.id.toUpperCase().replace('-', '_') + '_IMAGE'
                                        def imgTag = env[imageEnv]
                                        def dockerfile = (svc.id == 'discovery-server') ? 'dockerfile' : 'Dockerfile'
                                        sh """
                                            echo "Building ${svc.id} image..."
                                            if [ -f "${svc.dir}/${dockerfile}" ]; then
                                                docker build -f "${svc.dir}/${dockerfile}" -t "${imgTag}" -t "${svc.imageRepo}:latest" "${svc.dir}"
                                            elif [ -f "${svc.dir}/Dockerfile" ]; then
                                                docker build -t "${imgTag}" -t "${svc.imageRepo}:latest" "${svc.dir}"
                                            else
                                                echo "No Dockerfile found for ${svc.id}"
                                                exit 1
                                            fi
                                        """
                                    }
                                }
                            }
                        }
                    }
                    if (jobs.isEmpty()) {
                        echo "No images to build."
                    } else {
                        parallel jobs
                    }
                }
            }
        }

        stage('Smoke Test Images') {
            when { branch 'main' }
            steps {
                script {
                    def services = servicesList()
                    services.each { svc ->
                        def flag = svc.id.toUpperCase().replace('-', '_') + '_CHANGED'
                        def changed = env[flag] == 'true'
                        if (changed || env.INFRASTRUCTURE_CHANGED == 'true') {
                            if (svc.type == 'maven') {
                                def imageEnv = svc.id.toUpperCase().replace('-', '_') + '_IMAGE'
                                sh """docker run --rm --entrypoint java "${env[imageEnv]}" -version"""
                            } else {
                                echo "Skipping smoke test for ${svc.id} (requires compose network)"
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                withCredentials([file(credentialsId: 'APP_ENV_FILE', variable: 'ENV_FILE')]) {
                    script {
                        def services = servicesList()
                        def toDeploy = []
                        services.each { svc ->
                            def flag = svc.id.toUpperCase().replace('-', '_') + '_CHANGED'
                            def changed = env[flag] == 'true'
                            if (changed || env.INFRASTRUCTURE_CHANGED == 'true') {
                                toDeploy << svc.id
                            }
                        }
                        def order = ['discovery-server': 0, 'api-gateway': 1, 'user-service': 2, 'product-service': 2, 'media-service': 2, 'frontend': 3]
                        toDeploy = toDeploy.sort { a, b -> (order[a] ?: 99) <=> (order[b] ?: 99) }
                        env.SERVICES_TO_DEPLOY = toDeploy.join(' ')
                        echo "Services to deploy: ${env.SERVICES_TO_DEPLOY}"
                    }

                    sh '''
                        cp "${ENV_FILE}" .env
                        test -s .env
                        grep -q '^SPRING_PROFILES_ACTIVE=' .env

                        if [ -z "${SERVICES_TO_DEPLOY}" ]; then
                            echo "No services changed — skipping deployment."
                            exit 0
                        fi

                        snapshot_image() {
                            docker inspect --format '{{.Config.Image}}' "$1" 2>/dev/null || true
                        }

                        for SVC in ${SERVICES_TO_DEPLOY}; do
                            snapshot_image "$SVC" > ".prev_${SVC}"
                        done

                        for SVC in ${SERVICES_TO_DEPLOY}; do
                            if docker inspect "$SVC" > /dev/null 2>&1; then
                                docker stop "$SVC"
                                docker rm "$SVC"
                            else
                                echo "$SVC not running — skipping stop."
                            fi
                        done

                        export DISCOVERY_SERVER_IMAGE="${DISCOVERY_SERVER_IMAGE}"
                        export API_GATEWAY_IMAGE="${API_GATEWAY_IMAGE}"
                        export PRODUCT_SERVICE_IMAGE="${PRODUCT_SERVICE_IMAGE}"
                        export USER_SERVICE_IMAGE="${USER_SERVICE_IMAGE}"
                        export MEDIA_SERVICE_IMAGE="${MEDIA_SERVICE_IMAGE}"
                        export FRONTEND_IMAGE="${FRONTEND_IMAGE}"

                        docker compose up -d --no-deps --force-recreate ${SERVICES_TO_DEPLOY}

                        echo "Services restarted: ${SERVICES_TO_DEPLOY}"
                    '''
                }
            }
            post {
                failure {
                    sh '''
                        echo "Deploy failed — restoring previous images..."

                        for SVC in ${SERVICES_TO_DEPLOY}; do
                            PREV_IMAGE=$(cat ".prev_${SVC}" 2>/dev/null || true)
                            if [ -n "$PREV_IMAGE" ]; then
                                echo "Restoring $SVC to $PREV_IMAGE"
                                case $SVC in
                                    discovery-server)
                                        DISCOVERY_SERVER_IMAGE="$PREV_IMAGE"
                                        ;;
                                    api-gateway)
                                        API_GATEWAY_IMAGE="$PREV_IMAGE"
                                        ;;
                                    product-service)
                                        PRODUCT_SERVICE_IMAGE="$PREV_IMAGE"
                                        ;;
                                    user-service)
                                        USER_SERVICE_IMAGE="$PREV_IMAGE"
                                        ;;
                                    media-service)
                                        MEDIA_SERVICE_IMAGE="$PREV_IMAGE"
                                        ;;
                                    frontend)
                                        FRONTEND_IMAGE="$PREV_IMAGE"
                                        ;;
                                esac
                            fi
                        done

                        export DISCOVERY_SERVER_IMAGE API_GATEWAY_IMAGE PRODUCT_SERVICE_IMAGE USER_SERVICE_IMAGE MEDIA_SERVICE_IMAGE FRONTEND_IMAGE
                        docker compose up -d --no-deps --force-recreate ${SERVICES_TO_DEPLOY}
                        echo "Rollback complete."
                    '''
                }
            }
        }

        stage('Health Check') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                sh '''
                    if [ -z "${SERVICES_TO_DEPLOY}" ]; then
                        echo "No services deployed — skipping health check."
                        exit 0
                    fi

                    healthy() {
                        SVC=$1
                        TRIES=${HEALTH_RETRIES}
                        until [ "$(docker inspect --format '{{.State.Health.Status}}' "$SVC" 2>/dev/null)" = "healthy" ]; do
                            TRIES=$((TRIES - 1))
                            [ "$TRIES" -le 0 ] && { echo "ERROR: $SVC never became healthy."; return 1; }
                            echo "Waiting for $SVC... ($TRIES retries left)"
                            sleep "${HEALTH_INTERVAL}"
                        done
                        echo "$SVC is healthy."
                    }

                    for SVC in ${SERVICES_TO_DEPLOY}; do
                        if docker inspect --format '{{.State.Health.Status}}' "$SVC" 2>/dev/null | grep -qv '<no value>'; then
                            healthy "$SVC" || exit 1
                        else
                            echo "$SVC has no healthcheck configured, skipping health check."
                        fi
                    done

                    echo "All deployed services are healthy."
                '''
            }
            post {
                failure {
                    sh '''
                        echo "Health check failed — rolling back..."

                        for SVC in ${SERVICES_TO_DEPLOY}; do
                            PREV_IMAGE=$(cat ".prev_${SVC}" 2>/dev/null || true)
                            if [ -n "$PREV_IMAGE" ]; then
                                echo "Restoring $SVC to $PREV_IMAGE"
                                case $SVC in
                                    discovery-server)
                                        DISCOVERY_SERVER_IMAGE="$PREV_IMAGE"
                                        ;;
                                    api-gateway)
                                        API_GATEWAY_IMAGE="$PREV_IMAGE"
                                        ;;
                                    product-service)
                                        PRODUCT_SERVICE_IMAGE="$PREV_IMAGE"
                                        ;;
                                    user-service)
                                        USER_SERVICE_IMAGE="$PREV_IMAGE"
                                        ;;
                                    media-service)
                                        MEDIA_SERVICE_IMAGE="$PREV_IMAGE"
                                        ;;
                                    frontend)
                                        FRONTEND_IMAGE="$PREV_IMAGE"
                                        ;;
                                esac
                            fi
                        done

                        export DISCOVERY_SERVER_IMAGE API_GATEWAY_IMAGE PRODUCT_SERVICE_IMAGE USER_SERVICE_IMAGE MEDIA_SERVICE_IMAGE FRONTEND_IMAGE
                        docker compose up -d --no-deps --force-recreate ${SERVICES_TO_DEPLOY}
                        echo "Rollback complete — previous versions restored."
                    '''
                }
            }
        }

        stage('Cleanup Old Images') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }
            steps {
                sh '''
                    KEEP=3

                    for repo in discovery-server-image api-gateway-image product-service-image user-service-image media-service-image frontend-image; do
                        docker images "$repo" --format '{{.Tag}}' \
                          | grep -E '^[0-9]+$' \
                          | sort -rn \
                          | tail -n +$((KEEP + 1)) \
                          | while read -r tag; do
                                echo "Removing old image $repo:$tag"
                                docker rmi "$repo:$tag" || true
                            done
                    done

                    docker image prune -f || true
                    docker builder prune -f --filter until=72h || true

                    echo "Remaining images:"
                    docker images | grep -E 'discovery-server-image|api-gateway-image|product-service-image|user-service-image|media-service-image|frontend-image' || true
                '''
            }
        }
    }

    post {
        success {
            script {
                def msg = env.CHANGE_ID
                    ? "PR #${env.CHANGE_ID} validation passed — ${env.BUILD_URL}"
                    : "Build #${env.BUILD_NUMBER} deployed successfully — ${env.BUILD_URL}\nDeployed services: ${env.SERVICES_TO_DEPLOY ?: 'none (no changes)'}"
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    slackSend(channel: env.SLACK_CHANNEL, color: 'good', message: "SUCCESS: ${msg}")
                }
            }
        }
        failure {
            script {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    slackSend(channel: env.SLACK_CHANNEL, color: 'danger', message: "FAILED: Build #${env.BUILD_NUMBER} failed — ${env.BUILD_URL}\nCheck the console for details.")
                }
            }
        }
        always {
            sh '''
            for repo in discovery-server-image api-gateway-image product-service-image user-service-image media-service-image frontend-image; do
                docker image ls "$repo" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}" || true
            done
            '''
            cleanWs()
        }
    }
}
