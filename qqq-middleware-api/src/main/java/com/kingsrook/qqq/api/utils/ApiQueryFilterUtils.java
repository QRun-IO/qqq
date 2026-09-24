/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.utils;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utilities for working with Query Filters in the API (e.g., versioned field fun)
 *******************************************************************************/
public class ApiQueryFilterUtils
{
   private static final QLogger LOG = QLogger.getLogger(ApiQueryFilterUtils.class);



   /***************************************************************************
    **
    ***************************************************************************/
   public static void manageCriteriaFields(QQueryFilter filter, Map<String, QFieldMetaData> tableApiFields, List<String> badRequestMessages, String apiName, String apiVersion, QueryOrCountInputInterface input) throws QException
   {
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         String         apiFieldName = criteria.getFieldName();
         QFieldMetaData field        = tableApiFields.get(apiFieldName);

         String joinTableName = null;
         if(apiFieldName.contains("."))
         {
            if(apiVersion == null)
            {
               LOG.warn("No apiVersion provided for manageCriteriaFields.  Cannot process join criteria field", logPair("fieldName", apiFieldName));
               badRequestMessages.add("Cannot process joined criteria field: " + apiFieldName);
               continue;
            }

            try
            {
               String[] split = apiFieldName.split("\\.", 2);
               joinTableName = split[0];
               String joinFieldName = split[1];

               Map<String, QFieldMetaData> joinTableApiFields = GetTableApiFieldsAction.getTableApiFieldMap(new GetTableApiFieldsInput().withApiName(apiName).withVersion(apiVersion).withTableName(joinTableName).withInputSource(input.getInputSource()));
               field = joinTableApiFields.get(joinFieldName);
            }
            catch(Exception e)
            {
               badRequestMessages.add("Error processing criteria field: " + apiFieldName + ": " + e.getMessage());
               continue;
            }
         }

         if(field == null)
         {
            badRequestMessages.add("Unrecognized criteria field name: " + apiFieldName + ".");
         }
         else
         {
            try
            {
               QFieldMetaData   finalField       = field;
               ApiFieldMetaData apiFieldMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(finalField).getApiFieldMetaData(apiName), new ApiFieldMetaData());
               if(StringUtils.hasContent(apiFieldMetaData.getReplacedByFieldName()))
               {
                  String joinTablePrefix = joinTableName == null ? "" : (joinTableName + ".");
                  criteria.setFieldName(joinTablePrefix + apiFieldMetaData.getReplacedByFieldName());
               }
               else if(apiFieldMetaData.getCustomValueMapper() != null)
               {
                  ApiFieldCustomValueMapper customValueMapper = QCodeLoader.getAdHoc(ApiFieldCustomValueMapper.class, apiFieldMetaData.getCustomValueMapper());
                  customValueMapper.customizeFilterCriteriaForQueryOrCount(input, filter, criteria, apiFieldName, apiFieldMetaData);
               }
            }
            catch(Exception e)
            {
               badRequestMessages.add("Error processing criteria field " + apiFieldName + ": " + e.getMessage());
            }
         }
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static void throwIfBadRequestMessages(List<String> badRequestMessages) throws QBadRequestException
   {
      if(!badRequestMessages.isEmpty())
      {
         if(badRequestMessages.size() == 1)
         {
            throw (new QBadRequestException(badRequestMessages.get(0)));
         }
         else
         {
            throw (new QBadRequestException("Request failed with " + badRequestMessages.size() + " reasons: " + StringUtils.join("\n", badRequestMessages)));
         }
      }
   }

}
