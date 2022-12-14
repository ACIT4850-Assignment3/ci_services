def call(dockerRepoName, imageName, portNum) {
    pipeline {
    agent any
    parameters {
        booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
    }
    stages {
        stage('Build') {
            steps {
                sh 'pip install -r requirements.txt'
            }
        }
        stage('Python Lint') {
            steps {
                sh 'pylint --fail-under=5.0 *.py'
            }
        }
        stage('Zip Artifacts') {
            steps {
                sh 'zip app.zip *.py'
                archiveArtifacts artifacts: "app.zip", fingerprint: true, onlyIfSuccessful: true
            }
        } 
        stage('Package') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' }
            }
            steps {
                withCredentials([string(credentialsId: 'des0', variable: 'TOKEN')]) {
                    sh "docker login -u 'des0' -p '$TOKEN' docker.io"
                    sh "docker build -t ${dockerRepoName}:latest --tag des0/${dockerRepoName}:${imageName} ."
                    sh "docker push des0/${dockerRepoName}:${imageName}"
                }
            }
        }
        stage('Deploy') {
            when {
                expression {params.DEPLOY}
            }
            steps {
                sshagent(credentials : ['rj-3855']) {

                    sh "ssh -o StrictHostKeyChecking=no kafka@acit3855-kafka-lab6a.eastus.cloudapp.azure.com 'cd deployment; docker-compose down ;docker-compose pull ;docker-compose up -d --build'"
                    // sh "docker stop ${dockerRepoName} || true && docker rm ${dockerRepoName} || true"
                    // sh "docker run -d -p ${portNum}:${portNum} --name ${dockerRepoName} ${dockerRepoName}:latest"
                   
                    // sh "docker-compose up -d"
                }
            }
        }                                                                                                                               
    }
}
}