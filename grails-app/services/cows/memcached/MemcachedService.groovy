package cows.memcached

import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.MemcachedClient
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class MemcachedService {
  private static final Log LOG = LogFactory.getLog(MemcachedService)

  static transactional = false
  MemcachedClient memcachedClient
  Transcoder memcachedClientTranscoder
  Transcoder memcachedPackedClientTranscoder
  def grailsApplication

  @Lazy
  def memcachedEnabled =  {
    !grailsApplication.config.memcached.disabled
  }()

  def put(String key, Object content, int exp, gzip){
    try{
      if (memcachedEnabled) {
        def transcoder = gzip ? memcachedPackedClientTranscoder : memcachedClientTranscoder
        memcachedClient.add(key, exp, content, transcoder)
      }
    } catch (all){
      LOG.error("memcached: adding ($key)", all)
    }
  }

  def remove(String key) {
    try {
      if (memcachedEnabled){
        memcachedClient.delete(key)
      }
    } catch (all) {
      LOG.error("memcached: removing ", all)
    }

  }

  def flush(){
    if (memcachedEnabled){
      memcachedClient.flush()
    }
  }
}
