/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.kingsrook.qqq.backend.core.jobs.uploads;


import java.io.Serializable;
import java.time.Instant;


/*******************************************************************************
 ** An uploaded file, as clients see it (spec section 3.2). The ref is opaque,
 ** bound to the uploading user, and expires.
 **
 ** @param ref         the opaque ref (pass it in uploadRefs when launching a job)
 ** @param fileName    the file's name
 ** @param sizeBytes   its size
 ** @param contentType its content type, as uploaded
 ** @param expiresAt   when the ref stops working (a job that uses it extends it)
 *******************************************************************************/
public record UploadRef(String ref, String fileName, Long sizeBytes, String contentType, Instant expiresAt) implements Serializable
{
}
