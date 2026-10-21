/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.util.Collections;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Public API query and get retain core field enforcement.
 *******************************************************************************/
class FieldAccessApiTest extends BaseTest
{
   /*******************************************************************************
    ** The API adapter receives masks from the enforced core actions.
    *******************************************************************************/
   @Test
   void queryAndGet() throws Exception
   {
      TestUtils.insertSimpsons();
      QContext.getQInstance().setEnforcePermissionsInCoreActions(false);
      FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()).withResolverInstance((table, field) -> "lastName".equals(field.getName())
         ? new FieldAccessDecision(FieldAccessTier.MASKED, FieldMaskStyle.FULL, null) : FieldAccessDecision.FULL);
      ApiInstanceMetaData api = ApiInstanceMetaDataContainer.of(QContext.getQInstance()).getApiInstanceMetaData(TestUtils.API_NAME);
      assertEquals("***", ApiImplementation.get(api, TestUtils.CURRENT_API_VERSION, "person", "1").get("lastName"));
      String query = JsonUtils.toJson(ApiImplementation.query(api, TestUtils.CURRENT_API_VERSION, "person", Collections.emptyMap()));
      assertFalse(query.contains("Simpson"));
      assertTrue(query.contains("***"));
   }
}
