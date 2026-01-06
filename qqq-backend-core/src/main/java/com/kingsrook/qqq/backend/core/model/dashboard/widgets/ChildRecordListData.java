/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 * Model containing data structure expected by frontend ChildRecordList widget
 * See {@link ChildRecordListRenderer.Builder} for definitions of several properties
 * in here.
 *******************************************************************************/
public class ChildRecordListData extends QWidgetData
{
   private String         title;
   private QueryOutput    queryOutput;
   private QTableMetaData childTableMetaData;

   /////////////////////////////////////////////////////////////////////////////////////////////
   // Migrate away from childTableMetaData, being a QTableMetaData (which is not designed to  //
   // send) to a frontend instead of a QFrontendTableMetaData.                                //
   // Doing this specifically to get exposed joins sent to frontend, to support joins herein. //
   /////////////////////////////////////////////////////////////////////////////////////////////
   private QFrontendTableMetaData childFrontendTableMetaData;

   private String  tableName;
   private String  tablePath;
   private String  viewAllLink;
   private Integer totalRows;
   private Boolean disableRowClick   = false;
   private Boolean allowRecordEdit   = false;
   private Boolean allowRecordDelete = false;
   private Boolean isInProcess       = false;

   private boolean                   canAddChildRecord = false;
   private Map<String, Serializable> defaultValuesForNewChildRecords;
   private Set<String>               disabledFieldsForNewChildRecords;
   private Map<String, String>       defaultValuesForNewChildRecordsFromParentFields;
   private List<String>              omitFieldNames;
   private List<String>              onlyIncludeFieldNames;
   private List<String>              includeExposedJoinTables;



   /*******************************************************************************
    **
    *******************************************************************************/
   public ChildRecordListData(String title, QueryOutput queryOutput, QTableMetaData childTableMetaData, String tablePath, String viewAllLink, Integer totalRows) throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // ensure that if a table personalizer is active, that it gets applied to the table meta-data for the child table here. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      AbstractTableActionInput tableMetaDataInput = new TableMetaDataInput()
         .withTableName(childTableMetaData.getName())
         .withInputSource(QInputSource.USER);

      QTableMetaData personalizedChildTableMetaData = TableMetaDataPersonalizerAction.execute(tableMetaDataInput);

      this.title = title;
      this.queryOutput = queryOutput;
      this.childTableMetaData = personalizedChildTableMetaData;
      this.tablePath = tablePath;
      this.viewAllLink = viewAllLink;
      this.totalRows = totalRows;

