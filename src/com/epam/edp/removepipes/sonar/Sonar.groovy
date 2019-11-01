package com.epam.edp.removepipes.sonar

class Sonar {
    Script script
    String sonarToken
    String sonarHost

    Sonar(Script script) {
        this.script = script
    }

    void init() {
        script.withSonarQubeEnv('Sonar') {
            sonarToken = "${script.env.SONAR_AUTH_TOKEN}:".bytes.encodeBase64().toString()
            sonarHost = script.env.SONAR_HOST_URL
        }
    }

    void removeCodeReviewProjects(String codebaseName) {
        def response = script.httpRequest url: "${sonarHost}/api/projects/search?qualifiers=TRK&q=${codebaseName}",
                httpMode: 'GET',
                customHeaders: [[name: 'Authorization', value: "Basic ${sonarToken}"]],
                quiet: true
        def parsedResponse = new groovy.json.JsonSlurperClassic().parseText(response.content)
        parsedResponse.components.each {
            if (it.key.startsWith("${codebaseName}:change-"))
                removeProject("${it.key}")
        }
    }

    void removeProject(String projectKey) {
        try {
            script.httpRequest url: "${sonarHost}/api/projects/delete?key=${projectKey}",
                    httpMode: 'POST',
                    customHeaders: [[name: 'Authorization', value: "Basic ${sonarToken}"]],
                    quiet: true
            script.println("Sonar project \"${projectKey}\" has been removed.")
        }
        catch (Exception ex) {
            script.println("WARN: Sonar project \"${projectKey}\" not found. Skipped.")
        }
    }
}