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

package com.kingsrook.qqq.backend.core.model.metadata.variants;


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for BackendVariantsUtil 
 *******************************************************************************/
class BackendVariantsUtilTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetVariantId() throws QException
   {
      QBackendMetaData myBackend = getBackendMetaData();

      assertThatThrownBy(() -> BackendVariantsUtil.getVariantId(myBackend))
         .hasMessageContaining("Could not find Backend Variant information in session under key 'yourSelectedShape' for Backend 'TestBackend'");

      QContext.getQSession().setBackendVariants(Map.of("yourSelectedShape", 1701));
      assertEquals(1701, BackendVariantsUtil.getVariantId(myBackend));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QBackendMetaData getBackendMetaData()
   {
      QBackendMetaData myBackend = new QBackendMetaData()
         .withName("TestBackend")
         .withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig()
            .withOptionsTableName(TestUtils.TABLE_NAME_SHAPE)
            .withVariantTypeKey("yourSelectedShape"));
      return myBackend;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetVariantRecord() throws QException
   {
      QBackendMetaData myBackend = getBackendMetaData();

      TestUtils.insertDefaultShapes(QContext.getQInstance());

      assertThatThrownBy(() -> BackendVariantsUtil.getVariantRecord(myBackend))
         .hasMessageContaining("Could not find Backend Variant information in session under key 'yourSelectedShape' for Backend 'TestBackend'");

      QContext.getQSession().setBackendVariants(Map.of("yourSelectedShape", 1701));
      assertThatThrownBy(() -> BackendVariantsUtil.getVariantRecord(myBackend))
         .hasMessageContaining("Could not find Backend Variant in table shape with id '1701'");

      QContext.getQSession().setBackendVariants(Map.of("yourSelectedShape", 1));
      QRecord variantRecord = BackendVariantsUtil.getVariantRecord(myBackend);
      assertEquals(1, variantRecord.getValueInteger("id"));
      assertNotNull(variantRecord.getValue("name"));
   }

}