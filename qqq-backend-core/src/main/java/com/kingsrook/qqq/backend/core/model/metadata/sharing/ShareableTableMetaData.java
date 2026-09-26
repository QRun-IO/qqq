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

package com.kingsrook.qqq.backend.core.model.metadata.sharing;


import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** meta data to attach to a table, to describe that its records are shareable.
 *******************************************************************************/
public class ShareableTableMetaData implements Serializable, Cloneable
{
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // this is the name of the table that is a many-to-one join to the table whose records are being shared.  //
   // not the table whose records are shared (the asset table)                                               //
   // for example:  given that we want to share "savedReports", the value here could be "sharedSavedReports" //
   // and this object will be attached to the savedReports table.                                            //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private String sharedRecordTableName;

   ///////////////////////////////////////////////////////////////////////////////////////////////////////
   // name of the field in the sharedRecordTable that has a foreign key pointing at the asset table //
   ///////////////////////////////////////////////////////////////////////////////////////////////////////
   private String assetIdFieldName;

   //////////////////////////////////////////////////////
   // name of the scope field in the sharedRecordTable //
   //////////////////////////////////////////////////////
   private String scopeFieldName;

   ///////////////////////////////////////////////////////////
   // map of audienceTypes names to type definition objects //
   ///////////////////////////////////////////////////////////
   private Map<String, ShareableAudienceType> audienceTypes;

   /////////////////////////////////////////////////
   // PVS that lists the available audience types //
   /////////////////////////////////////////////////
   private String audienceTypesPossibleValueSourceName;

   ///////////////////////////////////////////////////
   // PVS that lists the available audience records //
   ///////////////////////////////////////////////////
   private String audiencePossibleValueSourceName;

   //////////////////////////////////////////////////////////////
   // name of a field in "this" table, that has the owner's id //
   //////////////////////////////////////////////////////////////
   private String thisTableOwnerIdFieldName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ShareableTableMetaData()
   {
   }



   /*******************************************************************************
    ** Getter for sharedRecordTableName
    *******************************************************************************/
   public String getSharedRecordTableName()
   {
      return (this.sharedRecordTableName);
   }



   /*******************************************************************************
    ** Setter for sharedRecordTableName
    *******************************************************************************/
   public void setSharedRecordTableName(String sharedRecordTableName)
   {
      this.sharedRecordTableName = sharedRecordTableName;
   }



