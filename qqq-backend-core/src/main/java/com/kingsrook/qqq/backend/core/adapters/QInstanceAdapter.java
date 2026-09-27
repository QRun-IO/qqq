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

package com.kingsrook.qqq.backend.core.adapters;


import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.json.JSONObject;


/*******************************************************************************
 ** Methods for adapting qInstances to serialized (string) formats (e.g., json),
 ** and vice versa.
 *******************************************************************************/
public class QInstanceAdapter
{

   /*******************************************************************************
    ** Convert a qInstance to JSON.
    **
    *******************************************************************************/
   public String qInstanceToJson(QInstance qInstance)
   {
      return (JsonUtils.toJson(qInstance));
   }



   /*******************************************************************************
    ** Convert a qInstance to JSON.
    **
    *******************************************************************************/
   public String qInstanceToJsonIncludingBackend(QInstance qInstance)
   {
      String jsonString = JsonUtils.toJson(qInstance);
      JSONObject jsonObject = JsonUtils.toJSONObject(jsonString);

      String backendsJsonString = JsonUtils.toJson(qInstance.getBackends());
      JSONObject backendsJsonObject = JsonUtils.toJSONObject(backendsJsonString);
      jsonObject.put("backends", backendsJsonObject);

      return (jsonObject.toString());
   }



   /*******************************************************************************
    ** Build a qInstance from JSON.
    **
    *******************************************************************************/
   public QInstance jsonToQInstance(String json) throws IOException
   {
      return (JsonUtils.toObject(json, QInstance.class));
   }



   /*******************************************************************************
    ** Build a qInstance from JSON.
    **
    *******************************************************************************/
   public QInstance jsonToQInstanceIncludingBackends(String json) throws IOException
   {
      QInstance qInstance = JsonUtils.toObject(json, QInstance.class);
      JSONObject jsonObject = JsonUtils.toJSONObject(json);
      JSONObject backendsJsonObject = jsonObject.getJSONObject("backends");
      Map<String, QBackendMetaData> backends = JsonUtils.toObject(backendsJsonObject.toString(), new TypeReference<>()
      {
      });
      qInstance.setBackends(backends);
      return qInstance;
   }

}
