package com.epam.edp.removepipes.jenkins

class JenkinsItem {

    Script script
    String name

    JenkinsItem(String name, Script script) {
        this.script = script
        this.name = name
    }

    void remove() {
        try {
            Jenkins.instance.getItemByFullName(name).delete()
            script.println("Jenkins item \"$name\" has been removed")
        } catch (any) {
            script.println("WARNING: Jenkins item \"$name\" cannot be found")
        }
    }
}