/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata.tables;


import java.util.Set;
import java.util.function.BiConsumer;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ApiTableMetaDataContainer 
 *******************************************************************************/
class ApiTableMetaDataContainerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testClone()
   {
      BiConsumer<QTableMetaData, String> setStrings = (table, s) ->
      {
         ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.ofOrWithNew(table);
         apiTableMetaDataContainer.withApis(MapBuilder.of(s, new ApiTableMetaData()
            .withInitialVersion(s)
            .withFinalVersion(s)
            .withEnabledOperations(Set.of(ApiOperation.DELETE))
            .withDisabledOperations(Set.of(ApiOperation.GET))
            .withApiAssociationMetaData(MapBuilder.of(s, new ApiAssociationMetaData().withInitialVersion(s)))
            .withRemovedApiFields(ListBuilder.of(new QFieldMetaData(s, QFieldType.INTEGER)))
         ));
      };

      BiConsumer<QTableMetaData, String> assertStrings = (table, s) ->
      {
         ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
         ApiTableMetaData          apiTableMetaData          = apiTableMetaDataContainer.getApis().get(s);
         assertEquals(s, apiTableMetaData.getInitialVersion());
         assertEquals(s, apiTableMetaData.getFinalVersion());
         assertEquals(Set.of(ApiOperation.DELETE), apiTableMetaData.getEnabledOperations());
         assertEquals(Set.of(ApiOperation.GET), apiTableMetaData.getDisabledOperations());
         assertEquals(s, apiTableMetaData.getApiAssociationMetaData().get(s).getInitialVersion());
         assertEquals(s, apiTableMetaData.getRemovedApiFields().get(0).getName());
      };

      QTableMetaData tableMetaData = new QTableMetaData();
      setStrings.accept(tableMetaData, "a");
      assertStrings.accept(tableMetaData, "a");

      QTableMetaData clone = tableMetaData.clone();
      assertStrings.accept(tableMetaData, "a");
      assertStrings.accept(clone, "a");

      setStrings.accept(clone, "z");
      assertStrings.accept(tableMetaData, "a");
      assertStrings.accept(clone, "z");
   }

}