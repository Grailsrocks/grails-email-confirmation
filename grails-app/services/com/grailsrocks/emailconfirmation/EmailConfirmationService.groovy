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

import org.springframework.transaction.annotation.Transactional
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext

import grails.util.Environment
import grails.util.Holders

import java.rmi.server.UID
import java.security.*

class EmailConfirmationService implements ApplicationContextAware {

    static prng = new SecureRandom()

    static EVENT_NAMESPACE = "plugin.emailConfirmation"

	static EVENT_TYPE_CONFIRMED = 'confirmed'
	static EVENT_TYPE_INVALID = 'invalid'
	static EVENT_TYPE_TIMEOUT = 'timeout'

	def mailService

	boolean transactional = true

	def maxAge = 1000*60*60*24*30L // 30 days

	/**
	 * This closure can be assigned by the application and must return true if the userToken is valid.
	 * This code needs to be threadsafe
	 */
	Closure onConfirmation = { email, userToken -> log.error "Your application received an email confirmation event it did not handle!"}

	Closure onInvalid = { invalidToken -> log.error  "Your application received an invalid email confirmation event it did not handle!"}

	Closure onTimeout = { email, userToken -> log.error  "Your application received an email confirmation timeout event it did not handle!"}

	// Auto populated by ApplicationContextAware
	ApplicationContext applicationContext

	def grailsApplication

	def makeURL(token) {
	    def grailsApplication = Holders.grailsApplication
		def serverURL = grailsApplication.config.grails.serverURL ?: 'http://localhost:8080/'+grailsApplication.metadata.'app.name'
        // @todo we should reverse-map this but currently you'd have to hack the plugin anyway so no point...
    	"${serverURL}/confirm/${token.encodeAsURL()}"
	}

	private makeConfirmationEventString(Map args) {
        def eventNamespaceAndPrefix = args.eventNamespace ?: EVENT_NAMESPACE
        eventNamespaceAndPrefix += '#'
        if (args.event) {
            eventNamespaceAndPrefix += args.event
        }
        return eventNamespaceAndPrefix
	}

