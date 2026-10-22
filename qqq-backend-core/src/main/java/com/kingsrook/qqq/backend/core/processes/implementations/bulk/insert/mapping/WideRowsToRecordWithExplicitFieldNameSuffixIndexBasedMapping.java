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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.FileToRowsInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** use a flatter mapping object, where field names look like:
 ** associationChain.fieldName,index.subIndex
 *******************************************************************************/
public class WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping implements RowsToRecordInterface
{
   private Memoization<Pair<String, String>, Boolean> shouldProcesssAssociationMemoization = new Memoization<>();



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<QRecord> nextPage(FileToRowsInterface fileToRowsInterface, BulkLoadFileRow headerRow, BulkInsertMapping mapping, Integer limit) throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(mapping.getTableName());
      if(table == null)
      {
         throw (new QException("Table [" + mapping.getTableName() + "] was not found in the Instance"));
      }

      List<QRecord> rs = new ArrayList<>();

      Map<String, Integer> fieldIndexes = mapping.getFieldIndexes(table, null, headerRow);

      while(fileToRowsInterface.hasNext() && rs.size() < limit)
      {
         BulkLoadFileRow row    = fileToRowsInterface.next();
         QRecord         record = makeRecordFromRow(mapping, table, "", row, fieldIndexes, headerRow, new ArrayList<>(), false);
         rs.add(record);
      }

      BulkLoadValueMapper.valueMapping(rs, mapping, table);

