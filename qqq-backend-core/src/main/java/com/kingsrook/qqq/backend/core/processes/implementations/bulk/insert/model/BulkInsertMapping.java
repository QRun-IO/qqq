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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.FlatRowsToRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.RowsToRecordInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.TallRowsToRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkInsertMapping implements Serializable
{
   private String  tableName;
   private Boolean hasHeaderRow;

   private Layout layout;

   /////////////////////////////////////////////////////////////////////
   // keys in here are:                                               //
   // fieldName (for the main table)                                  //
   // association.fieldName (for an associated child table)           //
   // association.association.fieldName (for grandchild associations) //
   /////////////////////////////////////////////////////////////////////
   private Map<String, String>                    fieldNameToHeaderNameMap   = new HashMap<>();
   private Map<String, Integer>                   fieldNameToIndexMap        = new HashMap<>();
   private Map<String, Serializable>              fieldNameToDefaultValueMap = new HashMap<>();
   private Map<String, Map<String, Serializable>> fieldNameToValueMapping    = new HashMap<>();

   private Map<String, List<Integer>> tallLayoutGroupByIndexMap = new HashMap<>();

   private List<String> mappedAssociations = new ArrayList<>();

   private Memoization<Pair<String, String>, Boolean> shouldProcessFieldForTable = new Memoization<>();



   /***************************************************************************
    **
    ***************************************************************************/
   public enum Layout implements PossibleValueEnum<String>
   {
      FLAT(FlatRowsToRecord::new),
      TALL(TallRowsToRecord::new),
      WIDE(WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping::new);


      /***************************************************************************
       **
       ***************************************************************************/
      private final Supplier<? extends RowsToRecordInterface> supplier;



      /***************************************************************************
       **
       ***************************************************************************/
      Layout(Supplier<? extends RowsToRecordInterface> supplier)
      {
         this.supplier = supplier;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      public RowsToRecordInterface newRowsToRecordInterface()
      {
         return (supplier.get());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getPossibleValueId()
      {
         return name();
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         return StringUtils.ucFirst(name().toLowerCase());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @JsonIgnore
   public Map<String, Integer> getFieldIndexes(QTableMetaData table, String associationNameChain, BulkLoadFileRow headerRow) throws QException
   {
      return getFieldIndexes(table, associationNameChain, headerRow, null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @JsonIgnore
   public Map<String, Integer> getFieldIndexes(QTableMetaData table, String associationNameChain, BulkLoadFileRow headerRow, List<Integer> wideAssociationIndexes) throws QException
   {
      if(hasHeaderRow && fieldNameToHeaderNameMap != null)
      {
         return (getFieldIndexesForHeaderMappedUseCase(table, associationNameChain, headerRow, wideAssociationIndexes));
      }
      else if(fieldNameToIndexMap != null)
      {
         return (getFieldIndexesForNoHeaderUseCase(table, associationNameChain, wideAssociationIndexes));
      }

      throw (new QException("Mapping was not properly configured."));
   }



   /***************************************************************************
    ** get a map of default-values for fields in a given table (at the specified
    ** association chain and wide-indexes).  Will only include fields using a
    ** default value.
    ***************************************************************************/
   @JsonIgnore
   public Map<String, Serializable> getFieldDefaultValues(QTableMetaData table, String associationNameChain, List<Integer> wideAssociationIndexes) throws QException
   {
      Map<String, Serializable> rs = new HashMap<>();

      String wideAssociationSuffix = "";
      if(CollectionUtils.nullSafeHasContents(wideAssociationIndexes))
      {
         wideAssociationSuffix = "," + StringUtils.join(".", wideAssociationIndexes);
      }

      ///////////////////////////////////////////////////////////////////////////
      // loop over fields - adding them to the rs if they have a default value //
      ///////////////////////////////////////////////////////////////////////////
      String fieldNamePrefix = !StringUtils.hasContent(associationNameChain) ? "" : associationNameChain + ".";
      for(QFieldMetaData field : table.getFields().values())
      {
         Serializable defaultValue = fieldNameToDefaultValueMap.get(fieldNamePrefix + field.getName() + wideAssociationSuffix);
         if(defaultValue != null)
         {
            rs.put(field.getName(), defaultValue);
         }
      }

      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @JsonIgnore
   private Map<String, Integer> getFieldIndexesForNoHeaderUseCase(QTableMetaData table, String associationNameChain, List<Integer> wideAssociationIndexes)
   {
      Map<String, Integer> rs = new HashMap<>();

      String wideAssociationSuffix = "";
      if(CollectionUtils.nullSafeHasContents(wideAssociationIndexes))
      {
         wideAssociationSuffix = "," + StringUtils.join(".", wideAssociationIndexes);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // loop over fields - finding what header name they are mapped to - then what index that header is at. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      String fieldNamePrefix = !StringUtils.hasContent(associationNameChain) ? "" : associationNameChain + ".";
      for(QFieldMetaData field : table.getFields().values())
      {
         Integer index = fieldNameToIndexMap.get(fieldNamePrefix + field.getName() + wideAssociationSuffix);
         if(index != null)
         {
            rs.put(field.getName(), index);
         }
      }

      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @JsonIgnore
   public Map<String, Map<String, Serializable>> getFieldNameToValueMappingForTable(String associatedTableName)
   {
      Map<String, Map<String, Serializable>> rs = new HashMap<>();

      for(Map.Entry<String, Map<String, Serializable>> entry : CollectionUtils.nonNullMap(fieldNameToValueMapping).entrySet())
      {
         if(shouldProcessFieldForTable(entry.getKey(), associatedTableName))
         {
            String key = StringUtils.hasContent(associatedTableName) ? entry.getKey().substring(associatedTableName.length() + 1) : entry.getKey();
            rs.put(key, entry.getValue());
         }
      }

      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   boolean shouldProcessFieldForTable(String fieldNameWithAssociationPrefix, String associationChain)
   {
      return shouldProcessFieldForTable.getResult(Pair.of(fieldNameWithAssociationPrefix, associationChain), p ->
      {
         List<String> fieldNameParts   = new ArrayList<>();
         List<String> associationParts = new ArrayList<>();

         if(StringUtils.hasContent(fieldNameWithAssociationPrefix))
         {
            fieldNameParts.addAll(Arrays.asList(fieldNameWithAssociationPrefix.split("\\.")));
         }

         if(StringUtils.hasContent(associationChain))
         {
            associationParts.addAll(Arrays.asList(associationChain.split("\\.")));
         }

         if(!fieldNameParts.isEmpty())
         {
            fieldNameParts.remove(fieldNameParts.size() - 1);
         }

         return (fieldNameParts.equals(associationParts));
      }).orElse(false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Map<String, Integer> getFieldIndexesForHeaderMappedUseCase(QTableMetaData table, String associationNameChain, BulkLoadFileRow headerRow, List<Integer> wideAssociationIndexes)
   {
      Map<String, Integer> rs = new HashMap<>();

      ////////////////////////////////////////////////////////
      // for the current file, map header values to indexes //
      ////////////////////////////////////////////////////////
      Map<String, Integer> headerToIndexMap = new HashMap<>();
      for(int i = 0; i < headerRow.size(); i++)
      {
         String headerValue = ValueUtils.getValueAsString(headerRow.getValue(i));
         headerToIndexMap.put(headerValue, i);
      }

      String wideAssociationSuffix = "";
      if(CollectionUtils.nullSafeHasContents(wideAssociationIndexes))
      {
         wideAssociationSuffix = "," + StringUtils.join(".", wideAssociationIndexes);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // loop over fields - finding what header name they are mapped to - then what index that header is at. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      String fieldNamePrefix = !StringUtils.hasContent(associationNameChain) ? "" : associationNameChain + ".";
      for(QFieldMetaData field : table.getFields().values())
      {
         String headerName = fieldNameToHeaderNameMap.get(fieldNamePrefix + field.getName() + wideAssociationSuffix);
         if(headerName != null)
         {
            Integer headerIndex = headerToIndexMap.get(headerName);
            if(headerIndex != null)
            {
               rs.put(field.getName(), headerIndex);
            }
         }
      }

      return (rs);
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
   public BulkInsertMapping withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
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
   public BulkInsertMapping withHasHeaderRow(Boolean hasHeaderRow)
   {
      this.hasHeaderRow = hasHeaderRow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldNameToHeaderNameMap
    *******************************************************************************/
   public Map<String, String> getFieldNameToHeaderNameMap()
   {
      return (this.fieldNameToHeaderNameMap);
   }



   /*******************************************************************************
    ** Setter for fieldNameToHeaderNameMap
    *******************************************************************************/
   public void setFieldNameToHeaderNameMap(Map<String, String> fieldNameToHeaderNameMap)
   {
      this.fieldNameToHeaderNameMap = fieldNameToHeaderNameMap;
   }



   /*******************************************************************************
    ** Fluent setter for fieldNameToHeaderNameMap
    *******************************************************************************/
   public BulkInsertMapping withFieldNameToHeaderNameMap(Map<String, String> fieldNameToHeaderNameMap)
   {
      this.fieldNameToHeaderNameMap = fieldNameToHeaderNameMap;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldNameToIndexMap
    *******************************************************************************/
   public Map<String, Integer> getFieldNameToIndexMap()
   {
      return (this.fieldNameToIndexMap);
   }



   /*******************************************************************************
    ** Setter for fieldNameToIndexMap
    *******************************************************************************/
   public void setFieldNameToIndexMap(Map<String, Integer> fieldNameToIndexMap)
   {
      this.fieldNameToIndexMap = fieldNameToIndexMap;
   }



   /*******************************************************************************
    ** Fluent setter for fieldNameToIndexMap
    *******************************************************************************/
   public BulkInsertMapping withFieldNameToIndexMap(Map<String, Integer> fieldNameToIndexMap)
   {
      this.fieldNameToIndexMap = fieldNameToIndexMap;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mappedAssociations
    *******************************************************************************/
   public List<String> getMappedAssociations()
   {
      return (this.mappedAssociations);
   }



   /*******************************************************************************
    ** Setter for mappedAssociations
    *******************************************************************************/
   public void setMappedAssociations(List<String> mappedAssociations)
   {
      this.mappedAssociations = mappedAssociations;
   }



   /*******************************************************************************
    ** Fluent setter for mappedAssociations
    *******************************************************************************/
   public BulkInsertMapping withMappedAssociations(List<String> mappedAssociations)
   {
      this.mappedAssociations = mappedAssociations;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldNameToDefaultValueMap
    *******************************************************************************/
   public Map<String, Serializable> getFieldNameToDefaultValueMap()
   {
      if(this.fieldNameToDefaultValueMap == null)
      {
         this.fieldNameToDefaultValueMap = new HashMap<>();
      }

      return (this.fieldNameToDefaultValueMap);
   }



   /*******************************************************************************
    ** Setter for fieldNameToDefaultValueMap
    *******************************************************************************/
   public void setFieldNameToDefaultValueMap(Map<String, Serializable> fieldNameToDefaultValueMap)
   {
      this.fieldNameToDefaultValueMap = fieldNameToDefaultValueMap;
   }



   /*******************************************************************************
    ** Fluent setter for fieldNameToDefaultValueMap
    *******************************************************************************/
   public BulkInsertMapping withFieldNameToDefaultValueMap(Map<String, Serializable> fieldNameToDefaultValueMap)
   {
      this.fieldNameToDefaultValueMap = fieldNameToDefaultValueMap;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldNameToValueMapping
    *******************************************************************************/
   public Map<String, Map<String, Serializable>> getFieldNameToValueMapping()
   {
      return (this.fieldNameToValueMapping);
   }



   /*******************************************************************************
    ** Setter for fieldNameToValueMapping
    *******************************************************************************/
   public void setFieldNameToValueMapping(Map<String, Map<String, Serializable>> fieldNameToValueMapping)
   {
      this.fieldNameToValueMapping = fieldNameToValueMapping;
   }



   /*******************************************************************************
    ** Fluent setter for fieldNameToValueMapping
    *******************************************************************************/
   public BulkInsertMapping withFieldNameToValueMapping(Map<String, Map<String, Serializable>> fieldNameToValueMapping)
   {
      this.fieldNameToValueMapping = fieldNameToValueMapping;
      return (this);
   }



   /*******************************************************************************
    ** Getter for layout
    *******************************************************************************/
   public Layout getLayout()
   {
      return (this.layout);
   }



   /*******************************************************************************
    ** Setter for layout
    *******************************************************************************/
   public void setLayout(Layout layout)
   {
      this.layout = layout;
   }



   /*******************************************************************************
    ** Fluent setter for layout
    *******************************************************************************/
   public BulkInsertMapping withLayout(Layout layout)
   {
      this.layout = layout;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tallLayoutGroupByIndexMap
    *******************************************************************************/
   public Map<String, List<Integer>> getTallLayoutGroupByIndexMap()
   {
      return (this.tallLayoutGroupByIndexMap);
   }



   /*******************************************************************************
    ** Setter for tallLayoutGroupByIndexMap
    *******************************************************************************/
   public void setTallLayoutGroupByIndexMap(Map<String, List<Integer>> tallLayoutGroupByIndexMap)
   {
      this.tallLayoutGroupByIndexMap = tallLayoutGroupByIndexMap;
   }



   /*******************************************************************************
    ** Fluent setter for tallLayoutGroupByIndexMap
    *******************************************************************************/
   public BulkInsertMapping withTallLayoutGroupByIndexMap(Map<String, List<Integer>> tallLayoutGroupByIndexMap)
   {
      this.tallLayoutGroupByIndexMap = tallLayoutGroupByIndexMap;
      return (this);
   }


}
