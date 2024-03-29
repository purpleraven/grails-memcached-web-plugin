/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.constructs.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * RFC 2616 - HTTP/1.1 Protocol
 * Section 3.3.1 defines the preferred full date and time as:
 * <pre>
 * HTTP-date    = rfc1123-date
 * rfc1123-date = wkday "," SP date1 SP time SP "GMT"
 * date1        = 2DIGIT SP month SP 4DIGIT
 * ; day month year (e.g., 02 Jun 1982)
 * time         = 2DIGIT ":" 2DIGIT ":" 2DIGIT
 * ; 00:00:00 - 23:59:59
 * wkday        = "Mon" | "Tue" | "Wed"
 * | "Thu" | "Fri" | "Sat" | "Sun"
 * month        = "Jan" | "Feb" | "Mar" | "Apr"
 * | "May" | "Jun" | "Jul" | "Aug"
 * | "Sep" | "Oct" | "Nov" | "Dec"
 * </pre>
 * An example is <code>Sun, 06 Nov 1994 08:49:37 GMT</code>
 * These are used in request and response headers.
 * @author Greg Luck
 */
public class HttpDateFormatter {

    private static final Logger LOG = LoggerFactory.getLogger(HttpDateFormatter.class);

    private final SimpleDateFormat httpDateFormat;



    /**
     * Constructs a new formatter.
     * Note that this class is not thread-safe for use by multiple threads, as SimpleDateFormat is not.
     * Each thread should create their own instance of this class.
     */
    public HttpDateFormatter() {
        httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }


    /**
     *
     * @param date
     * @return A date formatted in accordance with Section 3.3.1 of RFC 2616
     */
    public synchronized String formatHttpDate(Date date) {
        return httpDateFormat.format(date);
    }

    /**
     * Parses dates supplied in accordance with Section 3.3.1 of RFC 2616
     * @param date a date formatted in accordance with Section 3.3.1 of RFC 2616
     * @return the parsed Date. If the date cannot be parsed, the start of POSIX time, 1/1/1970 is returned, which will
     * have the effect of expiring the content.
     */
    public synchronized Date parseDateFromHttpDate(String date) {
        try {
            return httpDateFormat.parse(date);
        } catch (ParseException e) {
            LOG.debug("ParseException on date {}. 1/1/1970 will be returned", date);
            return new Date(0);
        }
    }
}