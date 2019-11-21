package cows.memcached;

import javax.servlet.ServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: 33cows.com
 * Date: 13.04.11
 * Time: 18:45
 */
public class CacheHelper {
    public static final String MEMCACHED = "X-Memcached-Cached";
    public static Integer getAnnotation(ServletRequest request){
        return (Integer) request.getAttribute(MEMCACHED);
    }

    public static void setMemcachedAnnotation(ServletRequest request, Integer seconds){
        request.setAttribute(MEMCACHED, seconds);
    }

    public static void removeAnnotation(ServletRequest request){
        request.removeAttribute(MEMCACHED);
    }

    public static boolean isCached(ServletRequest request) {
        return request.getAttribute(MEMCACHED)!=null;
    }
}