   /*******************************************************************************
    ** Fluent setter for sharedRecordTableName
    *******************************************************************************/
   public ShareableTableMetaData withSharedRecordTableName(String sharedRecordTableName)
   {
      this.sharedRecordTableName = sharedRecordTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for assetIdFieldName
    *******************************************************************************/
   public String getAssetIdFieldName()
   {
      return (this.assetIdFieldName);
   }



   /*******************************************************************************
    ** Setter for assetIdFieldName
    *******************************************************************************/
   public void setAssetIdFieldName(String assetIdFieldName)
   {
      this.assetIdFieldName = assetIdFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for assetIdFieldName
    *******************************************************************************/
   public ShareableTableMetaData withAssetIdFieldName(String assetIdFieldName)
   {
      this.assetIdFieldName = assetIdFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scopeFieldName
    *******************************************************************************/
   public String getScopeFieldName()
   {
      return (this.scopeFieldName);
   }



   /*******************************************************************************
    ** Setter for scopeFieldName
    *******************************************************************************/
   public void setScopeFieldName(String scopeFieldName)
   {
      this.scopeFieldName = scopeFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for scopeFieldName
    *******************************************************************************/
   public ShareableTableMetaData withScopeFieldName(String scopeFieldName)
   {
      this.scopeFieldName = scopeFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for audienceTypes
    *******************************************************************************/
   public Map<String, ShareableAudienceType> getAudienceTypes()
   {
      return (this.audienceTypes);
   }



   /*******************************************************************************
    ** Setter for audienceTypes
    *******************************************************************************/
   public void setAudienceTypes(Map<String, ShareableAudienceType> audienceTypes)
   {
      this.audienceTypes = audienceTypes;
   }



   /*******************************************************************************
    ** Fluent setter for audienceTypes
    *******************************************************************************/
   public ShareableTableMetaData withAudienceTypes(Map<String, ShareableAudienceType> audienceTypes)
   {
      this.audienceTypes = audienceTypes;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for audienceTypes
    *******************************************************************************/
   public ShareableTableMetaData withAudienceType(ShareableAudienceType audienceType)
   {
      if(this.audienceTypes == null)
      {
         this.audienceTypes = new LinkedHashMap<>();
      }

      if(audienceType.getName() == null)
      {
         throw (new IllegalArgumentException("Attempt to add an audience type without a name"));
      }

      if(this.audienceTypes.containsKey(audienceType.getName()))
      {
         throw (new IllegalArgumentException("Attempt to add more than 1 audience type with the same name [" + audienceType.getName() + "]"));
      }

      this.audienceTypes.put(audienceType.getName(), audienceType);

      return (this);
   }



   /*******************************************************************************
    ** Getter for audienceTypesPossibleValueSourceName
    *******************************************************************************/
   public String getAudienceTypesPossibleValueSourceName()
   {
      return (this.audienceTypesPossibleValueSourceName);
   }



   /*******************************************************************************
    ** Setter for audienceTypesPossibleValueSourceName
    *******************************************************************************/
   public void setAudienceTypesPossibleValueSourceName(String audienceTypesPossibleValueSourceName)
   {
      this.audienceTypesPossibleValueSourceName = audienceTypesPossibleValueSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for audienceTypesPossibleValueSourceName
    *******************************************************************************/
   public ShareableTableMetaData withAudienceTypesPossibleValueSourceName(String audienceTypesPossibleValueSourceName)
   {
      this.audienceTypesPossibleValueSourceName = audienceTypesPossibleValueSourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for thisTableOwnerIdFieldName
    *******************************************************************************/
   public String getThisTableOwnerIdFieldName()
   {
      return (this.thisTableOwnerIdFieldName);
   }



   /*******************************************************************************
    ** Setter for thisTableOwnerIdFieldName
    *******************************************************************************/
   public void setThisTableOwnerIdFieldName(String thisTableOwnerIdFieldName)
   {
      this.thisTableOwnerIdFieldName = thisTableOwnerIdFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for thisTableOwnerIdFieldName
    *******************************************************************************/
   public ShareableTableMetaData withThisTableOwnerIdFieldName(String thisTableOwnerIdFieldName)
   {
      this.thisTableOwnerIdFieldName = thisTableOwnerIdFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for audiencePossibleValueSourceName
    *******************************************************************************/
   public String getAudiencePossibleValueSourceName()
   {
      return (this.audiencePossibleValueSourceName);
   }



   /*******************************************************************************
    ** Setter for audiencePossibleValueSourceName
    *******************************************************************************/
   public void setAudiencePossibleValueSourceName(String audiencePossibleValueSourceName)
   {
      this.audiencePossibleValueSourceName = audiencePossibleValueSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for audiencePossibleValueSourceName
    *******************************************************************************/
   public ShareableTableMetaData withAudiencePossibleValueSourceName(String audiencePossibleValueSourceName)
   {
      this.audiencePossibleValueSourceName = audiencePossibleValueSourceName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance qInstance, QTableMetaData tableMetaData, QInstanceValidator qInstanceValidator)
   {
      String prefix = "ShareableTableMetaData for table [" + tableMetaData.getName() + "]: ";
      if(qInstanceValidator.assertCondition(StringUtils.hasContent(sharedRecordTableName), prefix + "missing sharedRecordTableName."))
      {
         boolean hasAssetIdFieldName = qInstanceValidator.assertCondition(StringUtils.hasContent(assetIdFieldName), prefix + "missing assetIdFieldName");
         boolean hasScopeFieldName   = qInstanceValidator.assertCondition(StringUtils.hasContent(scopeFieldName), prefix + "missing scopeFieldName");

         QTableMetaData sharedRecordTable         = qInstance.getTable(sharedRecordTableName);
         boolean        hasValidSharedRecordTable = qInstanceValidator.assertCondition(sharedRecordTable != null, prefix + "unrecognized sharedRecordTableName [" + sharedRecordTableName + "]");

         if(hasValidSharedRecordTable && hasAssetIdFieldName)
         {
            qInstanceValidator.assertCondition(sharedRecordTable.getFields().containsKey(assetIdFieldName), prefix + "unrecognized assertIdFieldName [" + assetIdFieldName + "] in sharedRecordTable [" + sharedRecordTableName + "]");
         }

         if(hasValidSharedRecordTable && hasScopeFieldName)
         {
            qInstanceValidator.assertCondition(sharedRecordTable.getFields().containsKey(scopeFieldName), prefix + "unrecognized scopeFieldName [" + scopeFieldName + "] in sharedRecordTable [" + sharedRecordTableName + "]");
         }

         if(qInstanceValidator.assertCondition(CollectionUtils.nullSafeHasContents(audienceTypes), prefix + "missing audienceTypes"))
         {
            for(Map.Entry<String, ShareableAudienceType> entry : audienceTypes.entrySet())
            {
               ShareableAudienceType audienceType = entry.getValue();
               qInstanceValidator.assertCondition(Objects.equals(entry.getKey(), audienceType.getName()), prefix + "inconsistent naming for shareableAudienceType [" + entry.getKey() + "] != [" + audienceType.getName() + "]");
               if(qInstanceValidator.assertCondition(StringUtils.hasContent(audienceType.getFieldName()), prefix + "missing fieldName for shareableAudienceType [" + entry.getKey() + "]") && hasValidSharedRecordTable)
               {
                  qInstanceValidator.assertCondition(sharedRecordTable.getFields().containsKey(audienceType.getFieldName()), prefix + "unrecognized fieldName [" + audienceType.getFieldName() + "] for shareableAudienceType [" + entry.getKey() + "] in sharedRecordTable [" + sharedRecordTableName + "]");
               }

               // todo - validate this audienceType.getSourceTableKeyFieldName() is a field, and it is a UKey

               /* todo - make these optional i guess, because i didn't put user table in qqq
               boolean hasSourceTableKeyFieldName = qInstanceValidator.assertCondition(StringUtils.hasContent(audienceType.getSourceTableKeyFieldName()), prefix + "missing sourceTableKeyFieldName for shareableAudienceType [" + entry.getKey() + "]");
               if(qInstanceValidator.assertCondition(qInstance.getTable(audienceType.getSourceTableName()) != null, prefix + "unrecognized sourceTableName [" + audienceType.getSourceTableName() + "] for shareableAudienceType [" + entry.getKey() + "] in sharedRecordTable [" + sharedRecordTableName + "]") && hasSourceTableKeyFieldName)
               {
                  qInstanceValidator.assertCondition(qInstance.getTable(audienceType.getSourceTableName()).getFields().containsKey(audienceType.getSourceTableKeyFieldName()), prefix + "unrecognized sourceTableKeyFieldName [" + audienceType.getSourceTableKeyFieldName() + "] for shareableAudienceType [" + entry.getKey() + "] in sharedRecordTable [" + sharedRecordTableName + "]");
               }
               */
            }
         }
      }

      if(StringUtils.hasContent(thisTableOwnerIdFieldName))
      {
         qInstanceValidator.assertCondition(tableMetaData.getFields().containsKey(thisTableOwnerIdFieldName), prefix + "unrecognized thisTableOwnerIdFieldName [" + thisTableOwnerIdFieldName + "]");
      }

      if(StringUtils.hasContent(audienceTypesPossibleValueSourceName))
      {
         qInstanceValidator.assertCondition(qInstance.getPossibleValueSource(audienceTypesPossibleValueSourceName) != null, prefix + "unrecognized audienceTypesPossibleValueSourceName [" + audienceTypesPossibleValueSourceName + "]");
      }

      if(StringUtils.hasContent(audiencePossibleValueSourceName))
      {
         qInstanceValidator.assertCondition(qInstance.getPossibleValueSource(audiencePossibleValueSourceName) != null, prefix + "unrecognized audiencePossibleValueSourceName [" + audiencePossibleValueSourceName + "]");
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public ShareableTableMetaData clone()
   {
      try
      {
         ShareableTableMetaData clone = (ShareableTableMetaData) super.clone();

         if(audienceTypes != null)
         {
            clone.audienceTypes = new HashMap<>(audienceTypes);
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
