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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendRelatedRecordPicker;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** A related-record picker of a table (docs/RELATED-RECORD-PICKER.md): what it
 ** picks, how to show it, and what the current user may do with it.  Served by
 ** the relatedRecordPicker routes.
 *******************************************************************************/
public class RelatedRecordPicker implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendRelatedRecordPicker wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RelatedRecordPicker(QFrontendRelatedRecordPicker wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RelatedRecordPicker()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Picker name; the {pickerName} path segment of the relatedRecordPicker routes")
   public String getName()
   {
      return (wrapped.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Dialog title, and the label of the button that opens it")
   public String getLabel()
   {
      return (wrapped.getLabel());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("SINGLE: choose one record for fieldName.  MULTI_CHILD: add child rows through associationName, after a review of reviewFieldNames.  LINK: link and unlink through associationName (a join table).")
   public String getMode()
   {
      return (wrapped.getMode());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("The foreign-key field the picker sets (SINGLE)")
   public String getFieldName()
   {
      return (wrapped.getFieldName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("The association whose rows the picker adds (MULTI_CHILD, LINK)")
   public String getAssociationName()
   {
      return (wrapped.getAssociationName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("The table records are picked from (read its metadata for fields, sections and quickCreateSectionName)")
   public String getTargetTableName()
   {
      return (wrapped.getTargetTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("The child or join table new rows go into (MULTI_CHILD, LINK)")
   public String getChildTableName()
   {
      return (wrapped.getChildTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("The child table's field that holds the picked record's key (MULTI_CHILD, LINK)")
   public String getChildTargetFieldName()
   {
      return (wrapped.getChildTargetFieldName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Filter the server always applies to the picker's queries (may contain ${input.fieldName} variables, read from the parent record)")
   public QueryFilter getFilter()
   {
      return (wrapped.getFilter() == null ? null : new QueryFilter(wrapped.getFilter()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Initial, user-editable filter; the query route returns it resolved as resolvedDefaultFilter")
   public QueryFilter getDefaultFilter()
   {
      return (wrapped.getDefaultFilter() == null ? null : new QueryFilter(wrapped.getDefaultFilter()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Target fields to show as result columns")
   @OpenAPIListItems(value = String.class)
   public List<String> getColumns()
   {
      return (wrapped.getColumns());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Target fields to show for the highlighted record")
   @OpenAPIListItems(value = String.class)
   public List<String> getPreviewFields()
   {
      return (wrapped.getPreviewFields());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Target field holding an image URL, for thumbnails")
   public String getThumbnailField()
   {
      return (wrapped.getThumbnailField());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Child fields the user fills per picked record before saving (MULTI_CHILD); empty means no review step")
   @OpenAPIListItems(value = String.class)
   public List<String> getReviewFieldNames()
   {
      return (wrapped.getReviewFieldNames());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Most records one add or link may pick (MULTI_CHILD, LINK)")
   public Integer getMaxSelections()
   {
      return (wrapped.getMaxSelections());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Whether to offer quick-create of a target record (insert route of targetTableName)")
   public Boolean getCanCreateTarget()
   {
      return (wrapped.getCanCreateTarget());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Whether the user may add child or join rows (prepare and add routes)")
   public Boolean getCanAddChildren()
   {
      return (wrapped.getCanAddChildren());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Whether the user may unlink (remove route; LINK only)")
   public Boolean getCanRemoveChildren()
   {
      return (wrapped.getCanRemoveChildren());
   }
}