    void makeToken(confirmation)
    {
        def uid = confirmation.emailAddress + new UID().toString() + prng.nextLong() + System.currentTimeMillis()
        def hash = uid.encodeAsSHA256Bytes()
        def token = hash.encodeAsBase62()
        confirmation.confirmationToken = token
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
     * eventNamespace - Optional, namespaces the events triggered e.g. so that your plugin receives them
	 * from - Optional, sender from address, defaults to config's emailConfirmation.from
	 * model - Optional, model to use in the email GSP view
	 * view - Optional, path to GSP view to use for the email body
	 * plugin - Optional, the "filesystem" name of the plugin
	 */
    @Transactional
	def sendConfirmation(Map args) {
		if (log.infoEnabled) {
			log.info "Sending email confirmation mail to ${args.to}, callback events will be prefixed with " +
                     "[${args.event ? args.event+'.' : ''}] in namespace [${args.eventNamespace ?: EVENT_NAMESPACE}], user data is [${args.id}])"
		}
		def conf = new PendingEmailConfirmation(
		    emailAddress:args.to,
		    userToken:args.id,
		    confirmationEvent:makeConfirmationEventString(args))
        makeToken(conf)

        if (log.debugEnabled) {
            log.debug "Created email confirmation token [${conf.confirmationToken}] for mail to ${conf.emailAddress}"
        }

        if (!conf.save()) {
			throw new IllegalArgumentException( "Unable to save pending confirmation: ${conf.errors}")
		}

		def binding = args.model ? new HashMap(args.model) : [:]

		binding['uri'] = makeURL(conf.confirmationToken)

		if (log.infoEnabled) {
			log.info( "Sending email confirmation mail to $args.to - confirmation link is: ${binding.uri}")
		}

		def defaultView = args.view == null
		def viewName = defaultView ? "/emailConfirmation/mail/confirmationRequest" : args.view
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
    @Transactional
	def sendConfirmation(String emailAddress, String thesubject,
		    Map binding = null, String userToken = null) {
        sendConfirmation(to:emailAddress, subject:thesubject, model:binding, id:userToken)
	}

    /**
     * Find the most recent confirmation URL sent to a given address, useful for tests that
     * don't want to receive emails and parse them in order to provide a confirmation
     * @param email The email address of the user
     * @param args The optional arguments for the confirmation to find, including e.g. id, event and eventNamespace
     * as used when sending the event. If not specified these values will not be checked
     */
    @Transactional
    def findLastConfirmationUrlFor(String email, Map args) {
        def confirmationEvent = makeConfirmationEventString(args)
	    def userToken = args.id

	    def pending = PendingEmailConfirmation.withCriteria({
	        eq('emailAddress', email)
	        if (confirmationEvent) {
	            eq('confirmationEvent', confirmationEvent)
	        }
	        if (userToken) {
	            eq('userToken', userToken)
	        }
	        order('timestamp', 'desc')
	        maxResults(1)
	    })

	    if (pending.size()) {
	        return makeURL(pending[0].confirmationToken)
	    } else {
	        return null
	    }
    }

	def fireEvent(String callbackType, String appEventPath, Map args, Closure legacyHandler, boolean expectsResult = true) {

        def eventNamespace
        def eventTopic
        if (!appEventPath) {
            appEventPath = ''
        }
        def n = appEventPath.indexOf('#')
        if (n > -1) {
            eventNamespace = appEventPath[0..n-1]
            if (n < appEventPath.size()-1) {
                eventTopic = appEventPath[n+1..-1]
            }
        } else {
            eventTopic = appEventPath
        }
	    def result
	    // If app-supplied callback topic, use that plus callback type, else just our declared callback types
	    eventTopic = eventTopic ? "${eventTopic}.${callbackType}" : callbackType
	    if (!eventNamespace) {
	        // Assume its an old event - purely for legacy. 'app' namespace should not be used
	        result = event(topic:eventTopic, namespace:EVENT_NAMESPACE, data:args, params:[fork:false]).value

            if (!result && expectsResult) {
    		    // Legacy only
    		    if (log.warnEnabled) {
    		        log.warn "No event listener for namespace:$EVENT_NAMESPACE and topic:'$eventTopic'. Calling DEPRECATED legacy event handler, change your code to use platform events instead"
    	        }
    	        result = legacyHandler(args)
    	    }

        } else {
	        result = event(namespace:eventNamespace, topic:eventTopic, data:args, fork:false).value

            if (!result && expectsResult) {
                if (log.warnEnabled) {
                    log.warn "No event handler was found for namespace ${eventNamespace} and topic ${eventTopic} or the result was null, confirmation event was effectively lost"
                }
                result = legacyHandler(args)
            }
        }

	    return result
	}

    /**
     * Validate a confirmation token and return a Map indicating "valid" and "actionToTake"
     */
    @Transactional
	def checkConfirmation(String confirmationToken) {
		if (log.traceEnabled) {
            log.trace("checkConfirmation looking for confirmation token: $confirmationToken")
        }

		def conf
        if (confirmationToken) {
            conf = PendingEmailConfirmation.findByConfirmationToken(confirmationToken)
            if (conf) {
                conf = PendingEmailConfirmation.lock(conf.ident())
            }
        }

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

    @Transactional
	void cullStaleConfirmations(long olderThan = 0) {
		if (log.infoEnabled) {
			log.info( "Checking for stale email confirmations...")
		}
		def threshold = olderThan ?: System.currentTimeMillis() - maxAge
	    def staleConfirmationIds = PendingEmailConfirmation.withCriteria {
            projections {
                property('id')
            }
            lt('timestamp', new Date(threshold))
        }

		def c = 0
        // This is unlikely to be too expensive for most people
        staleConfirmationIds.each { id ->
            def confirmation = PendingEmailConfirmation.lock(id)
            if (confirmation) {
    			// Tell application
    			if (log.debugEnabled) {
    				log.debug( "Notifying application of stale email confirmation for user token ${confirmation.userToken}")
    			}
    			fireEvent(EVENT_TYPE_TIMEOUT, confirmation.confirmationEvent,
                    [email:confirmation.emailAddress,
                     id:confirmation.userToken], {
        			onTimeout.clone().call( confirmation.emailAddress, confirmation.userToken )
    			}, false)

    			confirmation.delete()
    			c++
            }
		}
		if (log.infoEnabled) {
			log.info( "Done check for stale email confirmations, found $c")
		}
	}

}