      QBackendMetaData       backendForTable       = QContext.getQInstance().getBackendForTable(childTableMetaData.getName());
      QFrontendTableMetaData frontendTableMetaData = new QFrontendTableMetaData(tableMetaDataInput, backendForTable, personalizedChildTableMetaData, true, true);
      this.childFrontendTableMetaData = frontendTableMetaData;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.CHILD_RECORD_LIST.getType();
   }



   /*******************************************************************************
    ** Getter for title
    **
    *******************************************************************************/
   public String getTitle()
   {
      return title;
   }



   /*******************************************************************************
    ** Setter for title
    **
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    **
    *******************************************************************************/
   public ChildRecordListData withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryOutput
    **
    *******************************************************************************/
   public QueryOutput getQueryOutput()
   {
      return queryOutput;
   }



   /*******************************************************************************
    ** Setter for queryOutput
    **
    *******************************************************************************/
   public void setQueryOutput(QueryOutput queryOutput)
   {
      this.queryOutput = queryOutput;
   }



   /*******************************************************************************
    ** Fluent setter for queryOutput
    **
    *******************************************************************************/
   public ChildRecordListData withQueryOutput(QueryOutput queryOutput)
   {
      this.queryOutput = queryOutput;
      return (this);
   }



   /*******************************************************************************
    ** Getter for childTableMetaData
    **
    *******************************************************************************/
   public QTableMetaData getChildTableMetaData()
   {
      return childTableMetaData;
   }



   /*******************************************************************************
    ** Setter for childTableMetaData
    **
    *******************************************************************************/
   public void setChildTableMetaData(QTableMetaData childTableMetaData)
   {
      this.childTableMetaData = childTableMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for childTableMetaData
    **
    *******************************************************************************/
   public ChildRecordListData withChildTableMetaData(QTableMetaData childTableMetaData)
   {
      this.childTableMetaData = childTableMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tablePath
    **
    *******************************************************************************/
   public String getTablePath()
   {
      return tablePath;
   }



   /*******************************************************************************
    ** Setter for tablePath
    **
    *******************************************************************************/
   public void setTablePath(String tablePath)
   {
      this.tablePath = tablePath;
   }



   /*******************************************************************************
    ** Getter for viewAllLink
    **
    *******************************************************************************/
   public String getViewAllLink()
   {
      return viewAllLink;
   }



   /*******************************************************************************
    ** Setter for viewAllLink
    **
    *******************************************************************************/
   public void setViewAllLink(String viewAllLink)
   {
      this.viewAllLink = viewAllLink;
   }



   /*******************************************************************************
    ** Getter for canAddChildRecord
    **
    *******************************************************************************/
   public boolean getCanAddChildRecord()
   {
      return canAddChildRecord;
   }



   /*******************************************************************************
    ** Setter for canAddChildRecord
    **
    *******************************************************************************/
   public void setCanAddChildRecord(boolean canAddChildRecord)
   {
      this.canAddChildRecord = canAddChildRecord;
   }



   /*******************************************************************************
    ** Fluent setter for canAddChildRecord
    **
    *******************************************************************************/
   public ChildRecordListData withCanAddChildRecord(boolean canAddChildRecord)
   {
      this.canAddChildRecord = canAddChildRecord;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultValuesForNewChildRecords
    **
    *******************************************************************************/
   public Map<String, Serializable> getDefaultValuesForNewChildRecords()
   {
      return defaultValuesForNewChildRecords;
   }



   /*******************************************************************************
    ** Setter for defaultValuesForNewChildRecords
    **
    *******************************************************************************/
   public void setDefaultValuesForNewChildRecords(Map<String, Serializable> defaultValuesForNewChildRecords)
   {
      this.defaultValuesForNewChildRecords = defaultValuesForNewChildRecords;
   }



   /*******************************************************************************
    ** Fluent setter for defaultValuesForNewChildRecords
    **
    *******************************************************************************/
   public ChildRecordListData withDefaultValuesForNewChildRecords(Map<String, Serializable> defaultValuesForNewChildRecords)
   {
      this.defaultValuesForNewChildRecords = defaultValuesForNewChildRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for disabledFieldsForNewChildRecords
    **
    *******************************************************************************/
   public Set<String> getDisabledFieldsForNewChildRecords()
   {
      return disabledFieldsForNewChildRecords;
   }



   /*******************************************************************************
    ** Setter for disabledFieldsForNewChildRecords
    **
    *******************************************************************************/
   public void setDisabledFieldsForNewChildRecords(Set<String> disabledFieldsForNewChildRecords)
   {
      this.disabledFieldsForNewChildRecords = disabledFieldsForNewChildRecords;
   }



   /*******************************************************************************
    ** Fluent setter for disabledFieldsForNewChildRecords
    **
    *******************************************************************************/
   public ChildRecordListData withDisabledFieldsForNewChildRecords(Set<String> disabledFieldsForNewChildRecords)
   {
      this.disabledFieldsForNewChildRecords = disabledFieldsForNewChildRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for totalRows
    *******************************************************************************/
   public Integer getTotalRows()
   {
      return (this.totalRows);
   }



   /*******************************************************************************
    ** Setter for totalRows
    *******************************************************************************/
   public void setTotalRows(Integer totalRows)
   {
      this.totalRows = totalRows;
   }



   /*******************************************************************************
    ** Fluent setter for totalRows
    *******************************************************************************/
   public ChildRecordListData withTotalRows(Integer totalRows)
   {
      this.totalRows = totalRows;
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
   public ChildRecordListData withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tablePath
    *******************************************************************************/
   public ChildRecordListData withTablePath(String tablePath)
   {
      this.tablePath = tablePath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for disableRowClick
    *******************************************************************************/
   public Boolean getDisableRowClick()
   {
      return (this.disableRowClick);
   }



   /*******************************************************************************
    ** Setter for disableRowClick
    *******************************************************************************/
   public void setDisableRowClick(Boolean disableRowClick)
   {
      this.disableRowClick = disableRowClick;
   }



   /*******************************************************************************
    ** Fluent setter for disableRowClick
    *******************************************************************************/
   public ChildRecordListData withDisableRowClick(Boolean disableRowClick)
   {
      this.disableRowClick = disableRowClick;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allowRecordEdit
    *******************************************************************************/
   public Boolean getAllowRecordEdit()
   {
      return (this.allowRecordEdit);
   }



   /*******************************************************************************
    ** Setter for allowRecordEdit
    *******************************************************************************/
   public void setAllowRecordEdit(Boolean allowRecordEdit)
   {
      this.allowRecordEdit = allowRecordEdit;
   }



   /*******************************************************************************
    ** Fluent setter for allowRecordEdit
    *******************************************************************************/
   public ChildRecordListData withAllowRecordEdit(Boolean allowRecordEdit)
   {
      this.allowRecordEdit = allowRecordEdit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allowRecordDelete
    *******************************************************************************/
   public Boolean getAllowRecordDelete()
   {
      return (this.allowRecordDelete);
   }



   /*******************************************************************************
    ** Setter for allowRecordDelete
    *******************************************************************************/
   public void setAllowRecordDelete(Boolean allowRecordDelete)
   {
      this.allowRecordDelete = allowRecordDelete;
   }



   /*******************************************************************************
    ** Fluent setter for allowRecordDelete
    *******************************************************************************/
   public ChildRecordListData withAllowRecordDelete(Boolean allowRecordDelete)
   {
      this.allowRecordDelete = allowRecordDelete;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isInProcess
    *******************************************************************************/
   public Boolean getIsInProcess()
   {
      return (this.isInProcess);
   }



   /*******************************************************************************
    ** Setter for isInProcess
    *******************************************************************************/
   public void setIsInProcess(Boolean isInProcess)
   {
      this.isInProcess = isInProcess;
   }



   /*******************************************************************************
    ** Fluent setter for isInProcess
    *******************************************************************************/
   public ChildRecordListData withIsInProcess(Boolean isInProcess)
   {
      this.isInProcess = isInProcess;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultValuesForNewChildRecordsFromParentFields
    *******************************************************************************/
   public Map<String, String> getDefaultValuesForNewChildRecordsFromParentFields()
   {
      return (this.defaultValuesForNewChildRecordsFromParentFields);
   }



   /*******************************************************************************
    ** Setter for defaultValuesForNewChildRecordsFromParentFields
    *******************************************************************************/
   public void setDefaultValuesForNewChildRecordsFromParentFields(Map<String, String> defaultValuesForNewChildRecordsFromParentFields)
   {
      this.defaultValuesForNewChildRecordsFromParentFields = defaultValuesForNewChildRecordsFromParentFields;
   }



   /*******************************************************************************
    ** Fluent setter for defaultValuesForNewChildRecordsFromParentFields
    *******************************************************************************/
   public ChildRecordListData withDefaultValuesForNewChildRecordsFromParentFields(Map<String, String> defaultValuesForNewChildRecordsFromParentFields)
   {
      this.defaultValuesForNewChildRecordsFromParentFields = defaultValuesForNewChildRecordsFromParentFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for omitFieldNames
    *******************************************************************************/
   public List<String> getOmitFieldNames()
   {
      return (this.omitFieldNames);
   }



   /*******************************************************************************
    ** Setter for omitFieldNames
    *******************************************************************************/
   public void setOmitFieldNames(List<String> omitFieldNames)
   {
      this.omitFieldNames = omitFieldNames;
   }



   /*******************************************************************************
    ** Fluent setter for omitFieldNames
    *******************************************************************************/
   public ChildRecordListData withOmitFieldNames(List<String> omitFieldNames)
   {
      this.omitFieldNames = omitFieldNames;
      return (this);
   }


   /*******************************************************************************
    * Getter for includeExposedJoinTables
    * @see #withIncludeExposedJoinTables(List)
    *******************************************************************************/
   public List<String> getIncludeExposedJoinTables()
   {
      return (this.includeExposedJoinTables);
   }



   /*******************************************************************************
    * Setter for includeExposedJoinTables
    * @see #withIncludeExposedJoinTables(List)
    *******************************************************************************/
   public void setIncludeExposedJoinTables(List<String> includeExposedJoinTables)
   {
      this.includeExposedJoinTables = includeExposedJoinTables;
   }



   /*******************************************************************************
    * Fluent setter for includeExposedJoinTables
    *
    * @param includeExposedJoinTables
    * @return this
    *******************************************************************************/
   public ChildRecordListData withIncludeExposedJoinTables(List<String> includeExposedJoinTables)
   {
      this.includeExposedJoinTables = includeExposedJoinTables;
      return (this);
   }



   /*******************************************************************************
    * Getter for onlyIncludeFieldNames
    * @see #withOnlyIncludeFieldNames(List)
    *******************************************************************************/
   public List<String> getOnlyIncludeFieldNames()
   {
      return (this.onlyIncludeFieldNames);
   }



   /*******************************************************************************
    * Setter for onlyIncludeFieldNames
    * @see #withOnlyIncludeFieldNames(List)
    *******************************************************************************/
   public void setOnlyIncludeFieldNames(List<String> onlyIncludeFieldNames)
   {
      this.onlyIncludeFieldNames = onlyIncludeFieldNames;
   }



   /*******************************************************************************
    * Fluent setter for onlyIncludeFieldNames
    *
    * @param onlyIncludeFieldNames
    * @return this
    *******************************************************************************/
   public ChildRecordListData withOnlyIncludeFieldNames(List<String> onlyIncludeFieldNames)
   {
      this.onlyIncludeFieldNames = onlyIncludeFieldNames;
      return (this);
   }



   /*******************************************************************************
    * Getter for childFrontendTableMetaData
    * @see #withChildFrontendTableMetaData(QFrontendTableMetaData)
    *******************************************************************************/
   public QFrontendTableMetaData getChildFrontendTableMetaData()
   {
      return (this.childFrontendTableMetaData);
   }



   /*******************************************************************************
    * Setter for childFrontendTableMetaData
    * @see #withChildFrontendTableMetaData(QFrontendTableMetaData)
    *******************************************************************************/
   public void setChildFrontendTableMetaData(QFrontendTableMetaData childFrontendTableMetaData)
   {
      this.childFrontendTableMetaData = childFrontendTableMetaData;
   }



   /*******************************************************************************
    * Fluent setter for childFrontendTableMetaData
    *
    * @param childFrontendTableMetaData
    * @return this
    *******************************************************************************/
   public ChildRecordListData withChildFrontendTableMetaData(QFrontendTableMetaData childFrontendTableMetaData)
   {
      this.childFrontendTableMetaData = childFrontendTableMetaData;
      return (this);
   }


}



