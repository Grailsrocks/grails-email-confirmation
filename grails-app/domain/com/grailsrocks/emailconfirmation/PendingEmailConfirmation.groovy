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
 
import java.security.*

class PendingEmailConfirmation implements Serializable { 
    static prng = new SecureRandom()
    
    String emailAddress
	String confirmationToken = "?"
	String userToken
	String confirmationEvent
	
	Date timestamp = new Date()

    void makeToken()
    {
        // @todo replace deadbeef with some random hex digits
        def uidBytes = new byte[30]
        prng.nextBytes(uidBytes)
        
		confirmationToken = uidBytes.encodeAsBase62()
    }

    static mapping = {
        confirmationToken index:'emailconf_token_Idx'
        timestamp index:'emailconf_timestamp_Idx'
    }
    
	static constraints = {
	    emailAddress(size:1..80, email:true)
		confirmationToken(size:1..80)
		confirmationEvent(nullable:true, size:0..80)
		// Allow quite a bit of space here for app supplied data
		userToken(size:0..500, nullable: true, blank:true)
	}
}	
