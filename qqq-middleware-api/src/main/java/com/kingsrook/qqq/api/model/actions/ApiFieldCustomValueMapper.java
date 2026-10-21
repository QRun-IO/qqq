/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.actions;


import java.io.Serializable;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public abstract class ApiFieldCustomValueMapper
{

   /*******************************************************************************
    ** When producing a JSON Object to send over the API (e.g., for a GET), this method
    ** can run to customize the value that is produced, for the input QRecord's specified
    ** fieldName
    *******************************************************************************/
   public Serializable produceApiValue(QRecord record, String apiFieldName)
   {
      /////////////////////
      // null by default //
      /////////////////////
      return (null);
   }



   /*******************************************************************************
    ** When producing a QRecord (the first parameter) from a JSON Object that was
    ** received from the API (e.g., a POST or PATCH) - this method can run to
    ** allow customization of the incoming value.
    *******************************************************************************/
   public void consumeApiValue(QRecord record, Object value, JSONObject fullApiJsonObject, String apiFieldName)
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void customizeFilterCriteriaForQueryOrCount(QueryOrCountInputInterface input, QQueryFilter filter, QFilterCriteria criteria, String apiFieldName, ApiFieldMetaData apiFieldMetaData)
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void customizeFilterOrderBy(QueryInput queryInput, QFilterOrderBy orderBy, String apiFieldName, ApiFieldMetaData apiFieldMetaData)
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

}
