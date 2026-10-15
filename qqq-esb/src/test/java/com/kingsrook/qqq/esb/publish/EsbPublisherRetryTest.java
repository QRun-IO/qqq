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

package com.kingsrook.qqq.esb.publish;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import jakarta.jms.JMSException;
import jakarta.jms.TransactionRolledBackException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for which publish failures EsbPublisher retries once.
 *******************************************************************************/
class EsbPublisherRetryTest
{

   /*******************************************************************************
    ** Failover rollbacks, in-doubt commits, and stale sessions are retried;
    ** broker rejections and unknown destinations are not.
    *******************************************************************************/
   @Test
   void testIsFailoverOrStaleSession()
   {
      assertThat(EsbPublisher.isFailoverOrStaleSession(new TransactionRolledBackException("AMQ219025: The transaction was rolled back on failover to a backup server"))).isTrue();
      JMSException inDoubt = new JMSException("wrapper");
      inDoubt.initCause(new RuntimeException("AMQ219023: The commit outcome unknown due to failover"));
      assertThat(EsbPublisher.isFailoverOrStaleSession(inDoubt)).isTrue();
      assertThat(EsbPublisher.isFailoverOrStaleSession(new jakarta.jms.IllegalStateException("Session is closed"))).isTrue();
      assertThat(EsbPublisher.isFailoverOrStaleSession(new jakarta.jms.IllegalStateException("Publishing connection is no longer current"))).isTrue();

      assertThat(EsbPublisher.isFailoverOrStaleSession(new TransactionRolledBackException("owned commit rejection"))).isFalse();
      assertThat(EsbPublisher.isFailoverOrStaleSession(new QException("Unknown ESB destination: nope"))).isFalse();
      assertThat(EsbPublisher.isRetryable("nope", new QException("failover"))).as("an unknown destination is never retried").isFalse();
   }

}
