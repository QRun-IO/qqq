/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.filesystem.base.model.metadata;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.SectionFactory;
import com.kingsrook.qqq.backend.module.filesystem.local.FilesystemBackendModule;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.s3.S3BackendModule;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.sftp.SFTPBackendModule;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPTableBackendDetails;


/*******************************************************************************
 ** Builder class to create standard style QTableMetaData for tables in filesystem
 ** modules (avoid some boilerplate).
 **
 ** e.g., lets us create a file-based table like so:
 <pre>
 QTableMetaData table = new FilesystemTableMetaDataBuilder()
 .withName("myTableName")
 .withBackend(qInstance.getBackend("myBackendName"))
 .withGlob("*.csv")
 .withBasePath("/")
 .buildStandardCardinalityOneTable();
 </pre>
 *******************************************************************************/
public class FilesystemTableMetaDataBuilder
{
   private String           name;
   private QBackendMetaData backend;
   private String           basePath;
   private String           glob;

   private String contentsAdornmentFileNameField = "baseName";



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData buildStandardCardinalityOneTable()
   {
      boolean includeCreateDate = true;
      AbstractFilesystemTableBackendDetails tableBackendDetails = switch(backend.getBackendType())
      {
         case S3BackendModule.BACKEND_TYPE ->
         {
            includeCreateDate = false;
            yield new S3TableBackendDetails();
         }
         case FilesystemBackendModule.BACKEND_TYPE -> new FilesystemTableBackendDetails();
         case SFTPBackendModule.BACKEND_TYPE -> new SFTPTableBackendDetails();
         default -> throw new IllegalStateException("Unexpected value: " + backend.getBackendType());
      };

      List<QFieldMetaData> fields = new ArrayList<>();

      fields.add((new QFieldMetaData("fileName", QFieldType.STRING)));
      fields.add((new QFieldMetaData("baseName", QFieldType.STRING)));
      fields.add((new QFieldMetaData("size", QFieldType.LONG).withDisplayFormat(DisplayFormat.COMMAS)));
      fields.add((new QFieldMetaData("modifyDate", QFieldType.DATE_TIME)));
      fields.add((new QFieldMetaData("contents", QFieldType.BLOB)
         .withIsHeavy(true)
         .withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD)
            .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT, "%s")
            .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FIELD, contentsAdornmentFileNameField
         ))));

      QFieldSection t3Section = SectionFactory.defaultT3("modifyDate");

      AbstractFilesystemTableBackendDetails backendDetails = tableBackendDetails
         .withCardinality(Cardinality.ONE)
         .withFileNameFieldName("fileName")
         .withBaseNameFieldName("baseName")
         .withContentsFieldName("contents")
         .withSizeFieldName("size")
         .withModifyDateFieldName("modifyDate")
         .withBasePath(basePath)
         .withGlob(glob);

      if(includeCreateDate)
      {
         fields.add((new QFieldMetaData("createDate", QFieldType.DATE_TIME)));
         backendDetails.setCreateDateFieldName("createDate");

         ArrayList<String> t3FieldNames = new ArrayList<>(t3Section.getFieldNames());
         t3FieldNames.add(0, "createDate");
         t3Section.setFieldNames(t3FieldNames);
      }

      return new QTableMetaData()
         .withName(name)
         .withIsHidden(true)
         .withBackendName(backend.getName())
         .withPrimaryKeyField("fileName")
         .withFields(fields)
         .withSection(SectionFactory.defaultT1("fileName"))
         .withSection(SectionFactory.defaultT2("baseName", "contents", "size"))
         .withSection(t3Section)
         .withBackendDetails(backendDetails);
   }



   /*******************************************************************************
    ** Getter for backend
    *******************************************************************************/
   public QBackendMetaData getBackend()
   {
      return (this.backend);
   }



   /*******************************************************************************
    ** Setter for backend
    *******************************************************************************/
   public void setBackend(QBackendMetaData backend)
   {
      this.backend = backend;
   }



   /*******************************************************************************
    ** Fluent setter for backend
    *******************************************************************************/
   public FilesystemTableMetaDataBuilder withBackend(QBackendMetaData backend)
   {
      this.backend = backend;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public FilesystemTableMetaDataBuilder withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for basePath
    *******************************************************************************/
   public String getBasePath()
   {
      return (this.basePath);
   }



   /*******************************************************************************
    ** Setter for basePath
    *******************************************************************************/
   public void setBasePath(String basePath)
   {
      this.basePath = basePath;
   }



   /*******************************************************************************
    ** Fluent setter for basePath
    *******************************************************************************/
   public FilesystemTableMetaDataBuilder withBasePath(String basePath)
   {
      this.basePath = basePath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for glob
    *******************************************************************************/
   public String getGlob()
   {
      return (this.glob);
   }



   /*******************************************************************************
    ** Setter for glob
    *******************************************************************************/
   public void setGlob(String glob)
   {
      this.glob = glob;
   }



   /*******************************************************************************
    ** Fluent setter for glob
    *******************************************************************************/
   public FilesystemTableMetaDataBuilder withGlob(String glob)
   {
      this.glob = glob;
      return (this);
   }


   /*******************************************************************************
    ** Getter for contentsAdornmentFileNameField
    *******************************************************************************/
   public String getContentsAdornmentFileNameField()
   {
      return (this.contentsAdornmentFileNameField);
   }



   /*******************************************************************************
    ** Setter for contentsAdornmentFileNameField
    *******************************************************************************/
   public void setContentsAdornmentFileNameField(String contentsAdornmentFileNameField)
   {
      this.contentsAdornmentFileNameField = contentsAdornmentFileNameField;
   }



   /*******************************************************************************
    ** Fluent setter for contentsAdornmentFileNameField
    *******************************************************************************/
   public FilesystemTableMetaDataBuilder withContentsAdornmentFileNameField(String contentsAdornmentFileNameField)
   {
      this.contentsAdornmentFileNameField = contentsAdornmentFileNameField;
      return (this);
   }


}
