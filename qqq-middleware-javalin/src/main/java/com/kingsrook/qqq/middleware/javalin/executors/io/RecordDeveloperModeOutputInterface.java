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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils.AssociatedScriptDetails;


/*******************************************************************************
 ** Output of the record developer-mode route:  the record, and the details of
 ** each of its table's associated scripts.
 *******************************************************************************/
public interface RecordDeveloperModeOutputInterface extends AbstractMiddlewareOutputInterface
{
   /***************************************************************************
    ** Setter for the record (read with display values)
    ***************************************************************************/
   void setRecord(QRecord record);

   /***************************************************************************
    ** Setter for the details of the table's associated scripts
    ***************************************************************************/
   void setAssociatedScripts(List<AssociatedScriptDetails> associatedScripts);
}
