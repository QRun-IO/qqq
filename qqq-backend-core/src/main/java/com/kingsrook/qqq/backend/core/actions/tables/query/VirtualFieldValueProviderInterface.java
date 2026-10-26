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

package com.kingsrook.qqq.backend.core.actions.tables.query;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Supplies selected virtual-field values without replacing a host table customizer.
 ** Providers batch their lookups and preserve record order, identity and other fields.
 *******************************************************************************/
@FunctionalInterface
public interface VirtualFieldValueProviderInterface
{
   /*******************************************************************************
    ** Fill this field for one delivered batch after the host post-query customizer.
    ** For joins, table and records describe the joined table and fieldName is
    ** unprefixed; input remains the originating request (including its transaction).
    ** The caller retains responsibility for final field-access filtering.
    *******************************************************************************/
   void fill(QTableMetaData table, String fieldName, List<QRecord> records, QueryInput input) throws QException;
}
