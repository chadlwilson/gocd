/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.util;

import java.util.Calendar;
import java.util.Date;

public class GoConstants {
    public static final Date NEVER;
    public static final String GO_ARTIFACT_PAYLOAD_SIZE = "X-GO-ARTIFACT-SIZE";

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1900, Calendar.JANUARY, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        NEVER = calendar.getTime();
    }

    /**
     * This will force the browser to clear the cache only for this page.
     * If any other pages need to clear the cache, we might want to move this
     * logic to an interceptor.
     */
    public static final String CACHE_CONTROL = "max-age=1, no-cache";
    public static final String DEFAULT_APPROVED_BY = "changes";
    public static final String ERROR_FOR_PAGE = "errorMessage";
    public static final String ERROR_FOR_JSON = "error";

    public static final String RESPONSE_CHARSET = "text/plain; charset=utf-8";
    public static final String RESPONSE_CHARSET_JSON = "application/json; charset=utf-8";

    public static final String PRODUCT_NAME = "go";

    public static final int CONFIG_SCHEMA_VERSION = 139;

    public static final String APPROVAL_SUCCESS = "success";
    public static final String APPROVAL_MANUAL = "manual";
    public static final int PUBLISH_MAX_RETRIES = 3;
    public static final long MEGABYTES_IN_GIGABYTE = 1024;
    public static final long MEGA_BYTE = 1024 * 1024;
    public static final long GIGA_BYTE = MEGABYTES_IN_GIGABYTE * MEGA_BYTE;
    public static final String USE_COMPRESSED_JAVASCRIPT = "rails.use.compressed.js";
    public static final String GO_URL_CONTEXT = "/go";
    public static final String REGULAR_MULTIPART_FILENAME = "file";
    public static final String CHECKSUM_MULTIPART_FILENAME = "file_checksum";
    public static final String ZIP_MULTIPART_FILENAME = "zipfile";
    public static final String AGENT_JAR_MD5 = "agent.binary.md5";
    public static final String AGENT_PLUGINS_MD5 = "agent.plugins.md5";
    public static final String AGENT_BOOTSTRAPPER_VERSION = "agent.bootstrapper.version";
    public static final String TFS_IMPL_MD5 = "agent.tfs.md5";
    public static final String GIVEN_AGENT_LAUNCHER_JAR_MD5 = "agent.launcher.md5";
    public static final String ANY_PIPELINE = "[Any Pipeline]";
    public static final String ANY_STAGE = "[Any Stage]";
}
