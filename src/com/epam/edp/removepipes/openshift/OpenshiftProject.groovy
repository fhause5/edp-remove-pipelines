package com.epam.edp.removepipes.openshift

class OpenshiftProject extends OpenshiftResource {

    OpenshiftProject(String name, Script script) {
        super("project", name, script)
    }

    void remove() {
        try {
            script.openshift.withCluster() {
                script.openshift.raw("delete", "project", name, "--ignore-not-found=true")
                script.println("Project with name \"${name}\" has been removed")
            }
        } catch (Exception ex) {
            script.error "Failed in removing project with name \"${name}\". " +
                    "Exception message:\n${ex}"
        }
    }
}