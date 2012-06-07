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
 
import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext


import grails.util.Environment


class EmailConfirmationService implements ApplicationContextAware {

	static EVENT_TYPE_CONFIRMED = 'confirmed'
	static EVENT_TYPE_INVALID = 'invalid'
	static EVENT_TYPE_TIMEOUT = 'timeout'
	
	def mailService
	
	LinkGenerator grailsLinkGenerator

	boolean transactional = true

	def maxAge = 1000*60*60*24*30L // 30 days

	/**
	 * This closure can be assigned by the application and must return true if the userToken is valid.
	 * This code needs to be threadsafe
	 */
	Closure onConfirmation = { email, userToken -> log.error "Your application received an email confirmation event it did not handle!"}
	
	Closure onInvalid = { invalidToken -> log.error  "Your application received an invalid email cofirmation event it did not handle!"}
	
	Closure onTimeout = { email, userToken -> log.error  "Your application received an email cofirmation timeout event it did not handle!"}
	
	// Auto populated by ApplicationContextAware
	ApplicationContext applicationContext
	
	def grailsApplication
	
	def makeURL(token) {
		//@todo this needs to change to do a reverse mapping lookup
		//@todo also if uri already exists in binding, append token to it
		def serverURL = grailsLinkGenerator.serverBaseURL
    	"${serverURL}/confirm/${token.encodeAsURL()}"
	}
	
	/**
	 * Send a new email confirmation. This will trigger platform events when completed or timed out.
	 * Invalid tokens generate a separate "emailConfirmation.invalid" event
	 *
	 * @param args Map of arguments:
	 * to - Required, email address
	 * id - Required, application-specific token used in callbacks when this confirmation is complete/invalid
	 * subject - Required, subject for the email
	 * event - Required, name for event callbacks, used as stem i.e. for "signupConfirmation" you'll get
	 * "signupConfirmation.confirmed" or "signupConfirmation.timeout" events fired
	 * from - Optional, sender from address, defaults to config's emailConfirmation.from
	 * model - Optional, model to use in the email GSP view
	 * view - Optional, path to GSP view to use for the email body
	 * plugin - Optional, the "filesystem" name of the plugin
	 */
	def sendConfirmation(Map args) {
		if (log.infoEnabled) {
			log.info "Sending email confirmation mail to ${args.to}, callback events will be named [${args.event}.*], user data is [${args.id}])"
		}
		def conf = new PendingEmailConfirmation(
		    emailAddress:args.to, 
		    userToken:args.id, 
		    confirmationEvent:args.eventScope ? args.eventScope+'#'+args.event : args.event)
        conf.makeToken()
        
        if (!conf.save()) {
			throw new IllegalArgumentException( "Unable to save pending confirmation: ${conf.errors}")
		}
		
		def binding = args.model ? new HashMap(args.model) : [:]
	
		binding['uri'] = makeURL(conf.confirmationToken)

		if (log.infoEnabled) {
			log.info( "Sending email confirmation mail to $args.to - confirmation link is: ${binding.uri}")
		}
		
		def defaultView = args.view == null
		def viewName = defaultView ? "/emailconfirmation/mail/confirmationRequest" : args.view
        def pluginName = defaultView ? "email-confirmation" : args.plugin

		try {
    		mailService.sendMail {
    			to args.to 
    			from args.from ?: pluginConfig.from
    			subject args.subject
    			def bodyArgs = [view:viewName, model:binding]
    			if (pluginName) {
    			    bodyArgs.plugin = pluginName
    			}
    			body( bodyArgs)
    	    }
		} catch (Throwable t) {
		    if (Environment.current == Environment.DEVELOPMENT) {
		        log.warn "Mail sending failed but you're in development mode so I'm ignoring this fact, you can confirm using this link: ${binding.uri}"
                log.error "Mail send failed", t
		    } else {
	            throw t
            }
		}
		return conf
    }
    
    /**
     * Legacy confirmation method
     * @deprecated
     */
	def sendConfirmation(String emailAddress, String thesubject,  
		    Map binding = null, String userToken = null) {
        sendConfirmation(to:emailAddress, subject:thesubject, model:binding, id:userToken)
	}

	def fireEvent(String callbackType, String appEventPath, Map args, Closure legacyHandler) {
	    
        def eventScope
        def n = appEventPath.indexOf('#')
        if (n > -1) {
            eventScope = appEventPath[0..n-1]
            if (n < appEventPath.length) {
                args.confirmationEvent = appEventPath[n+1..-1]
            }
        } else {
            args.confirmationEvent = appEventPath
        }
	    def result
	    if (eventScope) {
	        result = event(callbackType, args).value
        } else {
	        result = event(scope:eventScope, topic:callbackType, args).value
        }
        
        if (!result) {
		    // Legacy only
		    if (log.warnEnabled) {
		        log.warn "DEPRECATED Calling legacy event handler, change your code to use platform events instead"
	        }
	        result = legacyHandler()	
	    }
	    return result
	}
	
	def checkConfirmation(String confirmationToken) {
		if (log.traceEnabled) log.trace("checkConfirmation looking for confirmation token: $confirmationToken")
		def conf = PendingEmailConfirmation.findByConfirmationToken(confirmationToken)
		// 100% double check that the token in the found object matches exactly. Some lame databases
		// are case insensitive for searches, which reduces the possible token space
		if (conf && (conf.confirmationToken == confirmationToken)) {
			if (log.debugEnabled) {
				log.debug( "Notifying application of valid email confirmation for user token ${conf.userToken}, email ${conf.emailAddress}")
			}
			// Tell application it's ok
			def result = fireEvent(EVENT_TYPE_CONFIRMED, conf.confirmationEvent, [email:conf.emailAddress, id:conf.userToken], {
                onConfirmation?.clone().call(conf.emailAddress, conf.userToken)					    
			})
			
			conf.delete()
			return [valid: true, actionToTake:result, email: conf.emailAddress, token:conf.userToken]
		} else {
			if (log.traceEnabled) {
			    log.trace("checkConfirmation did not find confirmation token: $confirmationToken")
		    }
			def result = fireEvent(EVENT_TYPE_INVALID, null, [token:confirmationToken], {
                onInvalid?.clone().call(confirmationToken)					    
			})
			
			return [valid:false, actionToTake:result]
		}
	}
	
	void cullStaleConfirmations() {
		if (log.infoEnabled) {
			log.info( "Checking for stale email confirmations...")
		}
		def threshold = System.currentTimeMillis() - maxAge
	    def staleConfirmations = PendingEmailConfirmation.findAllByTimestampLessThan(new Date(threshold))
		def c = 0
	    // @todo change this to a scrollable criteria to avoid blowing heap
		// @todo need to clear the session occasionally!
		staleConfirmations.each() {
			// Tell application
			if (log.debugEnabled) {
				log.debug( "Notifying application of stale email confirmation for user token ${it.userToken}")
			}
			fireEvent(EVENT_TYPE_TIMEOUT, it.confirmationEvent, [email:it.emailAddress, id:it.userToken], {
    			onTimeout.clone().call( it.emailAddress, it.userToken )
			})
			
			it.delete()
			c++
		}
		if (log.infoEnabled) {
			log.info( "Done check for stale email confirmations, found $c")
		}
	}

}

