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
// SPDX-License-Identifier: Apache-2.0

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.profile.UserProfileMetaDataProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.VersionInput;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.VersionResponseV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for VersionExecutor: the "avatars", "durableJobs" and
 ** "recordSearch" features are advertised only when something backs them.
 *******************************************************************************/
class VersionExecutorTest
{
   private static final String AVATARS = "avatars";

   private QInstance qInstance;



   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      MemoryRecordStore.fullReset();
      qInstance = TestUtils.defineInstance();

      ///////////////////////////////////////////////////////////////////////
      // back durableJobs and recordSearch, so only avatars varies by test //
      ///////////////////////////////////////////////////////////////////////
      new DurableJobsMetaDataProvider().defineAll(qInstance, TestUtils.BACKEND_NAME_MEMORY, null);
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON).setSearchFields(List.of("firstName"));
      QContext.init(qInstance, new QSystemUserSession());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Without user profiles installed, avatars is not advertised, and the
    ** other features are untouched.
    *******************************************************************************/
   @Test
   void testAvatarsNotAdvertisedWithoutProfiles() throws QException
   {
      VersionResponseV1 output = execute();

      assertThat(output.getFeatures()).doesNotContain(AVATARS);
      List<String> expected = MiddlewareVersionV1.FEATURES.stream().filter(f -> !AVATARS.equals(f) && !List.of("nativeSessionLogin", "nativeSessionLogout", "nativeSessionNonceValidation", "nativeSessionCallbackNoncePolicies").contains(f)).toList();
      assertThat(output.getFeatures()).containsExactlyElementsOf(expected);
   }



   /*******************************************************************************
    ** With user profiles installed, avatars is advertised along with every
    ** other feature, in order.
    *******************************************************************************/
   @Test
   void testAvatarsAdvertisedWithProfiles() throws QException
   {
      qInstance.addTable(new QTableMetaData().withName("avatarStorage").withBackendName(TestUtils.BACKEND_NAME_MEMORY));
      new UserProfileMetaDataProvider().defineAll(qInstance, TestUtils.BACKEND_NAME_MEMORY, "avatarStorage");

      VersionResponseV1 output = execute();

      assertThat(output.getFeatures()).contains(AVATARS);
      assertThat(output.getFeatures()).containsExactlyElementsOf(MiddlewareVersionV1.FEATURES.stream().filter(f -> !List.of("nativeSessionLogin", "nativeSessionLogout", "nativeSessionNonceValidation", "nativeSessionCallbackNoncePolicies").contains(f)).toList());
   }



   /*******************************************************************************
    ** A version that does not list avatars is unaffected either way.
    *******************************************************************************/
   @Test
   void testAdvertisedFeaturesLeavesOtherFeaturesAlone()
   {
      assertThat(VersionExecutor.advertisedFeatures(List.of("tables", "version"))).containsExactly("tables", "version");
      assertThat(VersionExecutor.advertisedFeatures(List.of())).isEmpty();
   }



   /*******************************************************************************
    ** Without the durable jobs tables (whose routes are then not bound), and
    ** with no search provider or table with search fields, durableJobs and
    ** recordSearch are not advertised (#1337, #1343).
    *******************************************************************************/
   @Test
   void testUnbackedJobsAndSearchNotAdvertised() throws QException
   {
      QInstance bare = TestUtils.defineInstance();
      bare.getTables().values().forEach(table -> table.setSearchFields(null));
      QContext.init(bare, new QSystemUserSession());

      VersionResponseV1 output = execute();
      assertThat(output.getFeatures()).doesNotContain("durableJobs", "recordSearch");
      assertThat(output.getFeatures()).contains("tables", "processes");

      bare.getTable(TestUtils.TABLE_NAME_PERSON).setSearchFields(List.of("firstName"));
      assertThat(execute().getFeatures()).contains("recordSearch").doesNotContain("durableJobs");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private VersionResponseV1 execute() throws QException
   {
      VersionResponseV1 output = new VersionResponseV1();
      new VersionExecutor().execute(new VersionInput().withMiddlewareVersion(new MiddlewareVersionV1()), output);
      return (output);
   }
}
