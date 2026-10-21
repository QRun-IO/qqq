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

package com.kingsrook.qqq.backend.core.alerts;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/*******************************************************************************
 ** A record that an alert is about.  The label is internal (shown in-app to
 ** users who can see the record), never sent to an external channel.
 *******************************************************************************/
public class AlertRecordRef implements Serializable
{
   private String       tableName;
   private Serializable primaryKey;
   private String       label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AlertRecordRef()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AlertRecordRef(String tableName, Serializable primaryKey)
   {
      this.tableName = tableName;
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** One ref per primary key, all in the same table.  Null keys are skipped.
    *******************************************************************************/
   public static List<AlertRecordRef> of(String tableName, Collection<? extends Serializable> primaryKeys)
   {
      List<AlertRecordRef> refs = new ArrayList<>();
      if(primaryKeys != null)
      {
         for(Serializable primaryKey : primaryKeys)
         {
            if(primaryKey != null)
            {
               refs.add(new AlertRecordRef(tableName, primaryKey));
            }
         }
      }
      return (refs);
   }



   /*******************************************************************************
    ** One ref to one record.
    *******************************************************************************/
   public static AlertRecordRef of(String tableName, Serializable primaryKey)
   {
      return (new AlertRecordRef(tableName, primaryKey));
   }



   /*******************************************************************************
    ** Two refs are equal when they name the same table and the same key (keys
    ** compared by their string form, so 1 and "1" match).
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return (true);
      }
      if(!(o instanceof AlertRecordRef that))
      {
         return (false);
      }
      return (Objects.equals(tableName, that.tableName) && Objects.equals(String.valueOf(primaryKey), String.valueOf(that.primaryKey)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return (Objects.hash(tableName, String.valueOf(primaryKey)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (tableName + ":" + primaryKey);
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
   public AlertRecordRef withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for primaryKey
    *******************************************************************************/
   public Serializable getPrimaryKey()
   {
      return (this.primaryKey);
   }



   /*******************************************************************************
    ** Setter for primaryKey
    *******************************************************************************/
   public void setPrimaryKey(Serializable primaryKey)
   {
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for primaryKey
    *******************************************************************************/
   public AlertRecordRef withPrimaryKey(Serializable primaryKey)
   {
      this.primaryKey = primaryKey;
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
    ** Fluent setter for label (internal: shown in-app only, never sent out)
    *******************************************************************************/
   public AlertRecordRef withLabel(String label)
   {
      this.label = label;
      return (this);
   }
}
