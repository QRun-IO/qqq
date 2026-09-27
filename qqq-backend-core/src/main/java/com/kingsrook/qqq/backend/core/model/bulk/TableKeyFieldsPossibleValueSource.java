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

package com.kingsrook.qqq.backend.core.model.bulk;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableKeyFieldsPossibleValueSource implements QCustomPossibleValueProvider<String>
{
   public static final String NAME = "tableKeyFields";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QPossibleValue<String> getPossibleValue(Serializable tableAndKey)
   {
      QPossibleValue<String> possibleValue = null;

      /////////////////////////////////////////////////////////////
      // keys are in the format <tableName>-<key1>|<key2>|<key3> //
      /////////////////////////////////////////////////////////////
      String[] keyParts  = tableAndKey.toString().split("-");
      String   tableName = keyParts[0];
      String   key       = keyParts[1];

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table.getPrimaryKeyField().equals(key))
      {
         String id    = table.getPrimaryKeyField();
         String label = table.getField(table.getPrimaryKeyField()).getLabel();
         possibleValue = new QPossibleValue<>(id, label);
      }
      else
      {
         for(UniqueKey uniqueKey : table.getUniqueKeys())
         {
            String potentialMatch = getIdFromUniqueKey(uniqueKey);
            if(potentialMatch.equals(key))
            {
               String id    = potentialMatch;
               String label = getLabelFromUniqueKey(table, uniqueKey);
               possibleValue = new QPossibleValue<>(id, label);
               break;
            }
         }
      }

      return (possibleValue);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QPossibleValue<String>> search(SearchPossibleValueSourceInput input) throws QException
   {
      List<QPossibleValue<String>> rs = new ArrayList<>();
      if(!CollectionUtils.nonNullMap(input.getPathParamMap()).containsKey("processName") || input.getPathParamMap().get("processName") == null || input.getPathParamMap().get("processName").isEmpty())
      {
         throw (new QException("Path Param of processName was not found."));
      }

      ////////////////////////////////////////////////////
      // process name will be like tnt.bulkEditWithFile //
      ////////////////////////////////////////////////////
      String processName = input.getPathParamMap().get("processName");
      String tableName   = processName.split("\\.")[0];

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      for(UniqueKey uniqueKey : CollectionUtils.nonNullList(table.getUniqueKeys()))
      {
         String id    = getIdFromUniqueKey(uniqueKey);
         String label = getLabelFromUniqueKey(table, uniqueKey);
         if(!StringUtils.hasContent(input.getSearchTerm()) || input.getSearchTerm().equals(id))
         {
            rs.add(new QPossibleValue<>(id, label));
         }
      }
      rs.sort(Comparator.comparing(QPossibleValue::getLabel));

      ///////////////////////////////
      // put the primary key first //
      ///////////////////////////////
      if(!StringUtils.hasContent(input.getSearchTerm()) || input.getSearchTerm().equals(table.getPrimaryKeyField()))
      {
         rs.add(0, new QPossibleValue<>(table.getPrimaryKeyField(), table.getField(table.getPrimaryKeyField()).getLabel()));
      }

      return rs;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getIdFromUniqueKey(UniqueKey uniqueKey)
   {
      return (StringUtils.join("|", uniqueKey.getFieldNames()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getLabelFromUniqueKey(QTableMetaData tableMetaData, UniqueKey uniqueKey)
   {
      List<String> fieldLabels = new ArrayList<>(uniqueKey.getFieldNames().stream().map(f -> tableMetaData.getField(f).getLabel()).toList());
      fieldLabels.sort(Comparator.naturalOrder());
      return (StringUtils.joinWithCommasAndAnd(fieldLabels));
   }
}
