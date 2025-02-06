def gitId = 'github'
def gitRepo = 'test.org/test.git'
// Set branch in the Jenkins Job or set here if not variable
def gitBranch = 'refs/heads/${branch}'
def projectDirectory = 'test-559-10'
def jenkinsSteps
// Set URL of ArgoCD server
def argocdUrl = ''
def argocdAppName = 'test-559-10'
def argocdNamespace = 'argocd'
def argocdDestinationServer = 'https://kubernetes.default.svc'
def argocdBranch = '${branch}'
def argocdSyncLabel = 'argocd.argoproj.io/instance=test-559-10'

node('master') {
    dir(projectDirectory) {
        checkout([$class: 'GitSCM', branches: [[name: gitBranch]], userRemoteConfigs: [[credentialsId: gitId, url: gitRepo]]])
        jenkinsSteps = load 'devops/jenkinsPipelineSteps.groovy'
    }
}

stage("Authenticate") {
    node('master') {
        withCredentials([string(credentialsId: 'argocdPassword', variable: 'argocdPassword')]) {
            try {
                dir(projectDirectory) {
                    slackSend color: "warning",
                            message: "Test Project authenticating"
                    jenkinsSteps.argocdAuthenticate(argocdUrl, '${argocdPassword}')
                }
            } catch (err) {
                slackSend color: "danger",
                        message: "Test Project failed to authenticate properly"
                throw err
            }
        }
    }
}

stage("Deploy") {
    node('master') {
        try {
            dir(projectDirectory) {
                slackSend color: "warning",
                        message: "Test Project deploying"
                jenkinsSteps.argocdDeploy(argocdAppName, argocdUrl, argocdDestinationServer, gitRepo, argocdBranch,
                        argocdNamespace, ['values.yaml'])
                slackSend color: "good",
                        message: "Test Project deployed successfully"
            }
        } catch (err) {
            slackSend color: "danger",
                    message: "Test Project failed to deploy properly"
            throw err
        }
    }
}

stage("Running") {
    node('master') {
        try {
            timeout(environmentUptime) {
                input message: 'Ready to kill the environment?', ok: 'Kill Test Server'
            }
        } catch (err) {}
    }
}

stage("Teardown") {
    node('master') {
        try {
            dir(projectDirectory) {
                slackSend color: "warning",
                        message: "Test Project shutting down"
                jenkinsSteps.argocdTerminate(argocdAppName)
            }
        } catch (err) {
            slackSend color: "danger",
                    message: "Test Project failed to shut down properly"
            throw err
        }
    }
}

