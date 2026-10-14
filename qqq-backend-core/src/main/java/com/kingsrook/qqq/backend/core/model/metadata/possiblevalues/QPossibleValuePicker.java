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

package com.kingsrook.qqq.backend.core.model.metadata.possiblevalues;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/*******************************************************************************
 ** Declares that a table-type possible value source is chosen with a record
 ** picker (a searchable, filterable, paged dialog) rather than only an inline
 ** dropdown - for sources over tables that grow large (e.g., 100,000+ media).
 **
 ** Every field that uses the source gets the picker.  The fields named here
 ** are the only ones the possible-values API will filter or sort on, or
 ** return per option, for a picker request: a picker never exposes other
 ** columns of the source table.
 **
 ** - thumbnailField: an image URL shown for each option (and the selection).
 ** - previewField: a larger image URL for the preview pane (else thumbnail).
 ** - subtitleFields: short secondary text under each option label.
 ** - detailFields: shown in the preview pane (e.g., dimensions, size, alt).
 ** - filterFields: offered as filters; their types/PVSes drive the controls.
 ** - searchFields: matched with CONTAINS (default: the source searchFields).
 ** - sorts: the offered orderings; the first is the default.  A sort on the
 **   primary key alone is paged by keyset (no OFFSET, no count).
 ** - pageSize: options per page (capped by the API).
 ** - uploadProcessName / uploadResultIdField: a process the picker can run
 **   to add a record (e.g., upload an image); when it completes, the new
 **   record's id is read from this process output value and selected.
 *******************************************************************************/
public class QPossibleValuePicker implements Serializable
{
   private String       thumbnailField;
   private String       previewField;
   private List<String> subtitleFields;
   private List<String> detailFields;
   private List<String> filterFields;
   private List<String> searchFields;
   private List<Sort>   sorts;
   private Integer      pageSize = 48;
   private String       uploadProcessName;
   private String       uploadResultIdField;



   /*******************************************************************************
    ** One ordering offered by the picker.
    *******************************************************************************/
   public static class Sort implements Serializable
   {
      private String  label;
      private String  fieldName;
      private boolean isAscending = true;



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      public Sort()
      {
      }



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      public Sort(String label, String fieldName, boolean isAscending)
      {
         this.label = label;
         this.fieldName = fieldName;
         this.isAscending = isAscending;
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
       ** Getter for isAscending
       *******************************************************************************/
      public boolean getIsAscending()
      {
         return (this.isAscending);
      }



      /*******************************************************************************
       ** Setter for isAscending
       *******************************************************************************/
      public void setIsAscending(boolean isAscending)
      {
         this.isAscending = isAscending;
      }
   }



