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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;


/*******************************************************************************
 ** Where the assisted bulk-load operations read an uploaded file from, by its
 ** opaque upload ref, as the current session's user.
 **
 ** The intended implementation is core's uploads (POST /qqq/v1/uploads,
 ** UploadRefs.open, design spec #1171 section 3.2), registered in one line
 ** through {@link BulkLoadUploadSources#register(BulkLoadUploadSource)} once
 ** that lands. Until then an application (or a test) registers its own.
 *******************************************************************************/
@FunctionalInterface
public interface BulkLoadUploadSource
{
   /*******************************************************************************
    ** Open an upload by its ref. Throw QNotFoundException when the ref is
    ** unknown, expired, or not the session user's (all three the same, so a
    ** ref's existence never leaks). The returned StorageInput's reference ends
    ** with the original file name, whose extension picks CSV or XLSX.
    *******************************************************************************/
   StorageInput open(String uploadRef) throws QException;
}
