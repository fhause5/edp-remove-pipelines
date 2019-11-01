package com.epam.edp.removepipes.jenkins

import jenkins.model.*

class JenkinsView extends JenkinsItem {

    String applicationName

    JenkinsView(String applicationName, String viewName, Script script) {
        super(viewName, script)
        this.applicationName = applicationName
    }

    void remove() {
        try {
            def view = Jenkins.instance.getItemByFullName(applicationName).getView(name)
            view.owner.deleteView(view)
            script.println("Jenkins view \"$name\" has been removed")
        } catch (any) {
            script.println("WARNING: Jenkins view \"$name\" cannot be found")
        }
    }
}