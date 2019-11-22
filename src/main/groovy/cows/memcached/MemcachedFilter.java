package cows.memcached;

import cows.memcached.MemcachedService;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.AlreadyGzippedException;

import net.sf.ehcache.constructs.web.ResponseUtil;
import net.sf.ehcache.constructs.web.ResponseHeadersNotModifiableException;
import net.sf.ehcache.constructs.web.SerializableCookie;
import javax.servlet.http.Cookie;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;
import net.sf.ehcache.constructs.web.Header;

/**
 * Created by IntelliJ IDEA.
 * User: 33cows.com
 * Date: 09.04.11
 * Time: 12:32  https://oss.sonatype.org/content/repositories/sourceforge-releases/net/sf/ehcache/ehcache-web/2.0.3/
 */
public class MemcachedFilter extends OncePerRequestFilter {

    private MemcachedService memcachedService;

    public void setMemcachedService(MemcachedService service) {
        this.memcachedService = service;
    }

    public MemcachedService getMemcachedService() {
        return this.memcachedService;
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {

        String url=null;
        try {
            if (request.getHeader("X-No-Cache")==null && !request.getRequestURI().contains("/static/")) { // todo fix static
                if (response.isCommitted()) {
                    logger.warn("MemcachedFilter error: Response already committed before doing buildPage");
                    return;
                }


                url = request.getRequestURI()+'?';
                String query = request.getQueryString();
                if (query!=null && !query.isEmpty()){
                    url +=query;
                }
                PageInfo pageInfo = buildPageInfo(url,request, response, chain);
                if (response.isCommitted()) {
                    if (pageInfo.isOk()){
                        logger.warn("MemcachedFilter error: Response committed before writing response from PageInfo."+url);
                    }

                    return;
                }
                writeResponse(request, response, pageInfo);
            } else {
                chain.doFilter(request, response);
            }
        } catch (ClientAbortException e) {
            logger.warn("MemcachedFilter ClientAbortException error, url = "+url);
        } catch (Exception all) {
            logger.warn("MemcachedFilter error, url = "+url, all);
        }
    }

    /*
     * Build page info either using the cache or building the page directly.
     */
    protected PageInfo buildPageInfo(String url, final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws Exception {
        // Look up the cached page
        PageInfo pageInfo = null;
        try {
            // Page is not cached - build the response, cache it, and
            // send to client
            pageInfo = buildPage(request, response, chain);
            Integer seconds = MemcachedHelper.isMarked(request);
            if (pageInfo.isOk() && seconds!=null) {
//        LOG.debug("PageInfo ok. Adding to cache  with key " + url);
                byte[] body = pageInfo.getUngzippedBody();
                if (body!=null && body.length > 60) {
                    boolean gzip = false;
                    if (seconds<0){
                        gzip = true;
                        seconds = -seconds;
                    }
                    memcachedService.put(url, body, seconds, gzip);
                } else {
                    logger.error("Achtung!!!!! empty page "+url+", content"+ new String(body));
                }
            }
        } catch (Throwable throwable) {
            logger.error("buildPageInfo error for url: "+ url, throwable);
            pageInfo = new PageInfo(HttpServletResponse.SC_SERVICE_UNAVAILABLE, null,
                    null, throwable.getMessage().getBytes(), false, 1000, null);
        }
        return pageInfo;
    }

    /**
     * Builds the PageInfo object by passing the request along the filter chain
     *
     * @param request
     * @param response
     * @param chain
     * @return a Serializable value object for the page or page fragment
     * @throws AlreadyGzippedException
     *             if an attempt is made to double gzip the body
     * @throws Exception
     */
    protected PageInfo buildPage(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws AlreadyGzippedException, Exception {

        // Invoke the next entity in the chain
        final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        final GenericResponseWrapper wrapper = new GenericResponseWrapper( response, outstr);
        chain.doFilter(request, wrapper);
        wrapper.flush();

        // Return the page info
        return new PageInfo(wrapper.getStatus(), wrapper.getContentType(),
                wrapper.getCookies(), outstr.toByteArray(), false, 1000, wrapper.getAllHeaders()); //todo
    }

    /**
     *
     * Writes the response from a PageInfo object.
     * Headers are set last so that there is an opportunity to override
     *
     * @param request
     * @param response
     * @param pageInfo
     * @throws IOException
     * @throws DataFormatException
     * @throws ResponseHeadersNotModifiableException
     *
     */
    protected void writeResponse(final HttpServletRequest request, final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, DataFormatException, ResponseHeadersNotModifiableException {

        try{
            setStatus(response, pageInfo);
            setContentType(response, pageInfo);
            setCookies(pageInfo, response);
            // do headers last so that users can override with their own header sets
            setHeaders(pageInfo, response);
            writeContent(request, response, pageInfo);
        } catch (ClientAbortException e){
            logger.warn(e.getMessage());
        }

    }

    /**
     * Set the content type.
     *
     * @param response
     * @param pageInfo
     */
    protected void setContentType(final HttpServletResponse response,
                                  final PageInfo pageInfo) {
        String contentType = pageInfo.getContentType();
        if (contentType != null && contentType.length() > 0) {
            response.setContentType(contentType);
        }
    }

    /**
     * Set the serializableCookies
     *
     * @param pageInfo
     * @param response
     */
    protected void setCookies(final PageInfo pageInfo, final HttpServletResponse response) {
        final Collection cookies = pageInfo.getSerializableCookies();
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            final Cookie cookie = ((SerializableCookie) iterator.next()).toCookie();
            response.addCookie(cookie);
        }
    }

    /**
     * Status code
     *
     * @param response
     * @param pageInfo
     */
    protected void setStatus(final HttpServletResponse response, final PageInfo pageInfo) {
        response.setStatus(pageInfo.getStatusCode());
    }

    /**
     * Set the headers in the response object, excluding the Gzip header
     *
     * @param pageInfo
     * @param response
     */
    protected void setHeaders(final PageInfo pageInfo, final HttpServletResponse response) {

        final Collection<Header<? extends Serializable>> headers = pageInfo.getHeaders();

        // Track which headers have been set so all headers of the same name
        // after the first are added
        final TreeSet<String> setHeaders = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER);

        for (final Header<? extends Serializable> header : headers) {
            final String name = header.getName();

            Header.Type type = header.getType();
            Serializable value = header.getValue();
            if (type == Header.Type.STRING) {
                if (!setHeaders.contains(name)) {
                    setHeaders.add(name);
                }
                response.setHeader(name, (String) value);
            } else if (type == Header.Type.DATE) {
                if (!setHeaders.contains(name)) {
                    setHeaders.add(name);
                }
                response.setDateHeader(name, (Long) value);
            } else if (type == Header.Type.INT) {
                if (!setHeaders.contains(name)) {
                    setHeaders.add(name);
                }
                response.setIntHeader(name, (Integer) value);
            } else {
                throw new IllegalArgumentException("No mapping for Header: " + header);
            }
        }
    }


    /**
     * Writes the response content. This will be gzipped or non gzipped
     * depending on whether the User Agent accepts GZIP encoding.
     * If the body is written gzipped a gzip header is added.
     *
     * @param response
     * @param pageInfo
     * @throws IOException
     */
    protected void writeContent(final HttpServletRequest request, final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException, ResponseHeadersNotModifiableException {
        byte[] body;

        boolean shouldBodyBeZero = ResponseUtil.shouldBodyBeZero(request, pageInfo.getStatusCode());
        if (shouldBodyBeZero) {
            body = new byte[0];
        } else  {
            body = pageInfo.getUngzippedBody();
        }

        response.setContentLength(body.length);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(body);
        out.flush();
    }
}
