/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Interface used within QueryOutput, to handle diffrent ways we may store records
 ** (e.g., in a list (that holds them all), or a pipe, that streams them to a consumer thread))
 *******************************************************************************/
interface QueryOutputStorageInterface
{

   /*******************************************************************************
    ** add a records to this output
    *******************************************************************************/
   void addRecord(QRecord record) throws QException;


   /*******************************************************************************
    ** add a list of records to this output
    *******************************************************************************/
   void addRecords(List<QRecord> records) throws QException;

   /*******************************************************************************
    ** Get all stored records
    *******************************************************************************/
   List<QRecord> getRecords();
}
