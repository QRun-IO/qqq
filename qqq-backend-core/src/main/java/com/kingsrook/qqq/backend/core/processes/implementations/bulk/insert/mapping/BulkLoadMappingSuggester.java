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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadTableStructure;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;


/*******************************************************************************
 ** Given a bulk-upload, create a suggested mapping
 *******************************************************************************/
public class BulkLoadMappingSuggester
{
   private Map<String, Integer> massagedHeadersWithoutNumbersToIndexMap;
   private Map<String, Integer> massagedHeadersWithNumbersToIndexMap;

   private String layout = "FLAT";



   /***************************************************************************
    **
    ***************************************************************************/
   public BulkLoadProfile suggestBulkLoadMappingProfile(BulkLoadTableStructure tableStructure, List<String> headerRow, boolean isBulkEdit)
   {
      massagedHeadersWithoutNumbersToIndexMap = new LinkedHashMap<>();
      for(int i = 0; i < headerRow.size(); i++)
      {
         String headerValue = massageHeader(headerRow.get(i), true);

         if(!massagedHeadersWithoutNumbersToIndexMap.containsKey(headerValue))
         {
            massagedHeadersWithoutNumbersToIndexMap.put(headerValue, i);
         }
      }

      massagedHeadersWithNumbersToIndexMap = new LinkedHashMap<>();
      for(int i = 0; i < headerRow.size(); i++)
      {
         String headerValue = massageHeader(headerRow.get(i), false);

         if(!massagedHeadersWithNumbersToIndexMap.containsKey(headerValue))
         {
            massagedHeadersWithNumbersToIndexMap.put(headerValue, i);
         }
      }

      ArrayList<BulkLoadProfileField> fieldList = new ArrayList<>();
      processTable(tableStructure, fieldList, headerRow);

      /////////////////////////////////////////////////
      // sort the fields to match the column indexes //
      /////////////////////////////////////////////////
      fieldList.sort(Comparator.comparing(blpf -> blpf.getColumnIndex()));

      BulkLoadProfile bulkLoadProfile = new BulkLoadProfile()
         .withVersion("v1")
         .withLayout(layout)
         .withHasHeaderRow(true)
         .withIsBulkEdit(isBulkEdit)
         .withFieldList(fieldList);

      return (bulkLoadProfile);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void processTable(BulkLoadTableStructure tableStructure, ArrayList<BulkLoadProfileField> fieldList, List<String> headerRow)
   {
      Map<String, Integer> rs = new HashMap<>();
      for(QFieldMetaData field : tableStructure.getFields())
      {
         String fieldName           = massageHeader(field.getName(), false);
         String fieldLabel          = massageHeader(field.getLabel(), false);
         String tablePlusFieldLabel = massageHeader(QContext.getQInstance().getTable(tableStructure.getTableName()).getLabel() + ": " + field.getLabel(), false);
         String fullFieldName       = (StringUtils.hasContent(tableStructure.getAssociationPath()) ? (tableStructure.getAssociationPath() + ".") : "") + field.getName();

         ////////////////////////////////////////////////////////////////////////////////////
         // consider, if this is a many-table, if there are many matches, for wide mode... //
         ////////////////////////////////////////////////////////////////////////////////////
         if(tableStructure.getIsMany())
         {
            List<Integer> matchingIndexes = new ArrayList<>();

            for(Map.Entry<String, Integer> entry : massagedHeadersWithNumbersToIndexMap.entrySet())
            {
               String header = entry.getKey();
               if(header.matches(fieldName + "\\d*$") || header.matches(fieldLabel + "\\d*$"))
               {
                  matchingIndexes.add(entry.getValue());
               }
            }

            if(CollectionUtils.nullSafeHasContents(matchingIndexes))
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               // if we found more than 1 match - consider this a likely wide file, and build fields as wide-fields //
               // else, if only 1, allow us to go down into the TALL block below                                    //
               // note - should we do a merger at the end, in case we found some wide, some tall?                   //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               if(matchingIndexes.size() > 1)
               {
                  layout = "WIDE";

                  int i = 0;
                  for(Integer index : matchingIndexes)
                  {
                     fieldList.add(new BulkLoadProfileField()
                        .withFieldName(fullFieldName + "," + i)
                        .withHeaderName(headerRow.get(index))
                        .withColumnIndex(index)
                     );

                     i++;
                  }

                  continue;
               }
            }
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // else - look for matches, first w/ headers with numbers, then headers w/o numbers checking labels and names //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         Integer index = null;

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // for each of these potential identities of the field:                                                                   //
         // 1) its label, massaged                                                                                                 //
         // 2) its name, massaged                                                                                                  //
         // 3) its label, massaged, with numbers stripped away                                                                     //
         // 4) its name, massaged, with numbers stripped away                                                                      //
         // check if that identity is in the massagedHeadersWithNumbersToIndexMap, or the massagedHeadersWithoutNumbersToIndexMap. //
         // this is currently successful in the both versions of the address 1 / address 2 <=> address / address 2 use-case        //
         // that is, BulkLoadMappingSuggesterTest.testChallengingAddress1And2                                                      //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         for(String fieldIdentity : ListBuilder.of(fieldLabel, fieldName, tablePlusFieldLabel, massageHeader(fieldLabel, true), massageHeader(fieldName, true)))
         {
            if(massagedHeadersWithNumbersToIndexMap.containsKey(fieldIdentity))
            {
               index = massagedHeadersWithNumbersToIndexMap.get(fieldIdentity);
            }
            else if(massagedHeadersWithoutNumbersToIndexMap.containsKey(fieldIdentity))
            {
               index = massagedHeadersWithoutNumbersToIndexMap.get(fieldIdentity);
            }

            if(index != null)
            {
               break;
            }
         }

         if(index != null)
         {
            fieldList.add(new BulkLoadProfileField()
               .withFieldName(fullFieldName)
               .withHeaderName(headerRow.get(index))
               .withColumnIndex(index)
            );

            if(tableStructure.getIsMany() && layout.equals("FLAT"))
            {
               //////////////////////////////////////////////////////////////////////////////////////////
               // the first time we find an is-many child, if we were still marked as flat, go to tall //
               //////////////////////////////////////////////////////////////////////////////////////////
               layout = "TALL";
            }
         }
      }

      ////////////////////////////////////////////
      // recursively process child associations //
      ////////////////////////////////////////////
      for(BulkLoadTableStructure associationTableStructure : CollectionUtils.nonNullList(tableStructure.getAssociations()))
      {
         processTable(associationTableStructure, fieldList, headerRow);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String massageHeader(String header, boolean stripNumbers)
   {
      if(header == null)
      {
         return (null);
      }

      String massagedWithNumbers = header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
      return stripNumbers ? massagedWithNumbers.replaceAll("[0-9]", "") : massagedWithNumbers;
   }

}
