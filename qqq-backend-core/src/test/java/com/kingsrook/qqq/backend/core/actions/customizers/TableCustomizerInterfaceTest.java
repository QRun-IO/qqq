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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for TableCustomizerInterface 
 *******************************************************************************/
class TableCustomizerInterfaceTest extends BaseTest
{
   private static List<String> events = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      events.clear();
      QLogger.deactivateCollectingLoggerForClass(TableCustomizerInterface.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreInsertOnly() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(PreInsertOnly.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PreInsertOnly.class));
      reInitInstanceInContext(qInstance);

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(TableCustomizerInterface.class);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1).contains("PreInsertOnly.preInsert()");
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1);
      assertThat(collectingLogger.getCollectedMessages()).hasSize(1).element(0).extracting("message").asString().contains("A default implementation of preUpdate is running");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreUpdateOnly() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PreUpdateOnly.class))
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(PreUpdateOnly.class));
      reInitInstanceInContext(qInstance);

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(TableCustomizerInterface.class);

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1).contains("PreUpdateOnly.preUpdate()");
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1);
      assertThat(collectingLogger.getCollectedMessages()).hasSize(1).element(0).extracting("message").asString().contains("A default implementation of preInsert is running");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreInsertOrUpdateOnly() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PreInsertOrUpdateOnly.class))
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(PreInsertOrUpdateOnly.class));
      reInitInstanceInContext(qInstance);

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(TableCustomizerInterface.class);

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1).contains("PreInsertOrUpdateOnly.preInsertOrUpdate()");
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(2).allMatch(s -> s.contains("PreInsertOrUpdateOnly.preInsertOrUpdate()"));
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostInsertOnly() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(PostInsertOnly.class))
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(PostInsertOnly.class));
      reInitInstanceInContext(qInstance);

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(TableCustomizerInterface.class);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1).contains("PostInsertOnly.postInsert()");
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1);
      assertThat(collectingLogger.getCollectedMessages()).hasSize(1).element(0).extracting("message").asString().contains("A default implementation of postUpdate is running");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostUpdateOnly() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(PostUpdateOnly.class))
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(PostUpdateOnly.class));
      reInitInstanceInContext(qInstance);

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(TableCustomizerInterface.class);

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1).contains("PostUpdateOnly.postUpdate()");
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1);
      assertThat(collectingLogger.getCollectedMessages()).hasSize(1).element(0).extracting("message").asString().contains("A default implementation of postInsert is running");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostInsertOrUpdateOnly() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(PostInsertOrUpdateOnly.class))
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(PostInsertOrUpdateOnly.class));
      reInitInstanceInContext(qInstance);

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(TableCustomizerInterface.class);

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(1).contains("PostInsertOrUpdateOnly.postInsertOrUpdate()");
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(2).allMatch(s -> s.contains("PostInsertOrUpdateOnly.postInsertOrUpdate()"));
      assertThat(collectingLogger.getCollectedMessages()).hasSize(0);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class PreInsertOnly implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
      {
         events.add("PreInsertOnly.preInsert()");
         return (records);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class PreUpdateOnly implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
      {
         events.add("PreUpdateOnly.preUpdate()");
         return (records);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class PreInsertOrUpdateOnly implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
      {
         events.add("PreInsertOrUpdateOnly.preInsertOrUpdate()");
         return (records);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class PostInsertOnly implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
      {
         events.add("PostInsertOnly.postInsert()");
         return (records);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class PostUpdateOnly implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
      {
         events.add("PostUpdateOnly.postUpdate()");
         return (records);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class PostInsertOrUpdateOnly implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> postInsertOrUpdate(AbstractActionInput input, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
      {
         events.add("PostInsertOrUpdateOnly.postInsertOrUpdate()");
         return (records);
      }
   }

}