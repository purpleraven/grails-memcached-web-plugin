# grails-cows-cache-plugin
[ ![Download](https://api.bintray.com/packages/purpleraven/plugins/grails-memcached-web-plugin/images/download.svg) ](https://bintray.com/purpleraven/plugins/grails-memcached-web-plugin/_latestVersion)

The plugin provides possibility to store pages of page fragments in [memcached] and use it directly from web servers

thx. to ehcache web for web filter code

Grails 3.2, 3.3 supported 

Usage
-----

Base logic: 
* on first page access, nginx request memcached and receives empty result. 
* as a fallback, nginx request web application. 
* if action marked as memcached, page will be rendered and added to memcached
* response from web application will be returned to user
* on next requests, content from memcached will be used without calling web application
* dynamic parts of page can be requested by SSI [doc] on server side or by ajax from client   

Plugin can be disabled by `memcached.disabled=true` setting

Controllers actions can be marked by `@Memcached(value = 7200, packed = true)` annotation or by MemcachedHelper.mark(request, 7200)

cached content can be removed by `memcachedService.remove(url)` or `memcachedService.flush()`

SSI for dynamic content supported , see [doc]
```html
<!--# include virtual="${createLink(controller: 'controller', action: 'action', id:id)}" wait='yes'-->
```

or 
```html
<mc:memcachedTile url="${createLink(controller:'controller',action:'action', id:id))}">
   <span>Content for non-cached page</span>
</mc:memcachedTile>
```


`<mc:memcachedLog/>` shows caching time if memcached activated fror the page


Installation
------------

Add the following dependencies in `build.gradle`
```
repositories {
...
  maven { url "http://dl.bintray.com/purpleraven/plugins" }
...
}
dependencies {
...
    compile 'org.grails.plugins:grails-memcached-web-plugin:1.2'
...
}
```

      

In web application config, example for Nginx



``` 

  upstream app.port {
    server localhost:8080; # tomcat port
  }
  
  upstream memcached.port {
    server localhost:11211; # memcached port
  }
  
  # without compression, bug ssi supported
  location / {
      ssi on; 
      set $memcached_key "$uri?$args";
      memcached_pass memcached.port;
      memcached_gzip_flag 2;
      default_type text/html;
      charset utf-8;
      gunzip on;
      proxy_set_header Accept-Encoding "gzip";
      error_page  404 405 400 500 502 503 504 = @fallback;
    }
  
  location @fallback {
      ssi on;
      proxy_pass http://app.port;
      proxy_max_temp_file_size 0;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_http_version 1.1;
      proxy_set_header Connection "";
      error_page 400 500 502 503 504  /offline.html;
    }
  
  # without compression, ssi NOT supported
  location /compressed/example {
    set $memcached_key "$uri?$args";
    memcached_pass memcached.port;
    memcached_gzip_flag 2;
    default_type text/html;
    charset utf-8;
    gunzip on;
    proxy_set_header Accept-Encoding "gzip";
    error_page  404 405 400 500 502 503 504 = @compressed_fallback;
  }

  location @compressed_fallback {
    proxy_pass http://app.port;
    proxy_max_temp_file_size 0;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    error_page 400 500 502 503 504  /offline.html;
  }


```

      
License
-------
Apache 2     


[doc]: https://en.wikipedia.org/wiki/Server_Side_Includes
[memcached]: https://www.memcached.org/