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

package com.kingsrook.qqq.backend.core.model.metadata.serialization;


import java.io.IOException;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;


/*******************************************************************************
 ** Jackson custom deserialization class, to return an appropriate sub-type of
 ** QTableBackendDetails, based on the backendType of the containing table.
 *******************************************************************************/
public class QStepMetaDataDeserializer extends JsonDeserializer<QStepMetaData>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QStepMetaData deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
   {
      TreeNode treeNode = jsonParser.readValueAsTree();
      String   stepType = DeserializerUtils.readTextValue(treeNode, "stepType");
      Class<? extends QStepMetaData> targetClass = switch(stepType)
      {
         case "backend" -> QBackendStepMetaData.class;
         case "frontend" -> QFrontendStepMetaData.class;
         default -> throw new IllegalArgumentException("Unsupported StepType " + stepType + " for deserialization");
      };
      return (DeserializerUtils.reflectivelyDeserialize(targetClass, treeNode));
   }

}
