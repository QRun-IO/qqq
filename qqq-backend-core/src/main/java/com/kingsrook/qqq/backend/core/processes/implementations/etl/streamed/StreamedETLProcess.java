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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed;


import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;


/*******************************************************************************
 ** Definition for Streamed ETL process.
 *******************************************************************************/
public class StreamedETLProcess
{
   public static final String PROCESS_NAME = "etl.streamed";

   public static final String FUNCTION_NAME_ETL = "streamedETL";

   public static final String FIELD_SOURCE_TABLE      = "sourceTable";
   public static final String FIELD_DESTINATION_TABLE = "destinationTable";
   public static final String FIELD_MAPPING_JSON      = "mappingJSON";
   public static final String FIELD_RECORD_COUNT      = "recordCount";



   /*******************************************************************************
    **
    *******************************************************************************/
   public QProcessMetaData defineProcessMetaData()
   {
      QStepMetaData etlFunction = new QBackendStepMetaData()
         .withName(FUNCTION_NAME_ETL)
         .withCode(new QCodeReference()
            .withName(StreamedETLBackendStep.class.getName())
            .withCodeType(QCodeType.JAVA))
         .withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData(FIELD_SOURCE_TABLE, QFieldType.STRING))
            .withField(new QFieldMetaData(FIELD_MAPPING_JSON, QFieldType.STRING))
            .withField(new QFieldMetaData(FIELD_DESTINATION_TABLE, QFieldType.STRING)))
         .withOutputMetaData(new QFunctionOutputMetaData()
            .withField(new QFieldMetaData(FIELD_RECORD_COUNT, QFieldType.INTEGER)));

      return new QProcessMetaData()
         .withName(PROCESS_NAME)
         .withStep(etlFunction);
   }
}
