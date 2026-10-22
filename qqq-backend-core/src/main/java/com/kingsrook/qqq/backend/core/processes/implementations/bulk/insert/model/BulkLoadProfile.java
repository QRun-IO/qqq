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


/***************************************************************************
 * this is the model of a saved bulk load profile - which is what passes back
 * and forth with the frontend.
 ****************************************************************************/
public class BulkLoadProfile implements Serializable
{
   private ArrayList<BulkLoadProfileField> fieldList;

   private Boolean hasHeaderRow;
   private String  layout;
   private String  version;
   private Boolean isBulkEdit;
   private String  keyFields;



   /*******************************************************************************
    ** Getter for fieldList
    *******************************************************************************/
   public ArrayList<BulkLoadProfileField> getFieldList()
   {
      return (this.fieldList);
   }



   /*******************************************************************************
    ** Getter for hasHeaderRow
    *******************************************************************************/
   public Boolean getHasHeaderRow()
   {
      return (this.hasHeaderRow);
   }



   /*******************************************************************************
    ** Setter for hasHeaderRow
    *******************************************************************************/
   public void setHasHeaderRow(Boolean hasHeaderRow)
   {
      this.hasHeaderRow = hasHeaderRow;
   }



   /*******************************************************************************
    ** Fluent setter for hasHeaderRow
    *******************************************************************************/
   public BulkLoadProfile withHasHeaderRow(Boolean hasHeaderRow)
   {
      this.hasHeaderRow = hasHeaderRow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for layout
    *******************************************************************************/
   public String getLayout()
   {
      return (this.layout);
   }



   /*******************************************************************************
    ** Setter for layout
    *******************************************************************************/
   public void setLayout(String layout)
   {
      this.layout = layout;
   }



   /*******************************************************************************
    ** Fluent setter for layout
    *******************************************************************************/
   public BulkLoadProfile withLayout(String layout)
   {
      this.layout = layout;
      return (this);
   }



   /*******************************************************************************
    ** Setter for fieldList
    *******************************************************************************/
   public void setFieldList(ArrayList<BulkLoadProfileField> fieldList)
   {
      this.fieldList = fieldList;
   }



   /*******************************************************************************
    ** Fluent setter for fieldList
    *******************************************************************************/
   public BulkLoadProfile withFieldList(ArrayList<BulkLoadProfileField> fieldList)
   {
      this.fieldList = fieldList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for version
    *******************************************************************************/
   public String getVersion()
   {
      return (this.version);
   }



   /*******************************************************************************
    ** Setter for version
    *******************************************************************************/
   public void setVersion(String version)
   {
      this.version = version;
   }



   /*******************************************************************************
    ** Fluent setter for version
    *******************************************************************************/
   public BulkLoadProfile withVersion(String version)
   {
      this.version = version;
      return (this);
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
   public BulkLoadProfile withIsBulkEdit(Boolean isBulkEdit)
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
   public BulkLoadProfile withKeyFields(String keyFields)
   {
      this.keyFields = keyFields;
      return (this);
   }

}
