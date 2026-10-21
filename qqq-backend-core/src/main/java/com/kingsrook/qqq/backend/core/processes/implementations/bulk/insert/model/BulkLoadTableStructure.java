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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model;


import java.io.Serializable;
import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkLoadTableStructure implements Serializable
{
   private boolean isMain;
   private boolean isMany;

   private String            tableName;
   private String            label;
   private String            associationPath; // null/empty for main table, then associationName for a child, associationName.associationName for a grandchild
   private Boolean           isBulkEdit;
   private String            keyFields;
   private ArrayList<String> possibleKeyFields;

   private ArrayList<QFieldMetaData>         fields; // mmm, not marked as serializable (at this time) - is okay?
   private ArrayList<BulkLoadTableStructure> associations;



   /*******************************************************************************
    ** Getter for isMain
    *******************************************************************************/
   public boolean getIsMain()
   {
      return (this.isMain);
   }



   /*******************************************************************************
    ** Setter for isMain
    *******************************************************************************/
   public void setIsMain(boolean isMain)
   {
      this.isMain = isMain;
   }



   /*******************************************************************************
    ** Fluent setter for isMain
    *******************************************************************************/
   public BulkLoadTableStructure withIsMain(boolean isMain)
   {
      this.isMain = isMain;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isMany
    *******************************************************************************/
   public boolean getIsMany()
   {
      return (this.isMany);
   }



   /*******************************************************************************
    ** Setter for isMany
    *******************************************************************************/
   public void setIsMany(boolean isMany)
   {
      this.isMany = isMany;
   }



   /*******************************************************************************
    ** Fluent setter for isMany
    *******************************************************************************/
   public BulkLoadTableStructure withIsMany(boolean isMany)
   {
      this.isMany = isMany;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public BulkLoadTableStructure withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public BulkLoadTableStructure withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fields
    *******************************************************************************/
   public ArrayList<QFieldMetaData> getFields()
   {
      return (this.fields);
   }



   /*******************************************************************************
    ** Setter for fields
    *******************************************************************************/
   public void setFields(ArrayList<QFieldMetaData> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    ** Fluent setter for fields
    *******************************************************************************/
   public BulkLoadTableStructure withFields(ArrayList<QFieldMetaData> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for associationPath
    *******************************************************************************/
   public String getAssociationPath()
   {
      return (this.associationPath);
   }



   /*******************************************************************************
    ** Setter for associationPath
    *******************************************************************************/
   public void setAssociationPath(String associationPath)
   {
      this.associationPath = associationPath;
   }



   /*******************************************************************************
    ** Fluent setter for associationPath
    *******************************************************************************/
   public BulkLoadTableStructure withAssociationPath(String associationPath)
   {
      this.associationPath = associationPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for associations
    *******************************************************************************/
   public ArrayList<BulkLoadTableStructure> getAssociations()
   {
      return (this.associations);
   }



   /*******************************************************************************
    ** Setter for associations
    *******************************************************************************/
   public void setAssociations(ArrayList<BulkLoadTableStructure> associations)
   {
      this.associations = associations;
   }



   /*******************************************************************************
    ** Fluent setter for associations
    *******************************************************************************/
   public BulkLoadTableStructure withAssociations(ArrayList<BulkLoadTableStructure> associations)
   {
      this.associations = associations;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void addAssociation(BulkLoadTableStructure association)
   {
      if(this.associations == null)
      {
         this.associations = new ArrayList<>();
      }
      this.associations.add(association);
   }



   /*******************************************************************************
    ** Getter for isBulkEdit
    *******************************************************************************/
   public Boolean getIsBulkEdit()
   {
      return (this.isBulkEdit);
   }



   /*******************************************************************************
    ** Setter for isBulkEdit
    *******************************************************************************/
   public void setIsBulkEdit(Boolean isBulkEdit)
   {
      this.isBulkEdit = isBulkEdit;
   }



   /*******************************************************************************
    ** Fluent setter for isBulkEdit
    *******************************************************************************/
   public BulkLoadTableStructure withIsBulkEdit(Boolean isBulkEdit)
   {
      this.isBulkEdit = isBulkEdit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for keyFields
    *******************************************************************************/
   public String getKeyFields()
   {
      return (this.keyFields);
   }



   /*******************************************************************************
    ** Setter for keyFields
    *******************************************************************************/
   public void setKeyFields(String keyFields)
   {
      this.keyFields = keyFields;
   }



   /*******************************************************************************
    ** Fluent setter for keyFields
    *******************************************************************************/
   public BulkLoadTableStructure withKeyFields(String keyFields)
   {
      this.keyFields = keyFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for possibleKeyFields
    *******************************************************************************/
   public ArrayList<String> getPossibleKeyFields()
   {
      return (this.possibleKeyFields);
   }



   /*******************************************************************************
    ** Setter for possibleKeyFields
    *******************************************************************************/
   public void setPossibleKeyFields(ArrayList<String> possibleKeyFields)
   {
      this.possibleKeyFields = possibleKeyFields;
   }



   /*******************************************************************************
    ** Fluent setter for possibleKeyFields
    *******************************************************************************/
   public BulkLoadTableStructure withPossibleKeyFields(ArrayList<String> possibleKeyFields)
   {
      this.possibleKeyFields = possibleKeyFields;
      return (this);
   }

}