   /*******************************************************************************
    ** Every field a picker request may return per option (thumbnail, preview,
    ** subtitle and detail fields), in declaration order, without duplicates.
    *******************************************************************************/
   public Set<String> getDisplayFieldNames()
   {
      Set<String> names = new LinkedHashSet<>();
      if(thumbnailField != null)
      {
         names.add(thumbnailField);
      }
      if(previewField != null)
      {
         names.add(previewField);
      }
      if(subtitleFields != null)
      {
         names.addAll(subtitleFields);
      }
      if(detailFields != null)
      {
         names.addAll(detailFields);
      }
      return (names);
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
   public QPossibleValuePicker withThumbnailField(String thumbnailField)
   {
      this.thumbnailField = thumbnailField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for previewField
    *******************************************************************************/
   public String getPreviewField()
   {
      return (this.previewField);
   }



   /*******************************************************************************
    ** Setter for previewField
    *******************************************************************************/
   public void setPreviewField(String previewField)
   {
      this.previewField = previewField;
   }



   /*******************************************************************************
    ** Fluent setter for previewField
    *******************************************************************************/
   public QPossibleValuePicker withPreviewField(String previewField)
   {
      this.previewField = previewField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subtitleFields
    *******************************************************************************/
   public List<String> getSubtitleFields()
   {
      return (this.subtitleFields);
   }



   /*******************************************************************************
    ** Setter for subtitleFields
    *******************************************************************************/
   public void setSubtitleFields(List<String> subtitleFields)
   {
      this.subtitleFields = subtitleFields;
   }



   /*******************************************************************************
    ** Fluent setter for subtitleFields
    *******************************************************************************/
   public QPossibleValuePicker withSubtitleFields(List<String> subtitleFields)
   {
      this.subtitleFields = subtitleFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for detailFields
    *******************************************************************************/
   public List<String> getDetailFields()
   {
      return (this.detailFields);
   }



   /*******************************************************************************
    ** Setter for detailFields
    *******************************************************************************/
   public void setDetailFields(List<String> detailFields)
   {
      this.detailFields = detailFields;
   }



   /*******************************************************************************
    ** Fluent setter for detailFields
    *******************************************************************************/
   public QPossibleValuePicker withDetailFields(List<String> detailFields)
   {
      this.detailFields = detailFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filterFields
    *******************************************************************************/
   public List<String> getFilterFields()
   {
      return (this.filterFields);
   }



   /*******************************************************************************
    ** Setter for filterFields
    *******************************************************************************/
   public void setFilterFields(List<String> filterFields)
   {
      this.filterFields = filterFields;
   }



   /*******************************************************************************
    ** Fluent setter for filterFields
    *******************************************************************************/
   public QPossibleValuePicker withFilterFields(List<String> filterFields)
   {
      this.filterFields = filterFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for searchFields
    *******************************************************************************/
   public List<String> getSearchFields()
   {
      return (this.searchFields);
   }



   /*******************************************************************************
    ** Setter for searchFields
    *******************************************************************************/
   public void setSearchFields(List<String> searchFields)
   {
      this.searchFields = searchFields;
   }



   /*******************************************************************************
    ** Fluent setter for searchFields
    *******************************************************************************/
   public QPossibleValuePicker withSearchFields(List<String> searchFields)
   {
      this.searchFields = searchFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sorts
    *******************************************************************************/
   public List<Sort> getSorts()
   {
      return (this.sorts);
   }



   /*******************************************************************************
    ** Setter for sorts
    *******************************************************************************/
   public void setSorts(List<Sort> sorts)
   {
      this.sorts = sorts;
   }



   /*******************************************************************************
    ** Fluent setter for sorts
    *******************************************************************************/
   public QPossibleValuePicker withSorts(List<Sort> sorts)
   {
      this.sorts = sorts;
      return (this);
   }



   /*******************************************************************************
    ** Fluent adder for one sort
    *******************************************************************************/
   public QPossibleValuePicker withSort(String label, String fieldName, boolean isAscending)
   {
      if(this.sorts == null)
      {
         this.sorts = new ArrayList<>();
      }
      this.sorts.add(new Sort(label, fieldName, isAscending));
      return (this);
   }



   /*******************************************************************************
    ** Getter for pageSize
    *******************************************************************************/
   public Integer getPageSize()
   {
      return (this.pageSize);
   }



   /*******************************************************************************
    ** Setter for pageSize
    *******************************************************************************/
   public void setPageSize(Integer pageSize)
   {
      this.pageSize = pageSize;
   }



   /*******************************************************************************
    ** Fluent setter for pageSize
    *******************************************************************************/
   public QPossibleValuePicker withPageSize(Integer pageSize)
   {
      this.pageSize = pageSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for uploadProcessName
    *******************************************************************************/
   public String getUploadProcessName()
   {
      return (this.uploadProcessName);
   }



   /*******************************************************************************
    ** Setter for uploadProcessName
    *******************************************************************************/
   public void setUploadProcessName(String uploadProcessName)
   {
      this.uploadProcessName = uploadProcessName;
   }



   /*******************************************************************************
    ** Fluent setter for uploadProcessName
    *******************************************************************************/
   public QPossibleValuePicker withUploadProcessName(String uploadProcessName)
   {
      this.uploadProcessName = uploadProcessName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for uploadResultIdField
    *******************************************************************************/
   public String getUploadResultIdField()
   {
      return (this.uploadResultIdField);
   }



   /*******************************************************************************
    ** Setter for uploadResultIdField
    *******************************************************************************/
   public void setUploadResultIdField(String uploadResultIdField)
   {
      this.uploadResultIdField = uploadResultIdField;
   }



   /*******************************************************************************
    ** Fluent setter for uploadResultIdField
    *******************************************************************************/
   public QPossibleValuePicker withUploadResultIdField(String uploadResultIdField)
   {
      this.uploadResultIdField = uploadResultIdField;
      return (this);
   }
}
