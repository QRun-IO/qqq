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

package com.kingsrook.sampleapp.metadata;


import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Storage table for files uploaded to processes (e.g. the bulk insert file
 ** step), named as the uploaded-file archive table in the sample's javalin
 ** metadata (QRun-IO/qqq#784).  The memory backend keeps each file under its
 ** storage reference (the STRING primary key) with its bytes in the BLOB field,
 ** so uploads last as long as the running sample, as the sample's data does.
 *******************************************************************************/
public class UploadedFileArchiveTableMetaDataProducer extends MetaDataProducer<QTableMetaData>
{
   public static final String NAME = "uploadedFileArchive";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QTableMetaData produce(QInstance instance)
   {
      return (new QTableMetaData()
         .withName(NAME)
         .withLabel("Uploaded File Archive")
         .withBackendName(SampleMetaDataProvider.MEMORY_BACKEND_NAME)
         .withIsHidden(true)
         .withPrimaryKeyField("fileName")
         .withField(new QFieldMetaData("fileName", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB)));
   }
}
