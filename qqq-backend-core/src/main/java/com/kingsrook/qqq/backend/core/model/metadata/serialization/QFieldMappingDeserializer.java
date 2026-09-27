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
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.AbstractQFieldMapping;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 ** Jackson custom deserialization class, to return an appropriate sub-type of
 ** A QBackendMetaData, based on the backendType specified within.
 *******************************************************************************/
public class QFieldMappingDeserializer extends JsonDeserializer<AbstractQFieldMapping>
{
   @Override
   public AbstractQFieldMapping deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
   {
      TreeNode treeNode = jsonParser.readValueAsTree();

      TreeNode sourceTypeTreeNode = treeNode.get("sourceType");
      if(sourceTypeTreeNode == null || sourceTypeTreeNode instanceof NullNode)
      {
         throw new IOException("Missing sourceType in serializedMapping");
      }

      if(!(sourceTypeTreeNode instanceof TextNode textNode))
      {
         throw new IOException("sourceType is not a string value (is: " + sourceTypeTreeNode.getClass().getSimpleName() + ")");
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // get the value of the backendType json node, and use it to look up the qBackendModule object //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      String backendType = textNode.asText();

      QBackendModuleInterface backendModule = DeserializerUtils.getBackendModule(treeNode);

      return null;
   }

}