      return (rs);
   }



   /***************************************************************************
    ** may return null, if there were no values in the row for this (sub-wide) record.
    ** more specifically:
    **
    ** the param `rowOfOnlyDefaultValues` - should be false for the header table,
    ** and true for an association iff all mapped fields are using 'default values'
    ** (e.g., not values from the file).
    **
    ** So this method will return null, indicating "no child row to build" if:
    ** - when doing a rowOfOnlyDefaultValues - only if there actually weren't any
    ** default values, which, probably never happens!
    ** - else (doing a row with at least 1 value from the file) - then, null is
    ** returned if there were NO values from the file.
    **
    ** The goal here is to support these cases:
    **
    ** Case A (a row of not only-default-values):
    ** - lineItem.sku,0 = column: sku1
    ** - lineItem.qty,0 = column: qty1
    ** - lineItem.lineNo,0 = Default: 1
    ** - lineItem.sku,1 = column: sku2
    ** - lineItem.qty,1 = column: qty2
    ** - lineItem.lineNo,1 = Default: 2
    ** then a file row with no values for sku2 & qty2 - we don't want a row
    ** in that case (which would only have the default value of lineNo=2)
    **
    ** Case B (a row of only-default-values):
    ** - lineItem.sku,0 = column: sku1
    ** - lineItem.qty,0 = column: qty1
    ** - lineItem.lineNo,0 = Default: 1
    ** - lineItem.sku,1 = Default: SUPPLEMENT
    ** - lineItem.qty,1 = Default: 1
    ** - lineItem.lineNo,1 = Default: 2
    ** we want every parent (order) to include a 2nd line item - with 3
    ** default values (sku=SUPPLEMENT, qty=q, lineNo=2).
    **
    ***************************************************************************/
   private QRecord makeRecordFromRow(BulkInsertMapping mapping, QTableMetaData table, String associationNameChain, BulkLoadFileRow row, Map<String, Integer> fieldIndexes, BulkLoadFileRow headerRow, List<Integer> wideAssociationIndexes, boolean rowOfOnlyDefaultValues) throws QException
   {
      //////////////////////////////////////////////////////
      // start by building the record with its own fields //
      //////////////////////////////////////////////////////
      QRecord record = new QRecord();
      BulkLoadRecordUtils.addBackendDetailsAboutFileRows(record, row);

      boolean hadAnyValuesInRowFromFile = false;
      boolean hadAnyValuesInRow = false;
      for(QFieldMetaData field : table.getFields().values())
      {
         hadAnyValuesInRowFromFile = setValueOrDefault(record, field, associationNameChain, mapping, row, fieldIndexes.get(field.getName()), wideAssociationIndexes) || hadAnyValuesInRowFromFile;

         /////////////////////////////////////////////////////////////////////////////////////
         // for wide mode (different from tall) - allow a row that only has default values. //
         // e.g., Line Item (2) might be a default to add to every order                    //
         /////////////////////////////////////////////////////////////////////////////////////
         if(record.getValue(field.getName()) != null)
         {
            hadAnyValuesInRow = true;
         }
      }

      if(rowOfOnlyDefaultValues)
      {
         if(!hadAnyValuesInRow)
         {
            return (null);
         }
      }
      else
      {
         if(!hadAnyValuesInRowFromFile)
         {
            return (null);
         }
      }

      /////////////////////////////
      // associations (children) //
      /////////////////////////////
      for(String associationName : CollectionUtils.nonNullList(mapping.getMappedAssociations()))
      {
         boolean processAssociation = shouldProcessAssociation(associationNameChain, associationName);

         if(processAssociation)
         {
            String associationNameMinusChain = StringUtils.hasContent(associationNameChain)
               ? associationName.substring(associationNameChain.length() + 1)
               : associationName;

            Optional<Association> association = table.getAssociations().stream().filter(a -> a.getName().equals(associationNameMinusChain)).findFirst();
            if(association.isEmpty())
            {
               throw (new QException("Couldn't find association: " + associationNameMinusChain + " under table: " + table.getName()));
            }

            QTableMetaData associatedTable = QContext.getQInstance().getTable(association.get().getAssociatedTableName());

            List<QRecord> associatedRecords = processAssociation(associationNameMinusChain, associationNameChain, associatedTable, mapping, row, headerRow);
            record.withAssociatedRecords(associationNameMinusChain, associatedRecords);
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // stash the wide-association indexes in records, so that in the value mapper, we know if if this is, e.g., ,1, or ,2.3, for value-mapping //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(wideAssociationIndexes))
      {
         ArrayList<Integer> indexesArrayList = CollectionUtils.useOrWrap(wideAssociationIndexes, new TypeToken<>() {});
         record.addBackendDetail("wideAssociationIndexes", indexesArrayList);
      }

      return record;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<QRecord> processAssociation(String associationName, String associationNameChain, QTableMetaData associatedTable, BulkInsertMapping mapping, BulkLoadFileRow row, BulkLoadFileRow headerRow) throws QException
   {
      List<QRecord> rs = new ArrayList<>();

      String associationNameChainForRecursiveCalls = "".equals(associationNameChain) ? associationName : associationNameChain + "." + associationName;

      for(int i = 0; true; i++)
      {
         // todo - doesn't support grand-children
         List<Integer>        wideAssociationIndexes = List.of(i);
         Map<String, Integer> fieldIndexes           = mapping.getFieldIndexes(associatedTable, associationNameChainForRecursiveCalls, headerRow, wideAssociationIndexes);

         boolean rowOfOnlyDefaultValues = false;
         if(fieldIndexes.isEmpty())
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if there aren't any field-indexes for this (i) value (e.g., no columns mapped for Line Item: X (2)), we can still build a //
            // child record here if there are any default values - so check for them - and only if they are empty, then break the loop.  //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Map<String, Serializable> fieldDefaultValues = mapping.getFieldDefaultValues(associatedTable, associationNameChainForRecursiveCalls, wideAssociationIndexes);
            if(!CollectionUtils.nullSafeHasContents(fieldDefaultValues))
            {
               break;
            }
            rowOfOnlyDefaultValues = true;
         }

         QRecord record = makeRecordFromRow(mapping, associatedTable, associationNameChainForRecursiveCalls, row, fieldIndexes, headerRow, wideAssociationIndexes, rowOfOnlyDefaultValues);
         if(record != null)
         {
            rs.add(record);
         }
      }

      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   boolean shouldProcessAssociation(String associationNameChain, String associationName)
   {
      return shouldProcesssAssociationMemoization.getResult(Pair.of(associationNameChain, associationName), p ->
      {
         List<String> chainParts = new ArrayList<>();
         List<String> nameParts  = new ArrayList<>();

         if(StringUtils.hasContent(associationNameChain))
         {
            chainParts.addAll(Arrays.asList(associationNameChain.split("\\.")));
         }

         if(StringUtils.hasContent(associationName))
         {
            nameParts.addAll(Arrays.asList(associationName.split("\\.")));
         }

         if(!nameParts.isEmpty())
         {
            nameParts.remove(nameParts.size() - 1);
         }

         return (chainParts.equals(nameParts));
      }).orElse(false);
   }

}