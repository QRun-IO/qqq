/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.api;


import java.io.IOException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;


/*******************************************************************************
 ** Utility methods for working with EasyPost API
 *******************************************************************************/
public class EasyPostUtils extends BaseAPIActionUtil
{
   private static final QLogger LOG = QLogger.getLogger(EasyPostUtils.class);



   /*******************************************************************************
    ** Build an HTTP Entity (e.g., for a PUT or POST) from a QRecord.  Can be
    ** overridden if an API doesn't do a basic json object.  Or, can override a
    ** helper method, such as recordToJsonObject.
    **
    *******************************************************************************/
   @Override
   protected AbstractHttpEntity recordToEntity(QTableMetaData table, QRecord record) throws IOException
   {
      JSONObject body      = recordToJsonObject(table, record);
      JSONObject wrapper   = new JSONObject();
      String     tablePath = getBackendDetails(table).getTableWrapperObjectName();
      wrapper.put(tablePath, body);
      String json = wrapper.toString();
      LOG.debug(json);
      return (new StringEntity(json));
   }

}
