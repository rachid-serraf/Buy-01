pipeline {
    agent any

    environment {
        REPO_URL             = 'https://github.com/rachid-serraf/Buy-01.git'
        COMPOSE_PROJECT_NAME = 'buy01'
        IMAGE_TAG            = "${BUILD_NUMBER}"
        HEALTH_RETRIES       = '30'
        HEALTH_INTERVAL      = '10'
        SLACK_CHANNEL        = '#project-buy-01'

        // Derived image names — one source of truth per service
        PRODUCT_SERVICE_IMAGE = "product-service-image:${BUILD_NUMBER}"
        USER_SERVICE_IMAGE    = "user-service-image:${BUILD_NUMBER}"
        MEDIA_SERVICE_IMAGE   = "media-service-image:${BUILD_NUMBER}"
        FRONTEND_IMAGE        = "frontend-image:${BUILD_NUMBER}"
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

        // ── Detect Changes ───────────────────────────────────────────────
        stage('Detect Changes') {
            steps {
                script {
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

                    env.PRODUCT_SERVICE_CHANGED = changedFiles.contains('backend/product-service/') ? 'true' : 'false'
                    env.USER_SERVICE_CHANGED    = changedFiles.contains('backend/user-service/')    ? 'true' : 'false'
                    env.MEDIA_SERVICE_CHANGED   = changedFiles.contains('backend/media-service/')   ? 'true' : 'false'
                    env.FRONTEND_CHANGED        = changedFiles.contains('frontend/')                ? 'true' : 'false'
                    env.INFRASTRUCTURE_CHANGED  = (changedFiles.contains('docker-compose.yml') ||
                                                   changedFiles.contains('Jenkinsfile') ||
                                                   changedFiles.contains('.env')) ? 'true' : 'false'
                }
            }
        }

        // ── Run tests only for changed services ───────────────────────────
        stage('Unit Tests') {
            parallel {
                stage('product-service') {
                    when {
                        expression {
                            return env.PRODUCT_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        dir('backend/product-service') {
                            sh './mvnw clean test'
                        }
                    }
                }
                stage('user-service') {
                    when {
                        expression {
                            return env.USER_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        dir('backend/user-service') {
                            sh './mvnw clean test'
                        }
                    }
                }
                stage('media-service') {
                    when {
                        expression {
                            return env.MEDIA_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        dir('backend/media-service') {
                            sh './mvnw clean test'
                        }
                    }
                }
                stage('frontend') {
                    when {
                        expression {
                            return env.FRONTEND_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'npm run test:ci'
                        }
                    }
                }
            }
            post {
                always {
                    junit(
                        testResults: '''
                            backend/product-service/target/surefire-reports/*.xml,
                            backend/media-service/target/surefire-reports/*.xml,
                            backend/user-service/target/surefire-reports/*.xml
                        ''',
                        allowEmptyResults: true
                    )
                }
            }
        }

        // ── Build images only for changed services ────────────────────────
        stage('Build Images') {
            when { branch 'main' }
            parallel {
                stage('product-service') {
                    when {
                        expression {
                            return env.PRODUCT_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        sh """
                            echo "Building product-service image..."
                            docker build \
                                -t "${PRODUCT_SERVICE_IMAGE}" \
                                -t "product-service-image:latest" \
                                backend/product-service
                        """
                    }
                }
                stage('user-service') {
                    when {
                        expression {
                            return env.USER_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        sh """
                            echo "Building user-service image..."
                            docker build \
                                -t "${USER_SERVICE_IMAGE}" \
                                -t "user-service-image:latest" \
                                backend/user-service
                        """
                    }
                }
                stage('media-service') {
                    when {
                        expression {
                            return env.MEDIA_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        sh """
                            echo "Building media-service image..."
                            docker build \
                                -t "${MEDIA_SERVICE_IMAGE}" \
                                -t "media-service-image:latest" \
                                backend/media-service
                        """
                    }
                }
                stage('frontend') {
                    when {
                        expression {
                            return env.FRONTEND_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true'
                        }
                    }
                    steps {
                        sh """
                            echo "Building frontend image..."
                            docker build \
                                -t "${FRONTEND_IMAGE}" \
                                -t "frontend-image:latest" \
                                frontend
                        """
                    }
                }
            }
        }

        // ── Smoke test only newly built images ────────────────────────────
        stage('Smoke Test Images') {
            when { branch 'main' }
            steps {
                script {
                    if (env.PRODUCT_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                        sh """docker run --rm --entrypoint java "${PRODUCT_SERVICE_IMAGE}" -version"""
                    }
                    if (env.USER_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                        sh """docker run --rm --entrypoint java "${USER_SERVICE_IMAGE}" -version"""
                    }
                    if (env.MEDIA_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                        sh """docker run --rm --entrypoint java "${MEDIA_SERVICE_IMAGE}" -version"""
                    }
                // Frontend smoke test skipped (requires compose network)
                }
            }
        }

        // ── Deploy: only restart changed services ─────────────────────────
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
                        // Determine which services to deploy
                        def servicesToDeploy = []
                        if (env.PRODUCT_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            servicesToDeploy << 'product-service'
                        }
                        if (env.USER_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            servicesToDeploy << 'user-service'
                        }
                        if (env.MEDIA_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            servicesToDeploy << 'media-service'
                        }
                        if (env.FRONTEND_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            servicesToDeploy << 'frontend'
                        }

                        env.SERVICES_TO_DEPLOY = servicesToDeploy.join(' ')
                        echo "Services to deploy: ${env.SERVICES_TO_DEPLOY}"
                    }

                    sh '''
                        # ── Validate env file ────────────────────────────────
                        cp "${ENV_FILE}" .env
                        test -s .env
                        grep -q '^SPRING_PROFILES_ACTIVE=' .env

                        # Only proceed if there are services to deploy
                        if [ -z "${SERVICES_TO_DEPLOY}" ]; then
                            echo "No services changed — skipping deployment."
                            exit 0
                        fi

                        # ── Snapshot running images for changed services ──────
                        snapshot_image() {
                            docker inspect --format '{{.Config.Image}}' "$1" 2>/dev/null || true
                        }

                        for SVC in ${SERVICES_TO_DEPLOY}; do
                            snapshot_image "$SVC" > ".prev_${SVC}"
                            echo "Snapshot for $SVC: $(cat .prev_${SVC})"
                        done

                        # ── Stop & remove containers for changed services ────
                        for SVC in ${SERVICES_TO_DEPLOY}; do
                            if docker inspect "$SVC" > /dev/null 2>&1; then
                                echo "Stopping $SVC..."
                                docker stop "$SVC"
                                docker rm   "$SVC"
                            else
                                echo "$SVC not running — skipping stop."
                            fi
                        done

                        # ── Start only changed services ───────────────────────
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

                        export PRODUCT_SERVICE_IMAGE USER_SERVICE_IMAGE MEDIA_SERVICE_IMAGE FRONTEND_IMAGE
                        docker compose up -d --no-deps --force-recreate ${SERVICES_TO_DEPLOY}
                        echo "Rollback complete."
                    '''
                }
            }
        }

        // ── Health Check: only verify changed services ────────────────────
        stage('Health Check') {
            when {
                allOf {
                    branch 'main'
                    not { changeRequest() }
                }
            }

            steps {
                sh '''
                    # Skip if no services were deployed
                    if [ -z "${SERVICES_TO_DEPLOY}" ]; then
                        echo "No services deployed — skipping health check."
                        exit 0
                    fi

                    healthy() {
                        SVC=$1
                        TRIES=${HEALTH_RETRIES}
                        until [ "$(docker inspect --format '{{.State.Health.Status}}' \
                                    "$SVC" 2>/dev/null)" = "healthy" ]; do
                            TRIES=$((TRIES - 1))
                            [ "$TRIES" -le 0 ] && \
                                { echo "ERROR: $SVC never became healthy."; return 1; }
                            echo "Waiting for $SVC... ($TRIES retries left)"
                            sleep "${HEALTH_INTERVAL}"
                        done
                        echo "$SVC is healthy."
                    }

                    # Check health of deployed services only
                    for SVC in ${SERVICES_TO_DEPLOY}; do
                        healthy "$SVC" || exit 1
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

                        export PRODUCT_SERVICE_IMAGE USER_SERVICE_IMAGE MEDIA_SERVICE_IMAGE FRONTEND_IMAGE
                        docker compose up -d --no-deps --force-recreate ${SERVICES_TO_DEPLOY}
                        echo "Rollback complete — previous versions restored."
                    '''
                }
            }
        }

        // ── Cleanup: remove old image tags, keep last 3 for rollback ──────
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

                    for repo in product-service-image user-service-image media-service-image frontend-image; do
                        docker images "$repo" --format '{{.Tag}}' \
                          | grep -E '^[0-9]+$' \
                          | sort -rn \
                          | tail -n +$((KEEP + 1)) \
                          | while read -r tag; do
                                echo "Removing old image $repo:$tag"
                                docker rmi "$repo:$tag" || true
                            done
                    done

                    # Dangling images and build cache older than 3 days
                    docker image prune -f || true
                    docker builder prune -f --filter until=72h || true

                    echo "Remaining images:"
                    docker images | grep -E 'product-service-image|user-service-image|media-service-image|frontend-image' || true
                '''
            }
        }

        // ── SonarQube Analysis ────────────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('SonarQube-Server') {
                        
                        // 1. Product Service (Maven/Spring Boot)
                        if (env.PRODUCT_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            echo "Running SonarQube analysis for Product Service..."
                            dir('backend/product-service') {
                                sh "mvn clean verify sonar:sonar \
                                    -Dsonar.projectKey=product-service \
                                    -Dsonar.projectName='Product Service'"
                            }
                        }

                        // 2. User Service (Maven/Spring Boot)
                        if (env.USER_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            echo "Running SonarQube analysis for User Service..."
                            dir('backend/user-service') {
                                sh "mvn clean verify sonar:sonar \
                                    -Dsonar.projectKey=user-service \
                                    -Dsonar.projectName='User Service'"
                            }
                        }

                        // 3. Media Service (Maven/Spring Boot)
                        if (env.MEDIA_SERVICE_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            echo "Running SonarQube analysis for Media Service..."
                            dir('backend/media-service') {
                                sh "mvn clean verify sonar:sonar \
                                    -Dsonar.projectKey=media-service \
                                    -Dsonar.projectName='Media Service'"
                            }
                        }

                        // 4. Frontend (Angular / Node CLI scanner)
                        if (env.FRONTEND_CHANGED == 'true' || env.INFRASTRUCTURE_CHANGED == 'true') {
                            echo "Running SonarQube analysis for Frontend..."
                            def scannerHome = tool 'sonar-scanner'
                            dir('frontend') {
                                sh "${scannerHome}/bin/sonar-scanner \
                                    -Dsonar.projectKey=frontend \
                                    -Dsonar.projectName='Frontend' \
                                    -Dsonar.sources=src \
                                    -Dsonar.exclusions=**/node_modules/**,**/dist/**"
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Pipeline-level notifications & cleanup ────────────────────────────────
    post {
        success {
            script {
                def msg = env.CHANGE_ID
                ? "PR #${env.CHANGE_ID} validation passed — ${env.BUILD_URL}"
                : "Build #${env.BUILD_NUMBER} deployed successfully — ${env.BUILD_URL}\nDeployed services: ${env.SERVICES_TO_DEPLOY ?: 'none (no changes)'}"

                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                    slackSend(channel: env.SLACK_CHANNEL,
                          color: 'good',
                          message: "SUCCESS: ${msg}")
                }
            }
        }
        failure {
            script {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    slackSend(channel: env.SLACK_CHANNEL,
                          color: 'danger',
                          message: "FAILED: Build #${env.BUILD_NUMBER} failed — ${env.BUILD_URL}\nCheck the console for details.")
                }
            }
        }
        always {
            sh '''
            for repo in product-service-image user-service-image media-service-image frontend-image; do
                docker image ls "$repo" \
                    --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}" || true
            done
        '''
            cleanWs()
        }
    }
}
