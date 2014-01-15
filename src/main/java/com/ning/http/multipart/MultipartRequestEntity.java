/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.multipart;

import static com.ning.http.util.MiscUtil.isNonEmpty;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * This class is an adaptation of the Apache HttpClient implementation
 * 
 * @link http://hc.apache.org/httpclient-3.x/
 */
public class MultipartRequestEntity implements RequestEntity {

    /**
     * The Content-Type for multipart/form-data.
     */
    private static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";

    /**
     * The pool of ASCII chars to be used for generating a multipart boundary.
     */
    private static byte[] MULTIPART_CHARS = MultipartEncodingUtil.getAsciiBytes("-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");

    /**
     * Generates a random multipart boundary string.
     * 
     * @return
     */
    public static byte[] generateMultipartBoundary() {
        Random rand = new Random();
        byte[] bytes = new byte[rand.nextInt(11) + 30]; // a random size from 30 to 40
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)];
        }
        return bytes;
    }

    private final Logger log = LoggerFactory.getLogger(MultipartRequestEntity.class);

    /**
     * The MIME parts as set by the constructor
     */
    protected Part[] parts;

    private final byte[] multipartBoundary;

    private final String contentType;

    /**
     * Creates a new multipart entity containing the given parts.
     * 
     * @param parts The parts to include.
     */
    public MultipartRequestEntity(Part[] parts, FluentCaseInsensitiveStringsMap requestHeaders) {
        if (parts == null) {
            throw new IllegalArgumentException("parts cannot be null");
        }
        this.parts = parts;
        String contentTypeHeader = requestHeaders.getFirstValue("Content-Type");
        if (isNonEmpty(contentTypeHeader)) {
        	int boundaryLocation = contentTypeHeader.indexOf("boundary=");
        	if (boundaryLocation != -1) {
        		// boundary defined in existing Content-Type
        		contentType = contentTypeHeader;
        		multipartBoundary = MultipartEncodingUtil.getAsciiBytes((contentTypeHeader.substring(boundaryLocation + "boundary=".length()).trim()));
        	} else {
        		// generate boundary and append it to existing Content-Type
        		multipartBoundary = generateMultipartBoundary();
                contentType = computeContentType(contentTypeHeader);
        	}
        } else {
        	multipartBoundary = generateMultipartBoundary();
            contentType = computeContentType(MULTIPART_FORM_CONTENT_TYPE);
        }
    }

    private String computeContentType(String base) {
    	StringBuilder buffer = new StringBuilder(base);
		if (!base.endsWith(";"))
			buffer.append(";");
        return buffer.append(" boundary=").append(MultipartEncodingUtil.getAsciiString(multipartBoundary)).toString();
    }

    /**
     * Returns the MIME boundary string that is used to demarcate boundaries of this part. The first call to this method will implicitly create a new boundary string. To create a boundary string first the HttpMethodParams.MULTIPART_BOUNDARY parameter is considered. Otherwise a
     * random one is generated.
     * 
     * @return The boundary string of this entity in ASCII encoding.
     */
    protected byte[] getMultipartBoundary() {
        return multipartBoundary;
    }

    /**
     * Returns <code>true</code> if all parts are repeatable, <code>false</code> otherwise.
     */
    public boolean isRepeatable() {
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isRepeatable()) {
                return false;
            }
        }
        return true;
    }

    public void writeRequest(OutputStream out) throws IOException {
        Part.sendParts(out, parts, multipartBoundary);
    }

    public long getContentLength() {
        try {
            return Part.getLengthOfParts(parts, multipartBoundary);
        } catch (Exception e) {
            log.error("An exception occurred while getting the length of the parts", e);
            return 0L;
        }
    }

    public String getContentType() {
        return contentType;
    }
}
