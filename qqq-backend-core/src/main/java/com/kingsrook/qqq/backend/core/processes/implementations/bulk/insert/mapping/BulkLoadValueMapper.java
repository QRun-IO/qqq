/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.CaseInsensitiveKeyMap;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkLoadValueMapper
{
   private static final QLogger LOG = QLogger.getLogger(BulkLoadValueMapper.class);



   /***************************************************************************
    **
    ***************************************************************************/
   public static void valueMapping(List<QRecord> records, BulkInsertMapping mapping, QTableMetaData table) throws QException
   {
      valueMapping(records, mapping, table, null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void valueMapping(List<QRecord> records, BulkInsertMapping mapping, QTableMetaData table, String associationNameChain) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(records))
      {
         return;
      }

      String associationNamePrefixForFields = StringUtils.hasContent(associationNameChain) ? associationNameChain + "." : "";
      String tableLabelPrefix               = StringUtils.hasContent(associationNameChain) ? table.getLabel() + ": " : "";

      Map<String, ListingHash<String, QRecord>> possibleValueToRecordMap = new HashMap<>();

      Map<String, Map<String, Serializable>> mappingForTable = mapping.getFieldNameToValueMappingForTable(associationNameChain);
      for(QRecord record : records)
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if we remove in the loop, we get ConcurrentModificationException, so track which ones to remove //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         Set<String> toRemove = new HashSet<>();

         for(Map.Entry<String, Serializable> valueEntry : record.getValues().entrySet())
         {
            QFieldMetaData field = table.getField(valueEntry.getKey());
            Serializable   value = valueEntry.getValue();

            String fieldNamePlusWideIndex = field.getName();
            if(record.getBackendDetail("wideAssociationIndexes") != null)
            {
               ArrayList<Integer> indexes = (ArrayList<Integer>) record.getBackendDetail("wideAssociationIndexes");
               fieldNamePlusWideIndex += "," + StringUtils.join(",", indexes);
            }

            ///////////////////
            // value mappin' //
            ///////////////////
            if(mappingForTable.containsKey(fieldNamePlusWideIndex) && value != null)
            {
               Serializable mappedValue = mappingForTable.get(fieldNamePlusWideIndex).get(ValueUtils.getValueAsString(value));
               if(mappedValue != null)
               {
                  value = mappedValue;
               }
            }

            /////////////////////
            // type convertin' //
            /////////////////////
            if(value != null && !"".equals(value))
            {
               if(StringUtils.hasContent(field.getPossibleValueSourceName()))
               {
                  ListingHash<String, QRecord> fieldPossibleValueToRecordMap = possibleValueToRecordMap.computeIfAbsent(field.getName(), k -> new ListingHash<>());
                  fieldPossibleValueToRecordMap.add(ValueUtils.getValueAsString(value), record);
               }
               else
               {
                  QFieldType type = field.getType();
                  try
                  {
                     value = ValueUtils.getValueAsFieldType(type, value);
                     record.setValue(field.getName(), value);
                  }
                  catch(Exception e)
                  {
                     record.addError(new BulkLoadValueTypeError(associationNamePrefixForFields + field.getName(), value, type, tableLabelPrefix + field.getLabel()));
                     toRemove.add(field.getName());
                  }
               }
            }
         }

         //////////////////////////////////////////////////////////////////////////////////////////
         // remove any field values that had an error.                                           //
         // otherwise there can be downstream issues (e.g., if say some customizer code tries to //
         // build an entity out of a record, and there's a temporal value that can't be parsed.  //
         //////////////////////////////////////////////////////////////////////////////////////////
         toRemove.forEach(record::removeValue);

         //////////////////////////////////////
         // recursively process associations //
         //////////////////////////////////////
         for(Map.Entry<String, List<QRecord>> entry : record.getAssociatedRecords().entrySet())
         {
            String                associationName = entry.getKey();
            Optional<Association> association     = table.getAssociations().stream().filter(a -> a.getName().equals(associationName)).findFirst();
            if(association.isPresent())
            {
               QTableMetaData associatedTable = QContext.getQInstance().getTable(association.get().getAssociatedTableName());
               valueMapping(entry.getValue(), mapping, associatedTable, StringUtils.hasContent(associationNameChain) ? associationNameChain + "." + associationName : associationName);
            }
            else
            {
               throw new QException("Missing association [" + associationName + "] on table [" + table.getName() + "]");
            }
         }
      }

      //////////////////////////////////////////
      // look up and validate possible values //
      //////////////////////////////////////////
      for(Map.Entry<String, ListingHash<String, QRecord>> entry : possibleValueToRecordMap.entrySet())
      {
         String                       fieldName                     = entry.getKey();
         QFieldMetaData               field                         = table.getField(fieldName);
         ListingHash<String, QRecord> fieldPossibleValueToRecordMap = possibleValueToRecordMap.get(fieldName);

         handlePossibleValues(field, fieldPossibleValueToRecordMap, associationNamePrefixForFields, tableLabelPrefix);
      }
   }



   /***************************************************************************
    ** Given a listingHash of Strings from the bulk-load file, to QRecords,
    ** we will either:
    ** - make sure the value set for the field is valid in the PV type
    ** - or put an error in the record (leaving the original value from the file in the field)
    **
    ** We'll do potentially 2 possible-value searches - the first "by id" -
    ** type-converting the input strings to the PV's id type.  Then, if any
    ** values weren't found by id, a second search by "labels"... which might
    ** be a bit suspicious, e.g., if the PV has a multi-field label...
    ***************************************************************************/
   private static void handlePossibleValues(QFieldMetaData field, ListingHash<String, QRecord> fieldPossibleValueToRecordMap, String associationNamePrefixForFields, String tableLabelPrefix) throws QException
   {
      QPossibleValueSource possibleValueSource = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());

      ////////////////////////////////////////////////////////////
      // String from file -> List<QRecord> that have that value //
      ////////////////////////////////////////////////////////////
      Set<String> values = fieldPossibleValueToRecordMap.keySet();

      /////////////////////////////////////////////////////////////////////////////////
      // String from file -> Integer (for example) - that is the type-converted      //
      // version of the PVS's idType (but before any lookups were done with that id) //
      // e.g., "42" -> 42                                                            //
      // e.g., "SOME_CONST" -> "SOME_CONST" (for PVS w/ string ids)                  //
      /////////////////////////////////////////////////////////////////////////////////
      Map<String, Serializable> valuesToValueInPvsIdTypeMap = new HashMap<>();

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // String versions of EITHER ids or values found in searchPossibleValueSource call (depending on what was searched by)                       //
      // e.g., "42" -> QPossibleValue(42, "Forty-two") (when searched by id)                                                                       //
      // e.g., "Forty-Two" -> QPossibleValue(42, "Forty-two") (when searched by label)                                                             //
      // e.g., "SOME_CONST" -> QPossibleValue("SOME_CONST", "Some Const") (when searched by id)                                                    //
      // e.g., "Some Const" -> QPossibleValue("SOME_CONST", "Some Const") (when searched by label)                                                 //
      // goal being - file could have "42" or "Forty-Two" (or "forty two") and those would all map to QPossibleValue(42, "Forty-two")              //
      // or - file could have "SOME_CONST" or "Some Const" (or "some const") and those would all map to QPossibleValue("SOME_CONST", "Some Const") //
      // this is also why using CaseInsensitiveKeyMap!                                                                                             //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      CaseInsensitiveKeyMap<QPossibleValue<?>> valuesFoundAsStrings = new CaseInsensitiveKeyMap<>();

      /////////////////////////////////////////////////////////
      // String values (from file) that still need looked up //
      /////////////////////////////////////////////////////////
      Set<String> valuesNotFound = new HashSet<>();

      ////////////////////////////////////////////////////////
      // do a search, trying to use all given values as ids //
      ////////////////////////////////////////////////////////
      ArrayList<Serializable> idList = new ArrayList<>();
      SearchPossibleValueSourceInput searchPossibleValueSourceInput = new SearchPossibleValueSourceInput();
      searchPossibleValueSourceInput.setPossibleValueSourceName(field.getPossibleValueSourceName());

      for(String value : values)
      {
         valuesNotFound.add(value);
         Serializable valueInPvsIdType = value;

         try
         {
            valueInPvsIdType = ValueUtils.getValueAsFieldType(possibleValueSource.getIdType(), value);
            valuesToValueInPvsIdTypeMap.put(value, valueInPvsIdType);
            idList.add(valueInPvsIdType);
         }
         catch(Exception e)
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // If the value can't be made into the id's type (probably not an integer), then don't try to look it up as an id. //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // todo - we should probably be doing a lot of what QJavalinImplementation.finishPossibleValuesRequest does here //
      //  to apply possible-value filters.  difficult to pass values in, but needed...                                 //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there are any ids (e.g., that could be type-converted from the file's values to the PVS's id type), then search by id. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(!idList.isEmpty())
      {
         searchPossibleValueSourceInput.setIdList(idList);
         searchPossibleValueSourceInput.setLimit(values.size());
         LOG.debug("Searching possible value source by ids during bulk load mapping", logPair("pvsName", field.getPossibleValueSourceName()), logPair("noOfIds", idList.size()), logPair("firstId", () -> idList.get(0)));
         SearchPossibleValueSourceOutput searchPossibleValueSourceOutput = idList.isEmpty() ? new SearchPossibleValueSourceOutput() : new SearchPossibleValueSourceAction().execute(searchPossibleValueSourceInput);

         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // for each possible value found, store it as a hit, and remove it from the set of ones not-found //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         for(QPossibleValue<?> possibleValue : searchPossibleValueSourceOutput.getResults())
         {
            String valueAsString = ValueUtils.getValueAsString(possibleValue.getId());
            valuesFoundAsStrings.put(valueAsString, possibleValue);
            valuesNotFound.remove(valueAsString);
         }
      }

      ///////////////////////////////////////////////////////////////////////////
      // if there are any that weren't found, try to look them up now by label //
      ///////////////////////////////////////////////////////////////////////////
      if(!valuesNotFound.isEmpty())
      {
         searchPossibleValueSourceInput = new SearchPossibleValueSourceInput();
         searchPossibleValueSourceInput.setPossibleValueSourceName(field.getPossibleValueSourceName());
         searchPossibleValueSourceInput.setLabelList(new ArrayList<>(valuesNotFound));
         searchPossibleValueSourceInput.setLimit(valuesNotFound.size() * 10); // todo - a little sus... leaves some room for dupes, which, can they happen?

         LOG.debug("Searching possible value source by labels during bulk load mapping", logPair("pvsName", field.getPossibleValueSourceName()), logPair("noOfLabels", valuesNotFound.size()), logPair("firstLabel", () -> valuesNotFound.iterator().next()));
         SearchPossibleValueSourceOutput searchPossibleValueSourceOutput = new SearchPossibleValueSourceAction().execute(searchPossibleValueSourceInput);
         for(QPossibleValue<?> possibleValue : searchPossibleValueSourceOutput.getResults())
         {
            // todo - deal with multiple values found - and maybe... if some end up not-found, but some dupes happened, should we try another search, in case we hit the limit?
            valuesFoundAsStrings.put(possibleValue.getLabel(), possibleValue);
            valuesNotFound.remove(possibleValue.getLabel());
         }
      }

      ////////////////////////////////////////////////////////////////////////////////
      // for each record now, either set a usable value (e.g., a PV.id) or an error //
      ////////////////////////////////////////////////////////////////////////////////
      for(Map.Entry<String, List<QRecord>> entry : fieldPossibleValueToRecordMap.entrySet())
      {
         String       value            = entry.getKey();
         Serializable valueInPvsIdType = valuesToValueInPvsIdTypeMap.get(entry.getKey());
         String       pvsIdAsString    = ValueUtils.getValueAsString(valueInPvsIdType);

         for(QRecord record : entry.getValue())
         {
            if(valuesFoundAsStrings.containsKey(pvsIdAsString))
            {
               record.setValue(field.getName(), valuesFoundAsStrings.get(pvsIdAsString).getId());
            }
            else if(valuesFoundAsStrings.containsKey(value))
            {
               record.setValue(field.getName(), valuesFoundAsStrings.get(value).getId());
            }
            else
            {
               record.addError(new BulkLoadPossibleValueError(associationNamePrefixForFields + field.getName(), value, tableLabelPrefix + field.getLabel()));
               record.removeValue(field.getName());
            }
         }
      }
   }

}
