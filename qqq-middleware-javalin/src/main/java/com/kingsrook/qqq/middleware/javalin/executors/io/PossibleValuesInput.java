/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Input for the possible values middleware executor.
 *******************************************************************************/
public class PossibleValuesInput extends AbstractMiddlewareInput
{
   private String                    possibleValueSourceName;
   private String                    tableName;
   private String                    processName;
   private String                    fieldName;
   private String                    searchTerm;
   private List<String>              idList;
   private Map<String, Serializable> otherValues;
   private String                    useCase;
   private QQueryFilter              defaultFilter;



   /*******************************************************************************
    ** Getter for possibleValueSourceName
    *******************************************************************************/
   public String getPossibleValueSourceName()
   {
      return (this.possibleValueSourceName);
   }



   /*******************************************************************************
    ** Setter for possibleValueSourceName
    *******************************************************************************/
   public void setPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for possibleValueSourceName
    *******************************************************************************/
   public PossibleValuesInput withPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
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
   public PossibleValuesInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Setter for processName
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public PossibleValuesInput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public PossibleValuesInput withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for searchTerm
    *******************************************************************************/
   public String getSearchTerm()
   {
      return (this.searchTerm);
   }



   /*******************************************************************************
    ** Setter for searchTerm
    *******************************************************************************/
   public void setSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
   }



   /*******************************************************************************
    ** Fluent setter for searchTerm
    *******************************************************************************/
   public PossibleValuesInput withSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
      return (this);
   }



   /*******************************************************************************
    ** Getter for idList
    *******************************************************************************/
   public List<String> getIdList()
   {
      return (this.idList);
   }



   /*******************************************************************************
    ** Setter for idList
    *******************************************************************************/
   public void setIdList(List<String> idList)
   {
      this.idList = idList;
   }



   /*******************************************************************************
    ** Fluent setter for idList
    *******************************************************************************/
   public PossibleValuesInput withIdList(List<String> idList)
   {
      this.idList = idList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for otherValues
    *******************************************************************************/
   public Map<String, Serializable> getOtherValues()
   {
      return (this.otherValues);
   }



   /*******************************************************************************
    ** Setter for otherValues
    *******************************************************************************/
   public void setOtherValues(Map<String, Serializable> otherValues)
   {
      this.otherValues = otherValues;
   }



   /*******************************************************************************
    ** Fluent setter for otherValues
    *******************************************************************************/
   public PossibleValuesInput withOtherValues(Map<String, Serializable> otherValues)
   {
      this.otherValues = otherValues;
      return (this);
   }



   /*******************************************************************************
    ** Getter for useCase
    *******************************************************************************/
   public String getUseCase()
   {
      return (this.useCase);
   }



   /*******************************************************************************
    ** Setter for useCase
    *******************************************************************************/
   public void setUseCase(String useCase)
   {
      this.useCase = useCase;
   }



   /*******************************************************************************
    ** Fluent setter for useCase
    *******************************************************************************/
   public PossibleValuesInput withUseCase(String useCase)
   {
      this.useCase = useCase;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultFilter
    *******************************************************************************/
   public QQueryFilter getDefaultFilter()
   {
      return (this.defaultFilter);
   }



   /*******************************************************************************
    ** Setter for defaultFilter
    *******************************************************************************/
   public void setDefaultFilter(QQueryFilter defaultFilter)
   {
      this.defaultFilter = defaultFilter;
   }



   /*******************************************************************************
    ** Fluent setter for defaultFilter
    *******************************************************************************/
   public PossibleValuesInput withDefaultFilter(QQueryFilter defaultFilter)
   {
      this.defaultFilter = defaultFilter;
      return (this);
   }

}
