/*
 * These events are sent to the application scope by default
 * Plugins can supply a custom namespace and event when requestion a confirmation using event + eventNamespace args
 * and if supplied these events will be send using the event as a prefix i.e. "signup.confirmed" if the event was passed as "signup"
 */
events = {
    /*
     * Triggered when a confirmation occurs.
     *
     * @param event object has properties:
     *    confirmationEvent - application defined event name for the confirmation
     *    email - email address that confirmed
     *    id - optional application defined id string relating to the kind of confirmation
     *
     * @return Receiver must return a Map which will be used to redirect() the user to a new page
     */ 
    confirmed(requiresReply:true)
    
    /*
     * Sent when a confirmation is attempted by a user, but the token is invalid, already consumed or otherwise broken
     *
     * @param event object has properties:
     *    confirmationEvent - application defined event name for the confirmation
     *    token - the email confirmation token that the user supplied in their link, which was not recognized
     *
     * @return Receiver must return a Map which will be used to redirect() the user to a new page
     */
    invalid()

    /*
     * Sent when a confirmation has lapsed with no successful attempt made by the user to confirm.
     *
     * @param event object has properties:
     *    confirmationEvent - application defined event name for the confirmation
     *    email - email address that timed out
     *    id - optional application defined id string relating to the kind of confirmation
     *
     * @return Nothing
     */
    timeout()
}