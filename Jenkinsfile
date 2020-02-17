import groovy.transform.Field

@Field 
def shouldWaitForQualityGate = false

pipeline {
    agent any
    tools {
        maven 'Maven 3.5.4'
    }
    stages {
        stage ('Build') {
            when {
                expression { env.CHANGE_ID != null || env.BRANCH_NAME == "master" }
            }
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Code Quality Analysis') {
            steps {
                script {
                    def scannerHome = tool 'SonarQube Scanner Latest';
                    withSonarQubeEnv("ec2-server") {
                        if (env.CHANGE_ID != null) {
                            sh "${scannerHome}/bin/sonar-scanner -Dsonar.branch.name=$CHANGE_BRANCH -Dsonar.branch.target=$CHANGE_TARGET"
                            shouldWaitForQualityGate = true
                        } else if (env.BRANCH_NAME == "master") {
                            sh "${scannerHome}/bin/sonar-scanner"
                            shouldWaitForQualityGate = true
                        }
                    }
                }
            }
        }
        stage("Quality Gate") {
            when {
                expression { shouldWaitForQualityGate == true }
            }
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    // Parameter indicates whether to set pipeline to UNSTABLE if Quality Gate fails
                    // true = set pipeline to UNSTABLE, false = don't
                    waitForQualityGate abortPipeline: true
                }
            }
        }
    }
}

