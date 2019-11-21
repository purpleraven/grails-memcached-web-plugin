import cows.memcached.CacheHelper

class MemcachedTagLib {
  static namespace = "mc"

  @Lazy
  def memcachedEnabled =  {
    !grailsApplication.config.memcached.disabled
  }()

  def memcachedTile = {attrs, body ->
    if (memcachedEnabled && CacheHelper.isCached(request)) {
      out << "<!--# include virtual=\"${attrs.url}\" wait='yes' -->" //todo if url is empty
    } else {
      out << body()
    }
  }

  def memcachedLog = {attrs, body ->
    if (memcachedEnabled && CacheHelper.isCached(request)) {
      out << "<!-- memcached ${new Date()} -->"
    }
  }

  def doOnCachedPage={attrs, body ->
    if (memcachedEnabled && CacheHelper.isCached(request)) {
      out << body()
    }
  }

  def doOnNotCachedPage={attrs, body ->
    if (!memcachedEnabled || !CacheHelper.isCached(request)) {
      out << body()
    }
  }
}
