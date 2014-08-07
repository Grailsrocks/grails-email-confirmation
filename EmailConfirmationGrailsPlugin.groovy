/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import grails.util.Environment


class EmailConfirmationGrailsPlugin {
	def version = "3.0.0"

    def grailsVersion = "2.4 > *"

    def loadAfter = ['logging']

    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp",
        "grails-app/views/index.gsp"
    ]

    def author = "Marc Palmer"
    def authorEmail = "marc@grailsrocks.com"
    def title = "Email Confirmation"
    def description = '''\\
Send emails to users to perform click-through confirmations of any kind.
'''

        // URL to the plugin's documentation
    def documentation = "http://grailsrocks.github.com/grails-email-confirmation"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "Grails Rocks", url: "http://grailsrocks.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Marc Palmer", email: "marc@grailsrocks.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPEMAILCONFIRMATION" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://github.com/grailsrocks/grails-email-confirmation" ]

    def doWithWebDescriptor = { xml ->
    }

    def doWithSpring = {
    }

    def doWithDynamicMethods = { ctx ->
    }

    def doWithApplicationContext = { ctx ->
    }

    def doWithConfigOptions = {
        'from'(type:String, defaultValue:'admin@localhost')
    }

    def onChange = { event ->
    }

    def onApplicationChange = { event ->
    }

    def onConfigChange = { event ->
    }

    def onShutdown = { event ->
    }
}
