package com.epam.edp.removepipes.gerrit

class Gerrit {
    Script script

    Gerrit(Script script) {
        this.script = script
    }

    void removeProject(String gerritSshPort, String projectName) {
        try {
            script.sh "ssh -p ${gerritSshPort} gerrit delete-project delete --yes-really-delete --force ${projectName}"
            script.println("Gerrit project \"${projectName}\" has been removed.")
        } catch (any) {
            script.println("WARN: gerrit project \"${projectName}\" has not been removed")
        }
    }
}