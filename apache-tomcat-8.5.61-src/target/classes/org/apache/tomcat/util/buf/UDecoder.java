/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.buf;

import java.io.ByteArrayOutputStream;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

/**
 * All URL decoding happens here. This way we can reuse, review, optimize
 * without adding complexity to the buffers.
 * <p>
 * The conversion will modify the original buffer.
 *
 * @author Costin Manolache
 */
public final class UDecoder {

    private static final StringManager sm = StringManager.getManager(UDecoder.class);

    private static final Log log = LogFactory.getLog(UDecoder.class);

    @Deprecated
    public static final boolean ALLOW_ENCODED_SLASH =
            Boolean.parseBoolean(System.getProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "false"));

    private static class DecodeException extends CharConversionException {
        private static final long serialVersionUID = 1L;

        public DecodeException(String s) {
            super(s);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            // This class does not provide a stack trace
            return this;
        }
    }

    /**
     * Unexpected end of data.
     */
    private static final IOException EXCEPTION_EOF = new DecodeException(sm.getString("uDecoder.eof"));

    /**
     * %xx with not-hex digit
     */
    private static final IOException EXCEPTION_NOT_HEX_DIGIT = new DecodeException(
            "isHexDigit");

    /**
     * %-encoded slash is forbidden in resource path
     */
    private static final IOException EXCEPTION_SLASH = new DecodeException(
            "noSlash");


    /**
     * URLDecode, will modify the source. Assumes source bytes are encoded using
     * a superset of US-ASCII as per RFC 7230. "%2f" will be rejected unless the
     * input is a query string.
     *
     * @param mb    The URL encoded bytes
     * @param query {@code true} if this is a query string. For a query string
     *              '+' will be decoded to ' '
     * @throws IOException Invalid %xx URL encoding
     */
    public void convert(ByteChunk mb, boolean query) throws IOException {
        if (query) {
            convert(mb, true, EncodedSolidusHandling.DECODE);
        } else {
            convert(mb, false, EncodedSolidusHandling.REJECT);
        }
    }


    /**
     * URLDecode, will modify the source. Assumes source bytes are encoded using
     * a superset of US-ASCII as per RFC 7230.
     *
     * @param mb                     The URL encoded bytes
     * @param encodedSolidusHandling How should the %2f sequence handled by
     *                               the decoder? For query strings this
     *                               parameter will be ignored and the
     *                               %2f sequence will be decoded
     * @throws IOException Invalid %xx URL encoding
     */
    public void convert(ByteChunk mb, EncodedSolidusHandling encodedSolidusHandling) throws IOException {
        convert(mb, false, encodedSolidusHandling);
    }


    private void convert(ByteChunk mb, boolean query, EncodedSolidusHandling encodedSolidusHandling) throws IOException {

        int start = mb.getOffset();
        byte buff[] = mb.getBytes();
        int end = mb.getEnd();

        int idx = ByteChunk.findByte(buff, start, end, (byte) '%');
        int idx2 = -1;
        if (query) {
            idx2 = ByteChunk.findByte(buff, start, (idx >= 0 ? idx : end), (byte) '+');
        }
        if (idx < 0 && idx2 < 0) {
            return;
        }

        // idx will be the smallest positive index ( first % or + )
        if ((idx2 >= 0 && idx2 < idx) || idx < 0) {
            idx = idx2;
        }

        for (int j = idx; j < end; j++, idx++) {
            if (buff[j] == '+' && query) {
                buff[idx] = (byte) ' ';
            } else if (buff[j] != '%') {
                buff[idx] = buff[j];
            } else {
                // read next 2 digits
                if (j + 2 >= end) {
                    throw EXCEPTION_EOF;
                }
                byte b1 = buff[j + 1];
                byte b2 = buff[j + 2];
                if (!isHexDigit(b1) || !isHexDigit(b2)) {
                    throw EXCEPTION_NOT_HEX_DIGIT;
                }

                j += 2;
                int res = x2c(b1, b2);
                if (res == '/') {
                    switch (encodedSolidusHandling) {
                        case DECODE: {
                            buff[idx] = (byte) res;
                            break;
                        }
                        case REJECT: {
                            throw EXCEPTION_SLASH;
                        }
                        case PASS_THROUGH: {
                            buff[idx++] = buff[j - 2];
                            buff[idx++] = buff[j - 1];
                            buff[idx] = buff[j];
                        }
                    }
                } else {
                    buff[idx] = (byte) res;
                }
            }
        }

        mb.setEnd(idx);

    }

    // -------------------- Additional methods --------------------

    /**
     * In-buffer processing - the buffer will be modified.
     * <p>
     * <b>WARNING:</b> This method assumes US-ASCII encoding.
     *
     * @param mb    The URL encoded chars
     * @param query <code>true</code> if this is a query string
     * @throws IOException Invalid %xx URL encoding
     * @deprecated Unused. Will be removed in Tomcat 10
     */
    @Deprecated
    public void convert(CharChunk mb, boolean query)
            throws IOException {
        //        log( "Converting a char chunk ");
        int start = mb.getOffset();
        char buff[] = mb.getBuffer();
        int cend = mb.getEnd();

        int idx = CharChunk.indexOf(buff, start, cend, '%');
        int idx2 = -1;
        if (query) {
            idx2 = CharChunk.indexOf(buff, start, (idx >= 0 ? idx : cend), '+');
        }
        if (idx < 0 && idx2 < 0) {
            return;
        }

        // idx will be the smallest positive index ( first % or + )
        if ((idx2 >= 0 && idx2 < idx) || idx < 0) {
            idx = idx2;
        }

        final boolean noSlash = !(ALLOW_ENCODED_SLASH || query);

        for (int j = idx; j < cend; j++, idx++) {
            if (buff[j] == '+' && query) {
                buff[idx] = (' ');
            } else if (buff[j] != '%') {
                buff[idx] = buff[j];
            } else {
                // read next 2 digits
                if (j + 2 >= cend) {
                    // invalid
                    throw EXCEPTION_EOF;
                }
                char b1 = buff[j + 1];
                char b2 = buff[j + 2];
                if (!isHexDigit(b1) || !isHexDigit(b2)) {
                    throw EXCEPTION_NOT_HEX_DIGIT;
                }

                j += 2;
                int res = x2c(b1, b2);
                if (noSlash && (res == '/')) {
                    throw EXCEPTION_SLASH;
                }
                buff[idx] = (char) res;
            }
        }
        mb.setEnd(idx);
    }

    /**
     * URLDecode, will modify the source.
     * <p>
     * <b>WARNING:</b> This method assumes US-ASCII encoding.
     *
     * @param mb    The URL encoded String, bytes or chars
     * @param query <code>true</code> if this is a query string
     * @throws IOException Invalid %xx URL encoding
     * @deprecated Unused. Will be removed in Tomcat 10
     */
    @Deprecated
    public void convert(MessageBytes mb, boolean query)
            throws IOException {

        switch (mb.getType()) {
            case MessageBytes.T_STR:
                String strValue = mb.toString();
                if (strValue == null) {
                    return;
                }
                try {
                    mb.setString(convert(strValue, query));
                } catch (RuntimeException ex) {
                    throw new DecodeException(ex.getMessage());
                }
                break;
            case MessageBytes.T_CHARS:
                CharChunk charC = mb.getCharChunk();
                convert(charC, query);
                break;
            case MessageBytes.T_BYTES:
                ByteChunk bytesC = mb.getByteChunk();
                convert(bytesC, query);
                break;
        }
    }

    /**
     * %xx decoding of a string.
     * <p>
     * <b>WARNING:</b> This method assumes US-ASCII encoding.
     * <p>
     * FIXME: this is inefficient.
     *
     * @param str   The URL encoded string
     * @param query <code>true</code> if this is a query string
     * @return the decoded string
     * @deprecated Unused. Will be removed in Tomcat 10
     */
    @Deprecated
    public final String convert(String str, boolean query) {
        if (str == null) {
            return null;
        }

        if ((!query || str.indexOf('+') < 0) && str.indexOf('%') < 0) {
            return str;
        }

        final boolean noSlash = !(ALLOW_ENCODED_SLASH || query);

        StringBuilder dec = new StringBuilder();    // decoded string output
        int strPos = 0;
        int strLen = str.length();

        dec.ensureCapacity(str.length());
        while (strPos < strLen) {
            int laPos;        // lookahead position

            // look ahead to next URLencoded metacharacter, if any
            for (laPos = strPos; laPos < strLen; laPos++) {
                char laChar = str.charAt(laPos);
                if ((laChar == '+' && query) || (laChar == '%')) {
                    break;
                }
            }

            // if there were non-metacharacters, copy them all as a block
            if (laPos > strPos) {
                dec.append(str.substring(strPos, laPos));
                strPos = laPos;
            }

            // shortcut out of here if we're at the end of the string
            if (strPos >= strLen) {
                break;
            }

            // process next metacharacter
            char metaChar = str.charAt(strPos);
            if (metaChar == '+') {
                dec.append(' ');
                strPos++;
                continue;
            } else if (metaChar == '%') {
                // We throw the original exception - the super will deal with
                // it
                //                try {
                char res = (char) Integer.parseInt(
                        str.substring(strPos + 1, strPos + 3), 16);
                if (noSlash && (res == '/')) {
                    throw new IllegalArgumentException(sm.getString("uDecoder.noSlash"));
                }
                dec.append(res);
                strPos += 3;
            }
        }

        return dec.toString();
    }


    /**
     * Decode and return the specified URL-encoded String.
     * When the byte array is converted to a string, ISO-885901 is used. This
     * may be different than some other servers. It is assumed the string is not
     * a query string.
     *
     * @param str The url-encoded string
     * @return the decoded string
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     * @deprecated Unused. This will be removed in Tomcat 10 onwards
     */
    @Deprecated
    public static String URLDecode(String str) {
        return URLDecode(str, StandardCharsets.ISO_8859_1);
    }


    /**
     * Decode and return the specified URL-encoded String. It is assumed the
     * string is not a query string.
     *
     * @param str The url-encoded string
     * @param enc The encoding to use; if null, ISO-885901 is used. If
     *            an unsupported encoding is specified null will be returned
     * @return the decoded string
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     * @deprecated This method will be removed in Tomcat 9
     */
    @Deprecated
    public static String URLDecode(String str, String enc) {
        return URLDecode(str, enc, false);
    }


    /**
     * Decode and return the specified URL-encoded String. It is assumed the
     * string is not a query string.
     *
     * @param str     The url-encoded string
     * @param charset The character encoding to use; if null, ISO-8859-1 is
     *                used.
     * @return the decoded string
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     */
    public static String URLDecode(String str, Charset charset) {
        return URLDecode(str, charset, false);
    }


    /**
     * Decode and return the specified URL-encoded String.
     *
     * @param str     The url-encoded string
     * @param enc     The encoding to use; if null, ISO-8859-1 is used. If
     *                an unsupported encoding is specified null will be returned
     * @param isQuery Is this a query string being processed
     * @return the decoded string
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     * @deprecated This method will be removed in Tomcat 9
     */
    @Deprecated
    public static String URLDecode(String str, String enc, boolean isQuery) {
        Charset charset = null;

        if (enc != null) {
            try {
                charset = B2CConverter.getCharset(enc);
            } catch (UnsupportedEncodingException uee) {
                if (log.isDebugEnabled()) {
                    log.debug(sm.getString("uDecoder.urlDecode.uee", enc), uee);
                }
            }
        }

        return URLDecode(str, charset, isQuery);
    }


    /**
     * Decode and return the specified URL-encoded byte array.
     *
     * @param bytes   The url-encoded byte array
     * @param enc     The encoding to use; if null, ISO-8859-1 is used. If
     *                an unsupported encoding is specified null will be returned
     * @param isQuery Is this a query string being processed
     * @return the decoded string
     * @throws IllegalArgumentException if a '%' character is not followed
     *                                  by a valid 2-digit hexadecimal number
     * @deprecated This method will be removed in Tomcat 9
     */
    @Deprecated
    public static String URLDecode(byte[] bytes, String enc, boolean isQuery) {
        throw new IllegalArgumentException(sm.getString("udecoder.urlDecode.iae"));
    }


    private static String URLDecode(String str, Charset charset, boolean isQuery) {

        if (str == null) {
            return null;
        }

        if (str.indexOf('%') == -1) {
            // No %nn sequences, so return string unchanged
            return str;
        }

        if (charset == null) {
            charset = StandardCharsets.ISO_8859_1;
        }

        /*
         * Decoding is required.
         *
         * Potential complications:
         * - The source String may be partially decoded so it is not valid to
         *   assume that the source String is ASCII.
         * - Have to process as characters since there is no guarantee that the
         *   byte sequence for '%' is going to be the same in all character
         *   sets.
         * - We don't know how many '%nn' sequences are required for a single
         *   character. It varies between character sets and some use a variable
         *   length.
         */

        // This isn't perfect but it is a reasonable guess for the size of the
        // array required
        ByteArrayOutputStream baos = new ByteArrayOutputStream(str.length() * 2);

        OutputStreamWriter osw = new OutputStreamWriter(baos, charset);

        char[] sourceChars = str.toCharArray();
        int len = sourceChars.length;
        int ix = 0;

        try {
            while (ix < len) {
                char c = sourceChars[ix++];
                if (c == '%') {
                    osw.flush();
                    if (ix + 2 > len) {
                        throw new IllegalArgumentException(
                                sm.getString("uDecoder.urlDecode.missingDigit", str));
                    }
                    char c1 = sourceChars[ix++];
                    char c2 = sourceChars[ix++];
                    if (isHexDigit(c1) && isHexDigit(c2)) {
                        baos.write(x2c(c1, c2));
                    } else {
                        throw new IllegalArgumentException(
                                sm.getString("uDecoder.urlDecode.missingDigit", str));
                    }
                } else if (c == '+' && isQuery) {
                    osw.append(' ');
                } else {
                    osw.append(c);
                }
            }
            osw.flush();

            return baos.toString(charset.name());
        } catch (IOException ioe) {
            throw new IllegalArgumentException(
                    sm.getString("uDecoder.urlDecode.conversionError", str, charset.name()), ioe);
        }
    }


    private static boolean isHexDigit(int c) {
        return ((c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f') ||
                (c >= 'A' && c <= 'F'));
    }


    private static int x2c(byte b1, byte b2) {
        int digit = (b1 >= 'A') ? ((b1 & 0xDF) - 'A') + 10 :
                (b1 - '0');
        digit *= 16;
        digit += (b2 >= 'A') ? ((b2 & 0xDF) - 'A') + 10 :
                (b2 - '0');
        return digit;
    }


    private static int x2c(char b1, char b2) {
        int digit = (b1 >= 'A') ? ((b1 & 0xDF) - 'A') + 10 :
                (b1 - '0');
        digit *= 16;
        digit += (b2 >= 'A') ? ((b2 & 0xDF) - 'A') + 10 :
                (b2 - '0');
        return digit;
    }
}
