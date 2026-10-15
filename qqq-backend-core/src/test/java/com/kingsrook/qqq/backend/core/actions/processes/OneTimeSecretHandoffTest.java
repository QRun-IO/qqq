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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
import com.kingsrook.qqq.backend.core.state.QStateProviders;
import com.kingsrook.qqq.backend.core.state.StateProviderMetaData;
import com.kingsrook.qqq.backend.core.state.TableStateProvider;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for OneTimeSecretHandoff
 *******************************************************************************/
class OneTimeSecretHandoffTest extends BaseTest
{

   /*******************************************************************************
    ** A parked secret is taken once; nulls and empties are ignored.
    *******************************************************************************/
   @Test
   void testOfferTakeOnce()
   {
      OneTimeSecretHandoff.offer("p1", List.of(new OneTimeSecret().withLabel("Key").withValue("v").withMasked("****v")));
      assertThat(OneTimeSecretHandoff.take("p1")).extracting(OneTimeSecret::getValue).containsExactly("v");
      assertThat(OneTimeSecretHandoff.take("p1")).isEmpty();

      OneTimeSecretHandoff.offer("p2", List.of());
      OneTimeSecretHandoff.offer(null, List.of(new OneTimeSecret().withValue("x")));
      assertThat(OneTimeSecretHandoff.take("p2")).isEmpty();
      assertThat(OneTimeSecretHandoff.take(null)).isEmpty();
   }



   /*******************************************************************************
    ** With an encrypted shared store, the store holds the only copy: once another
    ** node's poll took it, this node has nothing left to hand out.
    *******************************************************************************/
   @Test
   void testSharedStoreHoldsTheOnlyCopy() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      instance.addTable(TableStateProvider.defineTable(StateProviderMetaData.DEFAULT_TABLE_NAME, TestUtils.MEMORY_BACKEND_NAME));
      byte[] key = new byte[32];
      new SecureRandom().nextBytes(key);
      instance.withSupplementalMetaData(new StateProviderMetaData().withTableName(StateProviderMetaData.DEFAULT_TABLE_NAME).withEncryptionKey(Base64.getEncoder().encodeToString(key)));
      QStateProviders.clearForTests();
      try
      {
         String processUUID = UUID.randomUUID().toString();
         OneTimeSecretHandoff.offer(processUUID, List.of(new OneTimeSecret().withLabel("Key").withValue("shared").withMasked("****")));
         assertThat(QStateProviders.get().get(Serializable.class, OneTimeSecretHandoff.key(processUUID))).isPresent();

         ///////////////////////////////////////////////////////////
         // another node's status poll took (and removed) it      //
         ///////////////////////////////////////////////////////////
         QStateProviders.get().remove(OneTimeSecretHandoff.key(processUUID));
         assertThat(OneTimeSecretHandoff.take(processUUID)).isEmpty();

         ///////////////////////////////////////////////////////////
         // and a poll on any node takes it from the store, once  //
         ///////////////////////////////////////////////////////////
         String other = UUID.randomUUID().toString();
         OneTimeSecretHandoff.offer(other, List.of(new OneTimeSecret().withLabel("Key").withValue("once").withMasked("****")));
         assertThat(OneTimeSecretHandoff.take(other)).extracting(OneTimeSecret::getValue).containsExactly("once");
         assertThat(OneTimeSecretHandoff.take(other)).isEmpty();
      }
      finally
      {
         QStateProviders.clearForTests();
      }
   }
}
