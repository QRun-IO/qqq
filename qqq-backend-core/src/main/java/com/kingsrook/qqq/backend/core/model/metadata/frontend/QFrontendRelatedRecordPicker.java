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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.picker.RelatedRecordPickerAction;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.QRelatedRecordPicker;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.RelatedRecordPickerMode;


/*******************************************************************************
 ** A related-record picker as published to frontends: the metadata with its
 ** tables resolved, defaults applied, and what this user may do with it.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QFrontendRelatedRecordPicker
{
   private final String       name;
   private final String       label;
   private final String       mode;
   private final String       fieldName;
   private final String       associationName;
   private final String       targetTableName;
   private final String       childTableName;
   private final String       childTargetFieldName;
   private final QQueryFilter filter;
   private final QQueryFilter defaultFilter;
   private final List<String> columns;
   private final List<String> previewFields;
   private final String       thumbnailField;
   private final List<String> reviewFieldNames;
   private final Integer      maxSelections;
   private final boolean      canCreateTarget;
   private final boolean      canAddChildren;
   private final boolean      canRemoveChildren;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public QFrontendRelatedRecordPicker(AbstractActionInput actionInput, RelatedRecordPickerAction.Resolved resolved)
   {
      QRelatedRecordPicker picker = resolved.getPicker();
      QTableMetaData       target = resolved.getTargetTable();
      QTableMetaData       child  = resolved.getChildTable();

      this.name = picker.getName();
      this.mode = picker.getMode().name();
      this.fieldName = picker.getFieldName();
      this.associationName = picker.getAssociationName();
      this.targetTableName = target.getName();
      this.childTableName = child == null ? null : child.getName();
      this.childTargetFieldName = picker.getChildTargetFieldName();
      this.filter = picker.getFilter();
      this.defaultFilter = picker.getDefaultFilter();
      this.columns = resolved.getColumns();
      this.previewFields = resolved.getPreviewFields();
      this.thumbnailField = picker.getThumbnailField();
      this.reviewFieldNames = picker.getMode() == RelatedRecordPickerMode.MULTI_CHILD ? resolved.getReviewFieldNames() : null;
      this.maxSelections = picker.getMode() == RelatedRecordPickerMode.SINGLE ? null : Objects.requireNonNullElse(picker.getMaxSelections(), QRelatedRecordPicker.DEFAULT_MAX_SELECTIONS);

      this.label = picker.getLabel() != null ? picker.getLabel() : switch(picker.getMode())
      {
         case SINGLE -> "Choose " + resolved.getTable().getField(picker.getFieldName()).getLabel();
         case MULTI_CHILD -> "Add " + target.getLabel();
         case LINK -> "Link " + target.getLabel();
      };

      this.canCreateTarget = picker.getAllowQuickCreate() && PermissionsHelper.hasTablePermission(actionInput, target.getName(), TablePermissionSubType.INSERT);
      this.canAddChildren = child != null && PermissionsHelper.hasTablePermission(actionInput, child.getName(), TablePermissionSubType.INSERT);
      this.canRemoveChildren = picker.getMode() == RelatedRecordPickerMode.LINK && PermissionsHelper.hasTablePermission(actionInput, child.getName(), TablePermissionSubType.DELETE);
   }



   /*******************************************************************************
    ** The picker's name (the path segment of its v1 routes).
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** The dialog title and the child-list button label.
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** SINGLE, MULTI_CHILD or LINK.
    *******************************************************************************/
   public String getMode()
   {
      return (mode);
   }



   /*******************************************************************************
    ** The foreign-key field (SINGLE).
    *******************************************************************************/
   public String getFieldName()
   {
      return (fieldName);
   }



   /*******************************************************************************
    ** The association (MULTI_CHILD, LINK).
    *******************************************************************************/
   public String getAssociationName()
   {
      return (associationName);
   }



   /*******************************************************************************
    ** The table records are picked from.
    *******************************************************************************/
   public String getTargetTableName()
   {
      return (targetTableName);
   }



   /*******************************************************************************
    ** The child or join table (MULTI_CHILD, LINK).
    *******************************************************************************/
   public String getChildTableName()
   {
      return (childTableName);
   }



   /*******************************************************************************
    ** The child table's field holding the target key (MULTI_CHILD, LINK).
    *******************************************************************************/
   public String getChildTargetFieldName()
   {
      return (childTargetFieldName);
   }



   /*******************************************************************************
    ** The filter always applied by the server (may hold ${input.x} variables).
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (filter);
   }



   /*******************************************************************************
    ** The initial, user-editable filter (may hold ${input.x} variables; the
    ** query route returns it resolved).
    *******************************************************************************/
   public QQueryFilter getDefaultFilter()
   {
      return (defaultFilter);
   }



   /*******************************************************************************
    ** The result columns.
    *******************************************************************************/
   public List<String> getColumns()
   {
      return (columns);
   }



   /*******************************************************************************
    ** The preview fields.
    *******************************************************************************/
   public List<String> getPreviewFields()
   {
      return (previewFields);
   }



   /*******************************************************************************
    ** An image URL field of the target, for thumbnails.
    *******************************************************************************/
   public String getThumbnailField()
   {
      return (thumbnailField);
   }



   /*******************************************************************************
    ** The child fields of the review step (MULTI_CHILD; empty skips it).
    *******************************************************************************/
   public List<String> getReviewFieldNames()
   {
      return (reviewFieldNames);
   }



   /*******************************************************************************
    ** The most records one confirm may pick (MULTI_CHILD, LINK).
    *******************************************************************************/
   public Integer getMaxSelections()
   {
      return (maxSelections);
   }



   /*******************************************************************************
    ** Whether this user may quick-create a target record.
    *******************************************************************************/
   public boolean getCanCreateTarget()
   {
      return (canCreateTarget);
   }



   /*******************************************************************************
    ** Whether this user may add child or join rows.
    *******************************************************************************/
   public boolean getCanAddChildren()
   {
      return (canAddChildren);
   }



   /*******************************************************************************
    ** Whether this user may remove links (LINK).
    *******************************************************************************/
   public boolean getCanRemoveChildren()
   {
      return (canRemoveChildren);
   }
}
