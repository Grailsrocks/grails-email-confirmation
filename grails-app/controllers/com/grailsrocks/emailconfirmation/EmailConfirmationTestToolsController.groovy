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

class EmailConfirmationTestToolsController {

	def emailConfirmationService
	
	def confirmationLinksForAddressAndEvent = {
		// We should not decode params.id this but this is a hack for Grails 1.0.2 bug
		def result = emailConfirmationService.findConfirmationByEmailAndEvent(params.email, params.event)
        if (result) {
            def link = g.createLink(controller:'emailConfirmation', action:'index', params:[id:result.confirmationToken])
            render(text:link)
        } else {
            render(status:404, text:"No pending confirmations for ${params.id}")
        }

	}
}

