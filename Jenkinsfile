// =============================================================================
// SERVICE REGISTRY: the ONLY place you edit when adding a new service.
// Keep the compose service name / container_name equal to `id`, and use
// image: ${<ID_UPPER_SNAKE>_IMAGE} in docker-compose.yml
//   e.g. user-service -> ${USER_SERVICE_IMAGE}
// =============================================================================
def servicesList() {
    return [
        [id: 'discovery-server', dir: 'backend/discovery-server', type: 'maven', sonarName: 'Discovery Server'],
        [id: 'api-gateway',      dir: 'backend/api-gateway',      type: 'maven', sonarName: 'API Gateway'],
        [id: 'user-service',     dir: 'backend/user-service',     type: 'maven', sonarName: 'User Service'],
        [id: 'product-service',  dir: 'backend/product-service',  type: 'maven', sonarName: 'Product Service'],
        [id: 'media-service',    dir: 'backend/media-service',    type: 'maven', sonarName: 'Media Service'],
        [id: 'frontend',         dir: 'frontend',                 type: 'node',  sonarName: 'Frontend'],
    ]
}

// ---- naming conventions (derived, never stored per service) -----------------
def imageRepo(svc) { return "${svc.id}-image" }                                   // user-service -> user-service-image
def imageVar(svc)  { return svc.id.toUpperCase().replace('-', '_') + '_IMAGE' }   // user-service -> USER_SERVICE_IMAGE

// Services selected by 'Detect Changes', in the same order as servicesList()
def changedServices() {
    def ids = (env.CHANGED_SERVICES ?: '').tokenize(' ')
    def result = []
    for (svc in servicesList()) {
        if (ids.contains(svc.id)) {
            result << svc
        }
    }
    return result
}

// "export USER_SERVICE_IMAGE=user-service-image:<tag>" for every service
def imageExports(String tag) {
    return servicesList().collect { "export ${imageVar(it)}=${imageRepo(it)}:${tag}" }.join('\n') + '\n'
}

// "discovery-server-image api-gateway-image ..."
def allRepos() {
    return servicesList().collect { imageRepo(it) }.join(' ')
}

// Run a closure per service, but limit concurrency to 'maxBatch' (2 at a time) across the pipeline
def runParallel(List services, Closure body) {
    if (!services) return

    int maxBatch = 2 // Limits active concurrent services to 2 globally
    def batches = services.collate(maxBatch) // Splits the list into chunks of 2

    for (int i = 0; i < batches.size(); i++) {
        def batch = batches[i]
        def branches = [:]

        for (svc in batch) {
            def s = svc
            branches[s.id] = { body(s) }
        }

        // Executes this batch in parallel, waits for completion, then moves to the next batch
        stage("Batch ${i + 1} (${batch.collect { it.id }.join(', ')})") {
            parallel branches
        }
    }
}

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

