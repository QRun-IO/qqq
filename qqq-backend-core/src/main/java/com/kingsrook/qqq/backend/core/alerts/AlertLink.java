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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** A link from an alert to where a person fixes it: a record, a table with a
 ** filter, an app or widget path, or an absolute URL.  Exactly one target is
 ** expected, so construct these with the static factories.
 *******************************************************************************/
public class AlertLink implements Serializable
{
   private String       label;
   private String       tableName;
   private Serializable recordId;
   private QQueryFilter filter;
   private String       path;
   private String       url;



   /*******************************************************************************
    ** Link to one record.
    *******************************************************************************/
   public static AlertLink toRecord(String tableName, Serializable recordId, String label)
   {
      AlertLink link = new AlertLink();
      link.tableName = tableName;
      link.recordId = recordId;
      link.label = label;
      return (link);
   }



   /*******************************************************************************
    ** Link to a table, optionally filtered.
    *******************************************************************************/
   public static AlertLink toTable(String tableName, QQueryFilter filter, String label)
   {
      AlertLink link = new AlertLink();
      link.tableName = tableName;
      link.filter = filter;
      link.label = label;
      return (link);
   }



   /*******************************************************************************
    ** Link to an app or widget path within the application (e.g. "/admin/cdn").
    *******************************************************************************/
   public static AlertLink toPath(String path, String label)
   {
      AlertLink link = new AlertLink();
      link.path = path;
      link.label = label;
      return (link);
   }



   /*******************************************************************************
    ** Link to an absolute URL.  Implementations only follow these for types
    ** that declare them, so producer data cannot inject arbitrary links.
    *******************************************************************************/
   public static AlertLink toUrl(String url, String label)
   {
      AlertLink link = new AlertLink();
      link.url = url;
      link.label = label;
      return (link);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Getter for recordId
    *******************************************************************************/
   public Serializable getRecordId()
   {
      return (this.recordId);
   }



   /*******************************************************************************
    ** Getter for filter
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (this.filter);
   }



   /*******************************************************************************
    ** Getter for path
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Getter for url
    *******************************************************************************/
   public String getUrl()
   {
      return (this.url);
   }
}
