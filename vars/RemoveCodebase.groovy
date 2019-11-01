import com.epam.edp.removepipes.database.Database
import com.epam.edp.removepipes.openshift.OpenshiftResource
import com.epam.edp.removepipes.jenkins.JenkinsItem
import com.epam.edp.removepipes.gerrit.Gerrit
import com.epam.edp.removepipes.sonar.Sonar

def call() {
    def context = [:]

    node("master") {
        stage("Init") {
            final String EDP_DEPLOY_PROJECT = "edp-deploy"

            if (params.RESOURCES_VERSION_2) {
                context.codebaseBranchCR = "codebasebranch.v2"
                context.codebaseCR = "codebase.v2"
            } else {
                context.codebaseBranchCR = "codebasebranch"
                context.codebaseCR = "codebase"
            }

            context.projectName = System.getenv('JENKINS_UI_URL').substring(0, System.getenv('JENKINS_UI_URL')
                    .indexOf(".")).minus("https://jenkins-").minus("-edp-cicd")

            context.database = new Database(EDP_DEPLOY_PROJECT, this)
            context.database.init()

            context.sonar = new Sonar(this)
            context.sonar.init()

            if (!params.INTERACTIVE_MODE) {
                context.codebase = params.CODEBASE

                if (!context.database.getCodebases(context.projectName).contains(context.codebase))
                    error "Codebase \"${context.codebase}\" not found in the \"${context.projectName}\" project"

            } else {
                def codebaseChoices = []

                codebaseChoices.add(choice(choices: "${context.database.getCodebases(context.projectName).plus('No_deletion').join('\n')}", name: "CODEBASE"))
                context.codebase = input id: 'application', message: 'Codebase you want to remove.',
                        parameters: codebaseChoices

                if (context.codebase == "No_deletion")
                    error "Deletion aborted"
            }
            context.codebaseType = context.database.getCodebaseType(context.projectName, context.codebase)
        }
        stage("Check that codebase is not in use") {
            ArrayList cdPipelines
            String errorMessage
            switch (context.codebaseType) {
                case "autotests":
                    cdPipelines = context.database.getAutotestsCdPipelines(context.projectName, context.codebase, params.RESOURCES_VERSION_2)
                    errorMessage = "Autotests \"${context.codebase}\" cannot be removed while CD pipelines use it."
                    break
                case "application":
                    cdPipelines = context.database.getApplicationCdPipelines(context.projectName, context.codebase, params.RESOURCES_VERSION_2)
                    errorMessage = "Application \"${context.codebase}\" cannot be removed while CD pipelines use it."
                    break
                default:
                    break
            }
            if (cdPipelines.size() != 0) {
                println "Found ${context.codebaseType} \"${context.codebase}\" usage in CD pipelines:"
                cdPipelines.unique().each { pipeline ->
                    println "- ${pipeline}"
                }
                error errorMessage
            }
        }
        stage("Remove codebase branches") {
            context.database.getCodebaseBranches(context.projectName, context.codebase).each { branch ->
                new OpenshiftResource(context.codebaseBranchCR, "${context.codebase}-${branch}", this).remove()
                context.database.removeCodebaseBranch(context.projectName, context.codebase, branch, params.RESOURCES_VERSION_2)
                if (context.codebaseType != "autotests") {
                    new OpenshiftResource("imagestream", "${context.codebase}-${branch}", this).remove()
                    new OpenshiftResource("bc", "${context.codebase}-${branch}", this).remove()
                    if (params.REMOVE_SONAR_PROJECTS)
                        context.sonar.removeProject("${context.codebase}:${branch}")

                }
            }
        }
        stage("Remove custom resources") {
            new OpenshiftResource(context.codebaseCR, context.codebase, this).remove()
        }
        stage("Remove database entries") {
            context.database.removeCodebase(context.projectName, context.codebase, params.RESOURCES_VERSION_2)
        }
        stage("Remove Jenkins folder") {
            new JenkinsItem(context.codebase, this).remove()
        }
        stage("Remove Gerrit project") {
            if (params.REMOVE_GERRIT_PROJECT)
                new Gerrit(this).removeProject(params.GERRIT_SSH_PORT, context.codebase)
        }
        stage("Remove code review sonar projects") {
            if (params.REMOVE_SONAR_PROJECTS)
                context.sonar.removeCodeReviewProjects(context.codebase)
        }
    }
}
