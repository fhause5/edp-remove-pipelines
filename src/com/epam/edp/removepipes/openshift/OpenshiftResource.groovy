package com.epam.edp.removepipes.openshift

class OpenshiftResource {

    Script script
    String type
    String name

    OpenshiftResource(String type, String name, Script script) {
        this.type = type
        this.name = name
        this.script = script
    }

    void remove() {
        try {
            script.openshift.withCluster() {
                script.openshift.withProject() {
                    script.openshift.raw("delete", type, name, "--ignore-not-found=true")
                    script.println("Openshift resource \"${type}\" with name \"${name}\" has been removed")
                }
            }
        } catch (Exception ex) {
            script.error "Failed in removing \"${type}\" resource with name \"${name}\". " +
                    "Exception message:\n${ex}"
        }
    }
}