package cows.memcached

import java.lang.annotation.*

/**
 * actions of controller cached with memcached
 */

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.METHOD, ElementType.TYPE, ElementType.FIELD])
@Inherited
@Documented
public @interface Memcached {
    int value() default 86400; //expiration in seconds
    boolean packed() default false; //use gzip
}