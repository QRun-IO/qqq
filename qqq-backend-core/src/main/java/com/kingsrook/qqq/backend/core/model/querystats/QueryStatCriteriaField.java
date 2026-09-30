/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.querystats;


import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.tables.QQQTable;


/*******************************************************************************
 ** QRecord Entity for QueryStatCriteriaField table
 *******************************************************************************/
public class QueryStatCriteriaField extends QRecordEntity
{
   public static final String TABLE_NAME = "queryStatCriteriaField";

   @QField(isEditable = false)
   private Integer id;

   @QField(possibleValueSourceName = QueryStat.TABLE_NAME)
   private Integer queryStatId;

   @QField(label = "Table", possibleValueSourceName = QQQTable.TABLE_NAME)
   private Integer qqqTableId;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String name;

   @QField(maxLength = 30, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String operator;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String values;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public QueryStatCriteriaField()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public QueryStatCriteriaField(QRecord record)
   {
      populateFromQRecord(record);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public QueryStatCriteriaField withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryStatId
    *******************************************************************************/
   public Integer getQueryStatId()
   {
      return (this.queryStatId);
   }



   /*******************************************************************************
    ** Setter for queryStatId
    *******************************************************************************/
   public void setQueryStatId(Integer queryStatId)
   {
      this.queryStatId = queryStatId;
   }



   /*******************************************************************************
    ** Fluent setter for queryStatId
    *******************************************************************************/
   public QueryStatCriteriaField withQueryStatId(Integer queryStatId)
   {
      this.queryStatId = queryStatId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for qqqTableId
    *******************************************************************************/
   public Integer getQqqTableId()
   {
      return (this.qqqTableId);
   }



   /*******************************************************************************
    ** Setter for qqqTableId
    *******************************************************************************/
   public void setQqqTableId(Integer qqqTableId)
   {
      this.qqqTableId = qqqTableId;
   }



   /*******************************************************************************
    ** Fluent setter for qqqTableId
    *******************************************************************************/
   public QueryStatCriteriaField withQqqTableId(Integer qqqTableId)
   {
      this.qqqTableId = qqqTableId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public QueryStatCriteriaField withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for operator
    *******************************************************************************/
   public String getOperator()
   {
      return (this.operator);
   }



   /*******************************************************************************
    ** Setter for operator
    *******************************************************************************/
   public void setOperator(String operator)
   {
      this.operator = operator;
   }



   /*******************************************************************************
    ** Fluent setter for operator
    *******************************************************************************/
   public QueryStatCriteriaField withOperator(String operator)
   {
      this.operator = operator;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public String getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(String values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public QueryStatCriteriaField withValues(String values)
   {
      this.values = values;
      return (this);
   }

}
