package cows.memcached;

import javax.servlet.ServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: 33cows.com
 * Date: 13.04.11
 * Time: 18:45
 */
public class MemcachedHelper {
    public static final String MEMCACHED = "X-Memcached-Cached";

    public static Integer isMarked(ServletRequest request){
        return (Integer) request.getAttribute(MEMCACHED);
    }

    public static void mark(ServletRequest request, Integer seconds){
        request.setAttribute(MEMCACHED, seconds);
    }

    public static void unmark(ServletRequest request){
        request.removeAttribute(MEMCACHED);
    }

    public static boolean isCached(ServletRequest request) {
        return request.getAttribute(MEMCACHED)!=null;
    }
}
