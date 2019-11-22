package cows.memcached


import groovy.transform.CompileStatic
import java.util.concurrent.ConcurrentHashMap


//@GrailsCompileStatic(TypeCheckingMode.SKIP)
@CompileStatic
class MemcachedMarkerInterceptor {

    final AnnotationFinder annotationFinder = new AnnotationFinder()
    final Map<String, Integer> valuesCache = new ConcurrentHashMap<String, Integer>(1000)


    MemcachedMarkerInterceptor() {
//        if (Holders.config.memcached.disabled){
//            println("TODO disable interceptor")
//        }
        matchAll()
    }

    boolean before() {
        if (controllerName==null) return true

        String key = controllerName+"|"+ actionName

        if (!valuesCache.containsKey(key)){
            Memcached memcached = annotationFinder.find(Memcached.class, controllerName, actionName)
            def value = memcached?.value()?:0
            if (memcached?.packed()){
                value = -value
            }
//            log.warn "Add to valuesCache ${key}-> ${val}"
            valuesCache.put(key, value)
        }

        Integer val = valuesCache.get(key)

        if (val!=null && val != 0){
            MemcachedHelper.mark(request, val)
        }

        true
    }
}
