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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FollowEvent;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFollower;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReference;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Record followers registration, dispatch, and failure isolation.
 *******************************************************************************/
public class RecordFollowersTest extends BaseTest
{
   private QInstance instance;
   private final RecordReference reference = new RecordReference(TestUtils.TABLE_NAME_ORDER, 42);



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUpFollowers()
   {
      instance = QContext.getQInstance();
      RecordFeaturesMetaData.ofOrWithNew(instance).withEnabled(RecordFeature.FOLLOW, true);
      Provider.instance = null;
      Provider.failure = null;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void noProvider()
   {
      assertThat(RecordFollowers.get(instance, reference, FollowEvent.CHANGE)).isEmpty();
      RecordFeaturesMetaData.ofOrWithNew(instance.getTable(reference.tableName())).withFollowersProvider(new QCodeReference(Provider.class));
      assertThat(RecordFollowers.get(instance, reference, FollowEvent.CHANGE)).isEmpty();
      assertThat(Provider.instance).isNull();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void dispatchesArguments() throws QException
   {
      QCodeReference provider = new QCodeReference(Provider.class);
      RecordFeaturesMetaData metadata = RecordFeaturesMetaData.of(instance).withFollowersProvider(provider);
      new QInstanceValidator().revalidate(instance);
      assertSame(provider, metadata.copy().getFollowersProvider());
      for(FollowEvent event : FollowEvent.values())
      {
         assertSame(Provider.followers, RecordFollowers.get(instance, reference, event));
         assertSame(instance, Provider.instance);
         assertSame(reference, Provider.reference);
         assertSame(event, Provider.event);
      }
      RecordFollower follower = Provider.followers.get(0);
      assertThat(follower.getUserId()).isEqualTo("user1");
      assertThat(follower.getNotifyOnChange()).isTrue();
      assertThat(follower.getNotifyOnComment()).isFalse();
      assertThat(follower.getReason()).isEqualTo("owner");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void followDisabled()
   {
      RecordFeaturesMetaData.of(instance).withFollowersProvider(new QCodeReference(Provider.class));
      RecordFeaturesMetaData.ofOrWithNew(instance.getTable(reference.tableName())).withEnabled(RecordFeature.FOLLOW, false);
      assertThat(RecordFollowers.get(instance, reference, FollowEvent.ALERT)).isEmpty();
      assertThat(Provider.instance).isNull();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void providerFailureIsLogged()
   {
      RecordFeaturesMetaData.of(instance).withFollowersProvider(new QCodeReference(Provider.class));
      Provider.failure = new QException("Provider unavailable");
      QCollectingLogger logger = QLogger.activateCollectingLoggerForClass(RecordFollowers.class);
      try
      {
         assertThat(RecordFollowers.get(instance, reference, FollowEvent.COMMENT)).isEmpty();
         assertThat(logger.getCollectedMessages()).hasSize(1);
         assertThat(logger.getCollectedMessages().get(0).getLevel()).isEqualTo(Level.WARN);
         assertThat(logger.getCollectedMessages().get(0).getMessage()).contains("Unable to get record followers");
         assertThat(logger.getCollectedMessages().get(0).getMessage()).contains("Provider unavailable");
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(RecordFollowers.class);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void rejectsBadCodeReferences()
   {
      for(QCodeReference provider : List.of(new QCodeReference(String.class), new QCodeReference("missing.FollowersProvider", QCodeType.JAVA)))
      {
         RecordFeaturesMetaData.of(instance).withFollowersProvider(provider);
         QInstanceValidator validator = new QInstanceValidator();
         assertThrows(QException.class, () -> validator.revalidate(instance));
         assertThat(validator.getErrors()).anyMatch(error -> error.contains("followers provider"));
         assertThat(RecordFollowers.get(instance, reference, FollowEvent.CHANGE)).isEmpty();
      }
   }



   /*******************************************************************************
    ** Test provider captures the exact dispatch arguments.
    *******************************************************************************/
   public static class Provider implements RecordFollowersProviderInterface
   {
      private static QInstance instance;
      private static RecordReference reference;
      private static FollowEvent event;
      private static QException failure;
      private static final List<RecordFollower> followers = List.of(new RecordFollower().withUserId("user1")
         .withNotifyOnChange(true).withNotifyOnComment(false).withReason("owner"));



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<RecordFollower> getFollowers(QInstance instance, RecordReference reference, FollowEvent event) throws QException
      {
         Provider.instance = instance;
         Provider.reference = reference;
         Provider.event = event;
         if(failure != null)
         {
            throw (failure);
         }
         return (followers);
      }
   }

}
