import com.epam.edp.removepipes.database.Database
import com.epam.edp.removepipes.openshift.OpenshiftProject
import com.epam.edp.removepipes.openshift.OpenshiftResource
import com.epam.edp.removepipes.jenkins.JenkinsItem

def call() {
    def context = [:]

    node("master") {
        stage("Init") {
            final String EDP_DEPLOY_PROJECT = "edp-deploy"

            if (params.RESOURCES_VERSION_2) {
                context.stageCR = "stage.v2"
            } else {
                context.stageCR = "stage"
            }

            context.projectName = System.getenv('JENKINS_UI_URL').substring(0, System.getenv('JENKINS_UI_URL')
                    .indexOf(".")).minus("https://jenkins-").minus("-edp-cicd")

            context.database = new Database(EDP_DEPLOY_PROJECT, this)
            context.database.init()

            if (!params.INTERACTIVE_MODE) {
                context.cdPipeline = params.CD_PIPELINE
                context.cdStage = params.CD_STAGE

                if (context.database.getCdPipelines(context.projectName).contains(context.cdPipeline)) {
                    if (!context.database.getCdStages(context.projectName, context.cdPipeline).contains(context.cdStage))
                        error "CD stage \"${context.cdStage}\" not found in the \"${context.cdPipeline}\" CD pipeline"

                } else {
                    error "CD pipeline \"${context.cdPipeline}\" not found in the \"${context.projectName}\" project"
                }

            } else {
                def cdPipelineChoices = []
                def cdStageChoices = []

                cdPipelineChoices.add(choice(choices: "${context.database.getCdPipelines(context.projectName).plus('No_deletion').join('\n')}", name: "CD_PIPELINE"))
                context.cdPipeline = input id: 'cdPipeline', message: 'CD pipeline you want to remove stage in.',
                        parameters: cdPipelineChoices

                if (context.cdPipeline == "No_deletion")
                    error "Deletion aborted"

                cdStageChoices.add(choice(choices: "${context.database.getCdStages(context.projectName, context.cdPipeline).plus('No_deletion').join('\n')}", name: "CD_STAGE"))
                context.cdStage = input id: 'cdStage', message: 'CD stage you want to remove.',
                        parameters: cdStageChoices

                if (context.cdStage == "No_deletion")
                    error "Deletion aborted"
            }
        }
        stage("Remove stage custom resource") {
            new OpenshiftResource(context.stageCR, "${context.cdPipeline}-${context.cdStage}", this).remove()
        }
        stage("Remove database entries") {
            context.database.removeCdStage(context.projectName, context.cdPipeline, context.cdStage)
        }
        stage("Remove OpenShift project") {
            new OpenshiftProject("${context.projectName}-${context.cdPipeline}-${context.cdStage}", this).remove()
        }
        stage("Remove Jenkins deploy job") {
            new JenkinsItem("${context.cdPipeline}-cd-pipeline/${context.cdStage}", this).remove()
        }
        stage("Remove image streams") {
            context.database.getCdPipelineApplications(context.projectName, context.cdPipeline, params.RESOURCES_VERSION_2).each { application ->
                new OpenshiftResource("imagestream", "${context.cdPipeline}-${context.cdStage}-${application}-verified",
                        this).remove()
            }
        }
    }
}
