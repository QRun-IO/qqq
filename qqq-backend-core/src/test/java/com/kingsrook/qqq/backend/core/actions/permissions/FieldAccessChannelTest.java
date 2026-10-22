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

package com.kingsrook.qqq.backend.core.actions.permissions;


import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKernelSettings;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiExposureChannelPolicy;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessChannelPolicyInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Channel ceilings share the core decision, including unconfigured instances.
 *******************************************************************************/
class FieldAccessChannelTest extends FieldAccessTestSupport
{
   /*******************************************************************************
    ** The default assistant policy is isolated from all other channels.
    *******************************************************************************/
   @Test
   void defaultsWithoutInstanceConfiguration()
   {
      QContext.getQInstance().getSupplementalMetaData().remove(FieldAccessResolverMetaData.NAME);
      table.getField("email").withSupplementalMetaData(new AiFieldMetaData().withExposure(AiExposure.MASKED));
      assertNull(QContext.getQInstance().getSupplementalMetaData().get(ClientChannelsMetaData.NAME));
      for(String channel : new String[] { null, "next-ui", "api", "cli" })
      {
         QContext.getQSession().setClientChannel(channel);
         assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      }
      String fingerprint = FieldAccessHelper.getAccessFingerprint();
      QContext.getQSession().setClientChannel("assistant/turn-1");
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTier(table, "email"));
      assertNotEquals(fingerprint, FieldAccessHelper.getAccessFingerprint());
      table.getField("email").withSupplementalMetaData(new AiFieldMetaData().withExposure(AiExposure.METADATA_ONLY));
      assertEquals(AiExposureChannelPolicy.METADATA_ONLY_REASON, FieldAccessHelper.getDecision(table, table.getField("email")).reason());
   }



   /*******************************************************************************
    ** No policy can elevate a resolver or a lock, and a channel change invalidates
    ** resolver decisions even when the session object itself is reused.
    *******************************************************************************/
   @Test
   void mostRestrictiveAndChannelCache()
   {
      QContext.getQSession().setClientChannel("assistant");
      QFieldMetaData email = table.getField("email");
      email.withSupplementalMetaData(new AiFieldMetaData().withExposure(AiExposure.MASKED));
      restrict(FieldAccessTier.REVEALABLE);
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTier(table, "email"));
      restrict(FieldAccessTier.HIDDEN);
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
      restrict(FieldAccessTier.FULL);
      email.withFieldSecurityLock(new FieldSecurityLock().withDefaultBehavior(FieldSecurityLock.Behavior.DENY));
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
      email.setFieldSecurityLock(null);
      FieldAccessHelper.getTier(table, "email");
      int calls = resolver.calls;
      QContext.getQSession().setClientChannel("next-ui");
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      assertEquals(calls + 1, resolver.calls);
   }



   /*******************************************************************************
    ** Instance settings and both registration forms feed the same decision.
    *******************************************************************************/
   @Test
   void settingsAndRegistration()
   {
      QContext.getQSession().setClientChannel("assistant");
      QContext.getQInstance().withSupplementalMetaData(new AiToolKernelSettings().withDefaultExposure(AiExposure.METADATA_ONLY));
      assertEquals(FieldAccessTier.MASKED, FieldAccessHelper.getTier(table, "email"));
      QContext.setObject(AiToolKernelSettings.NAME, new AiToolKernelSettings().withDefaultExposure(AiExposure.NONE));
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
      ClientChannelsMetaData channels = ClientChannelsMetaData.ofOrWithNew(QContext.getQInstance());
      channels.withFieldAccessPolicy("assistant", (t, f) -> FieldAccessDecision.FULL);
      assertEquals(FieldAccessTier.FULL, FieldAccessHelper.getTier(table, "email"));
      channels.withFieldAccessPolicy("assistant", new QCodeReference(HiddenPolicy.class));
      assertEquals(FieldAccessTier.HIDDEN, FieldAccessHelper.getTier(table, "email"));
   }



   /*******************************************************************************
    ** Code-reference policy fixture.
    *******************************************************************************/
   public static class HiddenPolicy implements FieldAccessChannelPolicyInterface
   {
      /*******************************************************************************
       ** A restrictive channel.
       *******************************************************************************/
      @Override
      public FieldAccessDecision resolve(QTableMetaData table, QFieldMetaData field)
      {
         return (new FieldAccessDecision(FieldAccessTier.HIDDEN, null, "channel"));
      }
   }
}
