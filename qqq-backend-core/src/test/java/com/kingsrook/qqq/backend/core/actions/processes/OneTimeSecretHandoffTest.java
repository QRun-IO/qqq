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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
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
}