// Restore the images saved in .prev_<service> before the deploy
def rollback() {
    sh(imageExports('latest') + '''
        if [ -z "${SERVICES_TO_DEPLOY}" ]; then
            echo "No services were being deployed — nothing to roll back."
            exit 0
        fi

        for SVC in ${SERVICES_TO_DEPLOY}; do
            PREV_IMAGE=$(cat ".prev_${SVC}" 2>/dev/null || true)
            if [ -n "$PREV_IMAGE" ]; then
                VAR="$(echo "$SVC" | tr 'a-z-' 'A-Z_')_IMAGE"
                echo "Restoring $SVC to $PREV_IMAGE"
                export "$VAR=$PREV_IMAGE"
            fi
        done

        docker compose up -d --no-deps --force-recreate ${SERVICES_TO_DEPLOY}
        echo "Rollback complete — previous versions restored."
    ''')
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

                    // `find .` fallback prints "./path", so strip the leading "./"
                    def files = changedFiles.readLines().collect { it.startsWith('./') ? it.substring(2) : it }
                    def infra = files.any { it in ['docker-compose.yml', 'Jenkinsfile', '.env'] }

                    env.INFRASTRUCTURE_CHANGED = infra.toString()
                    env.CHANGED_SERVICES = servicesList()
                        .findAll { svc -> infra || files.any { f -> f.startsWith(svc.dir + '/') } }
                        .collect { it.id }
                        .join(' ')

                    echo "Changed services: ${env.CHANGED_SERVICES ?: 'none'}"
                    echo "Infrastructure changed: ${env.INFRASTRUCTURE_CHANGED}"
                }
            }
        }

        stage('Unit Tests') {
            steps {
                script {
                    runParallel(changedServices()) { svc ->
                        dir(svc.dir) {
                            if (svc.type == 'maven') {
                                sh './mvnw clean test'
                            } else {
                                sh 'npm ci'
                                sh 'npm run test:ci'
                            }
                        }
                    }
                }
            }
            post {
                always {
                    junit(
                        testResults: 'backend/*/target/surefire-reports/*.xml,backend/*/*/target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )
                }
            }
        }

        stage('SonarQube Analysis & Quality Gate') {
            steps {
                script {
                    def javaArgs = '-Dsonar.sources=src/main -Dsonar.java.binaries=target/classes ' +
                                   '-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml'
                    def nodeArgs = '-Dsonar.sources=src -Dsonar.exclusions=**/node_modules/**,**/dist/**'

                    runParallel(changedServices()) { svc ->
                        echo "Sonar: ${svc.sonarName}"
                        sonarScan(svc.dir, svc.id, svc.sonarName, svc.type == 'maven' ? javaArgs : nodeArgs)
                    }
                }
            }
        }

        stage('Build Images') {
            when { branch 'main' }
            steps {
                script {
                    runParallel(changedServices()) { svc ->
                        def repo = imageRepo(svc)
                        sh """
                            echo "Building ${svc.id} image..."
                            DF=\$(ls ${svc.dir}/[Dd]ockerfile 2>/dev/null | head -n1)
                            [ -n "\$DF" ] || { echo "No Dockerfile found for ${svc.id}"; exit 1; }
                            docker build -f "\$DF" -t ${repo}:${env.IMAGE_TAG} -t ${repo}:latest ${svc.dir}
                        """
                    }
                }
            }
        }

        stage('Smoke Test Images') {
            when { branch 'main' }
            steps {
                script {
                    // Only JVM images have `java -version` to check
                    for (svc in changedServices().findAll { it.type == 'maven' }) {
                        sh "docker run --rm --entrypoint java ${imageRepo(svc)}:${env.IMAGE_TAG} -version"
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
                        env.SERVICES_TO_DEPLOY = changedServices().collect { it.id }.join(' ')
                        echo "Services to deploy: ${env.SERVICES_TO_DEPLOY}"
                    }

                    sh(imageExports(env.IMAGE_TAG) + '''
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
                                echo "Stopping $SVC..."
                                docker stop "$SVC"
                                docker rm "$SVC"
                            else
                                echo "$SVC not running — skipping stop."
                            fi
                        done

                        docker compose up -d --no-deps --force-recreate ${SERVICES_TO_DEPLOY}

                        echo "Services restarted: ${SERVICES_TO_DEPLOY}"
                    ''')
                }
            }
            post {
                failure {
                    script {
                        echo 'Deploy failed — restoring previous images...'
                        rollback()
                    }
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
                    script {
                        echo 'Health check failed — rolling back...'
                        rollback()
                    }
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
                script {
                    sh("REPOS='${allRepos()}'\n" + '''
                        KEEP=3

                        for repo in $REPOS; do
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
                        docker images | grep -E "$(echo $REPOS | tr ' ' '|')" || true
                    ''')
                }
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
            script {
                sh("REPOS='${allRepos()}'\n" + '''
                    for repo in $REPOS; do
                        docker image ls "$repo" --format "table {{.Repository}} {{.Tag}} {{.ID}}" || true
                    done
                ''')
            }
            cleanWs()
        }
    }
}