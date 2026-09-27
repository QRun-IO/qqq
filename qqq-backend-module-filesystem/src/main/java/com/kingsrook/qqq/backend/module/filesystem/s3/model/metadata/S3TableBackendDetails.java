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

package com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.s3.S3BackendModule;


/*******************************************************************************
 ** S3 specific Extension of QTableBackendDetails
 *******************************************************************************/
public class S3TableBackendDetails extends AbstractFilesystemTableBackendDetails
{
   private ContentTypeStrategy contentTypeStrategy = ContentTypeStrategy.NONE;
   private String              contentTypeFieldName;
   private String              hardcodedContentType;



   /***************************************************************************
    **
    ***************************************************************************/
   public enum ContentTypeStrategy
   {
      BASED_ON_FILE_NAME,
      FROM_FIELD,
      HARDCODED,
      NONE
   }



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public S3TableBackendDetails()
   {
      super();
      setBackendType(S3BackendModule.class);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void validate(QInstance qInstance, QTableMetaData table, QInstanceValidator qInstanceValidator)
   {
      super.validate(qInstance, table, qInstanceValidator);

      String prefix = "Table " + (table == null ? "null" : table.getName()) + " backend details - ";
      switch(Objects.requireNonNullElse(contentTypeStrategy, ContentTypeStrategy.NONE))
      {
         case FROM_FIELD ->
         {
            qInstanceValidator.assertCondition(!StringUtils.hasContent(hardcodedContentType), prefix + "hardcodedContentType should not be set when contentTypeStrategy is " + contentTypeStrategy);

            if(table != null && qInstanceValidator.assertCondition(StringUtils.hasContent(contentTypeFieldName), prefix + "contentTypeFieldName must be set when contentTypeStrategy is " + contentTypeStrategy))
            {
               qInstanceValidator.assertCondition(table.getFields().containsKey(contentTypeFieldName), prefix + "contentTypeFieldName must be a valid field name in the table");
            }
         }
         case HARDCODED ->
         {
            qInstanceValidator.assertCondition(!StringUtils.hasContent(contentTypeFieldName), prefix + "contentTypeFieldName should not be set when contentTypeStrategy is " + contentTypeStrategy);
            qInstanceValidator.assertCondition(StringUtils.hasContent(hardcodedContentType), prefix + "hardcodedContentType must be set when contentTypeStrategy is " + contentTypeStrategy);
         }
         case BASED_ON_FILE_NAME, NONE ->
         {
            qInstanceValidator.assertCondition(!StringUtils.hasContent(contentTypeFieldName), prefix + "contentTypeFieldName should not be set when contentTypeStrategy is " + contentTypeStrategy);
            qInstanceValidator.assertCondition(!StringUtils.hasContent(hardcodedContentType), prefix + "hardcodedContentType should not be set when contentTypeStrategy is " + contentTypeStrategy);
         }
         default ->
         {
            throw new IllegalStateException("Unexpected value: " + contentTypeStrategy);
         }
      }
   }



   /*******************************************************************************
    ** Getter for contentTypeStrategy
    *******************************************************************************/
   public ContentTypeStrategy getContentTypeStrategy()
   {
      return (this.contentTypeStrategy);
   }



   /*******************************************************************************
    ** Setter for contentTypeStrategy
    *******************************************************************************/
   public void setContentTypeStrategy(ContentTypeStrategy contentTypeStrategy)
   {
      this.contentTypeStrategy = contentTypeStrategy;
   }



   /*******************************************************************************
    ** Fluent setter for contentTypeStrategy
    *******************************************************************************/
   public S3TableBackendDetails withContentTypeStrategy(ContentTypeStrategy contentTypeStrategy)
   {
      this.contentTypeStrategy = contentTypeStrategy;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentTypeFieldName
    *******************************************************************************/
   public String getContentTypeFieldName()
   {
      return (this.contentTypeFieldName);
   }



   /*******************************************************************************
    ** Setter for contentTypeFieldName
    *******************************************************************************/
   public void setContentTypeFieldName(String contentTypeFieldName)
   {
      this.contentTypeFieldName = contentTypeFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for contentTypeFieldName
    *******************************************************************************/
   public S3TableBackendDetails withContentTypeFieldName(String contentTypeFieldName)
   {
      this.contentTypeFieldName = contentTypeFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hardcodedContentType
    *******************************************************************************/
   public String getHardcodedContentType()
   {
      return (this.hardcodedContentType);
   }



   /*******************************************************************************
    ** Setter for hardcodedContentType
    *******************************************************************************/
   public void setHardcodedContentType(String hardcodedContentType)
   {
      this.hardcodedContentType = hardcodedContentType;
   }



   /*******************************************************************************
    ** Fluent setter for hardcodedContentType
    *******************************************************************************/
   public S3TableBackendDetails withHardcodedContentType(String hardcodedContentType)
   {
      this.hardcodedContentType = hardcodedContentType;
      return (this);
   }



   /***************************************************************************
    * finish the cloning process in a subclass of AbstractFilesystemTableBackendDetails
    ***************************************************************************/
   @Override
   protected S3TableBackendDetails finishFilesystemSubclassClone(QTableBackendDetails abstractClone)
   {
      S3TableBackendDetails clone = (S3TableBackendDetails) abstractClone;
      clone.contentTypeStrategy = contentTypeStrategy;
      clone.contentTypeFieldName = contentTypeFieldName;
      clone.hardcodedContentType = hardcodedContentType;
      return (clone);
   }

}
