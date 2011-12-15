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
package com.grailsrocks.emailconfirmation

import grails.util.GrailsUtil

class EmailConfirmationController {

	def emailConfirmationService
	
	// Look up the supplied code and tell view what outcome was
	def index = {
		if (log.traceEnabled) log.trace("Email confirmation controller invoked with token ${params.id}")
		
		// We should not decode params.id this but this is a hack for Grails 1.0.2 bug
		def result = emailConfirmationService.checkConfirmation(
		    GrailsUtil.grailsVersion == "1.0.2" ? params.id.decodeURL() : params.id)

        // if callback specified args, do a redirect or closure invoke instead of our default view
		if ( result.action ) {
        	flash.success = result.valid
        	if (result.valid) {
            	flash.email = result.email
            	flash.token = result.token
        	}

            // Was the result a redirect args map?
            if (result.action instanceof Map) {
		        redirect( result.action)
	        } else if (result.action instanceof Closure) {
	            // No it was a closure to do application "magic" in our context
	            def code = result.action.clone()
	            code.delegate = this
	            code.resolveStrategy = Closure.DELEGATE_FIRST
	            code()
	        }
		} else {
		    return [success: result.valid, email: result.email]
		}
	}
}

