package cows.memcached

import grails.core.GrailsApplication
import grails.plugins.*
import net.spy.memcached.AddrUtil
import net.spy.memcached.MemcachedClient
import net.spy.memcached.transcoders.SerializingTranscoder
import org.springframework.boot.web.servlet.FilterRegistrationBean

class MemcachedWebGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.6 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Cows Memcached" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/cows-memcached"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    @Override
    Closure doWithSpring() {{ ->
        log.warn "Memcached doWith Spring ${grailsApplication}"
        if (!isEnabled(grailsApplication)) {
            log.error "Memcached plugin is disabled"
        } else {
            if (!grailsApplication.config.memcached.servers){
                log.error "Memcached plugin is disabled, property memcached.servers is absent"
                return
            }
            log.warn("Starting memcached client ${application.config.memcached.servers}")
            memcachedClient(MemcachedClient, AddrUtil.getAddresses(grailsApplication.config.memcached.servers))
            memcachedPackedClientTranscoder(SerializingTranscoder) {
                compressionThreshold = 1024 //1k
            }

            memcachedClientTranscoder(SerializingTranscoder) {
                compressionThreshold = 20 * 1024 * 1024 //20M
            }

            log.error("Adding memcachedFilter")

            memcachedFilter(FilterRegistrationBean) {
                filter = bean(MemcachedFilter){
                    memcachedService = ref('memcachedService')
                }
                order = FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER + 1
            }
        }
    }}

    private boolean isEnabled(GrailsApplication application) {
        application.config.memcached && !application.config.memcached.disabled
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
