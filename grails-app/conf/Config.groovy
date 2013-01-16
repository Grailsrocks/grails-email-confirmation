grails.doc.title = "Email Confirmation"
grails.doc.subtitle = "Email Confirmation - for sending and verifying confirmation emails"
grails.doc.images = new File("resources/img")
grails.doc.authors = "Marc Palmer (marc@grailsrocks.com)"
grails.doc.license = "ASL 2"
grails.doc.copyright = "&copy; 2013 Marc Palmer"
grails.doc.footer = "Please contact the author with any corrections or suggestions"
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"


log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
    debug 'com.grailsrocks',
          'org.grails.plugin.platform',
          'grails.app.controller',
          'grails.app.service'

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    warn   'org.mortbay.log'
}
