/* Copyright 2019 EPAM Systems.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.*/

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