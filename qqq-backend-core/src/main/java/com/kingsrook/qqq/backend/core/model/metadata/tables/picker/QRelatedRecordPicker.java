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

package com.kingsrook.qqq.backend.core.model.metadata.tables.picker;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 ** A related-record picker declared on a table: a modal for choosing records of
 ** a target table, for a foreign-key field ({@link RelatedRecordPickerMode#SINGLE}),
 ** to add as child rows through an association
 ** ({@link RelatedRecordPickerMode#MULTI_CHILD}), or to link and unlink through
 ** an association to a join table ({@link RelatedRecordPickerMode#LINK}).
 **
 ** Served by the v1 routes {@code /table/{table}/relatedRecordPicker/{name}/...}
 ** and validated by QInstanceValidator.  See docs/RELATED-RECORD-PICKER.md.
 *******************************************************************************/
public class QRelatedRecordPicker implements QMetaDataObject, Cloneable
{
   public static final int DEFAULT_MAX_SELECTIONS = 200;

   private String                  name;
   private String                  label;
   private RelatedRecordPickerMode mode;

   private String fieldName;
   private String associationName;
   private String childTargetFieldName;
   private String targetTableName;

   private QQueryFilter filter;
   private QQueryFilter defaultFilter;

   private List<String> columns;
   private List<String> previewFields;
   private String       thumbnailField;

   private boolean allowQuickCreate = true;

   private Map<String, Serializable> childFieldDefaults;
   private Map<String, String>       childValuesFromTarget;
   private List<String>              reviewFieldNames;
   private Integer                   maxSelections = DEFAULT_MAX_SELECTIONS;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public QRelatedRecordPicker()
   {
   }



   /*******************************************************************************
    ** Deep-enough copy (lists, maps and filters are copied).
    *******************************************************************************/
   @Override
   public QRelatedRecordPicker clone()
   {
      try
      {
         QRelatedRecordPicker clone = (QRelatedRecordPicker) super.clone();
         clone.filter = filter == null ? null : filter.clone();
         clone.defaultFilter = defaultFilter == null ? null : defaultFilter.clone();
         clone.columns = columns == null ? null : new ArrayList<>(columns);
         clone.previewFields = previewFields == null ? null : new ArrayList<>(previewFields);
         clone.childFieldDefaults = childFieldDefaults == null ? null : new LinkedHashMap<>(childFieldDefaults);
         clone.childValuesFromTarget = childValuesFromTarget == null ? null : new LinkedHashMap<>(childValuesFromTarget);
         clone.reviewFieldNames = reviewFieldNames == null ? null : new ArrayList<>(reviewFieldNames);
         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw (new RuntimeException(e));
      }
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
   public QRelatedRecordPicker withName(String name)
   {
      this.name = name;
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
   public QRelatedRecordPicker withLabel(String label)
   {
      this.label = label;
      return (this);
   }


   /*******************************************************************************
    ** Getter for mode
    *******************************************************************************/
   public RelatedRecordPickerMode getMode()
   {
      return (this.mode);
   }



   /*******************************************************************************
    ** Setter for mode
    *******************************************************************************/
   public void setMode(RelatedRecordPickerMode mode)
   {
      this.mode = mode;
   }



   /*******************************************************************************
    ** Fluent setter for mode
    *******************************************************************************/
   public QRelatedRecordPicker withMode(RelatedRecordPickerMode mode)
   {
      this.mode = mode;
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
   public QRelatedRecordPicker withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for associationName
    *******************************************************************************/
   public String getAssociationName()
   {
      return (this.associationName);
   }



   /*******************************************************************************
    ** Setter for associationName
    *******************************************************************************/
   public void setAssociationName(String associationName)
   {
      this.associationName = associationName;
   }



   /*******************************************************************************
    ** Fluent setter for associationName
    *******************************************************************************/
   public QRelatedRecordPicker withAssociationName(String associationName)
   {
      this.associationName = associationName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for childTargetFieldName
    *******************************************************************************/
   public String getChildTargetFieldName()
   {
      return (this.childTargetFieldName);
   }



   /*******************************************************************************
    ** Setter for childTargetFieldName
    *******************************************************************************/
   public void setChildTargetFieldName(String childTargetFieldName)
   {
      this.childTargetFieldName = childTargetFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for childTargetFieldName
    *******************************************************************************/
   public QRelatedRecordPicker withChildTargetFieldName(String childTargetFieldName)
   {
      this.childTargetFieldName = childTargetFieldName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for targetTableName
    *******************************************************************************/
   public String getTargetTableName()
   {
      return (this.targetTableName);
   }



   /*******************************************************************************
    ** Setter for targetTableName
    *******************************************************************************/
   public void setTargetTableName(String targetTableName)
   {
      this.targetTableName = targetTableName;
   }



   /*******************************************************************************
    ** Fluent setter for targetTableName
    *******************************************************************************/
   public QRelatedRecordPicker withTargetTableName(String targetTableName)
   {
      this.targetTableName = targetTableName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for filter
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (this.filter);
   }



   /*******************************************************************************
    ** Setter for filter
    *******************************************************************************/
   public void setFilter(QQueryFilter filter)
   {
      this.filter = filter;
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public QRelatedRecordPicker withFilter(QQueryFilter filter)
   {
      this.filter = filter;
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
   public QRelatedRecordPicker withDefaultFilter(QQueryFilter defaultFilter)
   {
      this.defaultFilter = defaultFilter;
      return (this);
   }


   /*******************************************************************************
    ** Getter for columns
    *******************************************************************************/
   public List<String> getColumns()
   {
      return (this.columns);
   }



   /*******************************************************************************
    ** Setter for columns
    *******************************************************************************/
   public void setColumns(List<String> columns)
   {
      this.columns = columns;
   }



   /*******************************************************************************
    ** Fluent setter for columns
    *******************************************************************************/
   public QRelatedRecordPicker withColumns(List<String> columns)
   {
      this.columns = columns;
      return (this);
   }


   /*******************************************************************************
    ** Getter for previewFields
    *******************************************************************************/
   public List<String> getPreviewFields()
   {
      return (this.previewFields);
   }



   /*******************************************************************************
    ** Setter for previewFields
    *******************************************************************************/
   public void setPreviewFields(List<String> previewFields)
   {
      this.previewFields = previewFields;
   }



   /*******************************************************************************
    ** Fluent setter for previewFields
    *******************************************************************************/
   public QRelatedRecordPicker withPreviewFields(List<String> previewFields)
   {
      this.previewFields = previewFields;
      return (this);
   }


   /*******************************************************************************
    ** Getter for thumbnailField
    *******************************************************************************/
   public String getThumbnailField()
   {
      return (this.thumbnailField);
   }



   /*******************************************************************************
    ** Setter for thumbnailField
    *******************************************************************************/
   public void setThumbnailField(String thumbnailField)
   {
      this.thumbnailField = thumbnailField;
   }



   /*******************************************************************************
    ** Fluent setter for thumbnailField
    *******************************************************************************/
   public QRelatedRecordPicker withThumbnailField(String thumbnailField)
   {
      this.thumbnailField = thumbnailField;
      return (this);
   }


   /*******************************************************************************
    ** Getter for allowQuickCreate
    *******************************************************************************/
   public boolean getAllowQuickCreate()
   {
      return (this.allowQuickCreate);
   }



   /*******************************************************************************
    ** Setter for allowQuickCreate
    *******************************************************************************/
   public void setAllowQuickCreate(boolean allowQuickCreate)
   {
      this.allowQuickCreate = allowQuickCreate;
   }



   /*******************************************************************************
    ** Fluent setter for allowQuickCreate
    *******************************************************************************/
   public QRelatedRecordPicker withAllowQuickCreate(boolean allowQuickCreate)
   {
      this.allowQuickCreate = allowQuickCreate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for childFieldDefaults
    *******************************************************************************/
   public Map<String, Serializable> getChildFieldDefaults()
   {
      return (this.childFieldDefaults);
   }



   /*******************************************************************************
    ** Setter for childFieldDefaults
    *******************************************************************************/
   public void setChildFieldDefaults(Map<String, Serializable> childFieldDefaults)
   {
      this.childFieldDefaults = childFieldDefaults;
   }



   /*******************************************************************************
    ** Fluent setter for childFieldDefaults
    *******************************************************************************/
   public QRelatedRecordPicker withChildFieldDefaults(Map<String, Serializable> childFieldDefaults)
   {
      this.childFieldDefaults = childFieldDefaults;
      return (this);
   }


   /*******************************************************************************
    ** Getter for childValuesFromTarget
    *******************************************************************************/
   public Map<String, String> getChildValuesFromTarget()
   {
      return (this.childValuesFromTarget);
   }



   /*******************************************************************************
    ** Setter for childValuesFromTarget
    *******************************************************************************/
   public void setChildValuesFromTarget(Map<String, String> childValuesFromTarget)
   {
      this.childValuesFromTarget = childValuesFromTarget;
   }



   /*******************************************************************************
    ** Fluent setter for childValuesFromTarget
    *******************************************************************************/
   public QRelatedRecordPicker withChildValuesFromTarget(Map<String, String> childValuesFromTarget)
   {
      this.childValuesFromTarget = childValuesFromTarget;
      return (this);
   }


   /*******************************************************************************
    ** Getter for reviewFieldNames
    *******************************************************************************/
   public List<String> getReviewFieldNames()
   {
      return (this.reviewFieldNames);
   }



   /*******************************************************************************
    ** Setter for reviewFieldNames
    *******************************************************************************/
   public void setReviewFieldNames(List<String> reviewFieldNames)
   {
      this.reviewFieldNames = reviewFieldNames;
   }



   /*******************************************************************************
    ** Fluent setter for reviewFieldNames
    *******************************************************************************/
   public QRelatedRecordPicker withReviewFieldNames(List<String> reviewFieldNames)
   {
      this.reviewFieldNames = reviewFieldNames;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxSelections
    *******************************************************************************/
   public Integer getMaxSelections()
   {
      return (this.maxSelections);
   }



   /*******************************************************************************
    ** Setter for maxSelections
    *******************************************************************************/
   public void setMaxSelections(Integer maxSelections)
   {
      this.maxSelections = maxSelections;
   }



   /*******************************************************************************
    ** Fluent setter for maxSelections
    *******************************************************************************/
   public QRelatedRecordPicker withMaxSelections(Integer maxSelections)
   {
      this.maxSelections = maxSelections;
      return (this);
   }
}
