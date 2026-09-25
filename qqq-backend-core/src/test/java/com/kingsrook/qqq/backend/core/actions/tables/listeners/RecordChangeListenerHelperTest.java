/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.actions.tables.listeners;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryDeleteAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryQueryAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryUpdateAction;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for RecordChangeListenerHelper.  The public nested classes are the
 ** listeners and the no-pre-fetch backend that the Insert, Update and Delete
 ** action tests use too.
 *******************************************************************************/
public class RecordChangeListenerHelperTest extends BaseTest
{
   public static final String NO_PREFETCH_TABLE_NAME = "recordChangeNoPrefetch";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      resetListeners();
   }



   /*******************************************************************************
    ** Clear the static state the listeners and the test backend keep.
    *******************************************************************************/
   public static void resetListeners()
   {
      CapturingListener.events.clear();
      CapturingListener.appliesToTableName = null;
      CapturingListener.appliesToType = null;
      ThrowingListener.events.clear();
      NoPrefetchMemoryModule.failQueries = false;
   }



   /*******************************************************************************
    ** Add a memory-backed table whose backend does not pre-fetch old records on
    ** update or delete (like the API and filesystem backends).
    *******************************************************************************/
   public static String defineNoPrefetchTable()
   {
      QBackendModuleDispatcher.registerBackendModule(new NoPrefetchMemoryModule());
      QInstance qInstance = QContext.getQInstance();
      qInstance.addBackend(new QBackendMetaData().withName(NO_PREFETCH_TABLE_NAME).withBackendType(NoPrefetchMemoryModule.class));
      qInstance.addTable(new QTableMetaData().withName(NO_PREFETCH_TABLE_NAME).withBackendName(NO_PREFETCH_TABLE_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING)));
      return (NO_PREFETCH_TABLE_NAME);
   }



   /*******************************************************************************
    ** Number of queries the memory backend has run since statistics were reset.
    *******************************************************************************/
   public static Integer getMemoryQueryCount()
   {
      return (MemoryRecordStore.getStatistics().getOrDefault(MemoryRecordStore.STAT_QUERIES_RAN, 0));
   }



   /*******************************************************************************
    ** With no listeners, nothing applies and firing is a no-op.
    *******************************************************************************/
   @Test
   void testNoListeners()
   {
      QInstance qInstance = QContext.getQInstance();
      assertThat(qInstance.getRecordChangeListeners()).isEmpty();
      assertFalse(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.INSERT));
      assertFalse(RecordChangeListenerHelper.anyApply(null, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.INSERT));
      RecordChangeListenerHelper.fire(qInstance, personInsertEvent());
      RecordChangeListenerHelper.fire(null, personInsertEvent());

      qInstance.setRecordChangeListeners(null);
      assertFalse(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.INSERT));
      RecordChangeListenerHelper.fire(qInstance, personInsertEvent());

      qInstance.withRecordChangeListener(new QCodeReference(CapturingListener.class));
      assertThat(qInstance.getRecordChangeListeners()).hasSize(1);
      RecordChangeListenerHelper.fire(qInstance, null);
      assertThat(CapturingListener.events).isEmpty();
   }



   /*******************************************************************************
    ** Only applying listeners hear about an event, and events without records
    ** are not sent at all.
    *******************************************************************************/
   @Test
   void testFireOnlyToApplyingListeners()
   {
      QInstance qInstance = QContext.getQInstance().withRecordChangeListener(new QCodeReference(CapturingListener.class));
      CapturingListener.appliesToTableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      assertTrue(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.INSERT));
      assertFalse(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_SHAPE, RecordChangeType.INSERT));

      CapturingListener.appliesToType = RecordChangeType.UPDATE;
      assertFalse(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.INSERT));
      assertTrue(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.UPDATE));
      CapturingListener.appliesToType = null;

      RecordChangeEvent event = personInsertEvent();
      RecordChangeListenerHelper.fire(qInstance, event);
      RecordChangeListenerHelper.fire(qInstance, personInsertEvent().withTableName(TestUtils.TABLE_NAME_SHAPE));
      RecordChangeListenerHelper.fire(qInstance, personInsertEvent().withRecords(List.of()));
      RecordChangeListenerHelper.fire(qInstance, personInsertEvent().withRecords(null));
      assertThat(CapturingListener.events).containsExactly(event);
   }



   /*******************************************************************************
    ** A listener that throws, or that cannot be loaded, does not keep the other
    ** listeners from running, and nothing escapes to the caller.
    *******************************************************************************/
   @Test
   void testListenerFailuresAreCaught()
   {
      QInstance qInstance = QContext.getQInstance()
         .withRecordChangeListener(new QCodeReference(ThrowingListener.class))
         .withRecordChangeListener(new QCodeReference(ThrowingAppliesToListener.class))
         .withRecordChangeListener(new QCodeReference(Object.class))
         .withRecordChangeListener(new QCodeReference("com.kingsrook.qqq.NoSuchListener", QCodeType.JAVA))
         .withRecordChangeListener(new QCodeReference("noSuchScript", QCodeType.JAVA_SCRIPT))
         .withRecordChangeListener(new QCodeReference(CapturingListener.class));

      RecordChangeEvent event = personInsertEvent();
      RecordChangeListenerHelper.fire(qInstance, event);
      assertThat(ThrowingListener.events).containsExactly(event);
      assertThat(CapturingListener.events).containsExactly(event);
      assertTrue(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.DELETE));

      qInstance.setRecordChangeListeners(new ArrayList<>(List.of(new QCodeReference(ThrowingAppliesToListener.class), new QCodeReference(Object.class))));
      assertFalse(RecordChangeListenerHelper.anyApply(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY, RecordChangeType.DELETE));
   }



   /*******************************************************************************
    ** The event is a plain bean; setters and fluent setters match.
    *******************************************************************************/
   @Test
   void testEventAccessors()
   {
      QBackendTransaction transaction = new QBackendTransaction();
      List<QRecord>       records     = List.of(new QRecord().withValue("id", 1));
      List<QRecord>       oldRecords  = List.of(new QRecord().withValue("id", 1).withValue("firstName", "Old"));

      RecordChangeEvent event = new RecordChangeEvent();
      event.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      event.setType(RecordChangeType.UPDATE);
      event.setRecords(records);
      event.setOldRecords(oldRecords);
      event.setTransaction(transaction);

      assertEquals(TestUtils.TABLE_NAME_PERSON_MEMORY, event.getTableName());
      assertEquals(RecordChangeType.UPDATE, event.getType());
      assertSame(records, event.getRecords());
      assertSame(oldRecords, event.getOldRecords());
      assertSame(transaction, event.getTransaction());

      RecordChangeEvent fluent = new RecordChangeEvent().withOldRecords(oldRecords).withTransaction(transaction);
      assertSame(oldRecords, fluent.getOldRecords());
      assertSame(transaction, fluent.getTransaction());
      assertNull(fluent.getTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RecordChangeEvent personInsertEvent()
   {
      return (new RecordChangeEvent()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withType(RecordChangeType.INSERT)
         .withRecords(List.of(new QRecord().withValue("id", 1))));
   }



   /*******************************************************************************
    ** Records every event it gets.  Applies to all tables and types unless
    ** appliesToTableName or appliesToType is set.
    *******************************************************************************/
   public static class CapturingListener implements RecordChangeListenerInterface
   {
      public static final List<RecordChangeEvent> events = new ArrayList<>();

      public static String           appliesToTableName = null;
      public static RecordChangeType appliesToType      = null;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean appliesTo(String tableName, RecordChangeType type)
      {
         return ((appliesToTableName == null || appliesToTableName.equals(tableName)) && (appliesToType == null || appliesToType.equals(type)));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void onRecordsChanged(RecordChangeEvent event)
      {
         events.add(event);
      }
   }



   /*******************************************************************************
    ** Records every event it gets, then throws.
    *******************************************************************************/
   public static class ThrowingListener implements RecordChangeListenerInterface
   {
      public static final List<RecordChangeEvent> events = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean appliesTo(String tableName, RecordChangeType type)
      {
         return (true);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void onRecordsChanged(RecordChangeEvent event) throws QException
      {
         events.add(event);
         throw (new QException("Expected listener failure"));
      }
   }



   /*******************************************************************************
    ** Throws from appliesTo, so it never receives an event.
    *******************************************************************************/
   public static class ThrowingAppliesToListener implements RecordChangeListenerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean appliesTo(String tableName, RecordChangeType type)
      {
         throw (new IllegalStateException("Expected appliesTo failure"));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void onRecordsChanged(RecordChangeEvent event)
      {
         throw (new IllegalStateException("Should not be called"));
      }
   }



   /*******************************************************************************
    ** Memory storage behind update and delete actions that do not pre-fetch old
    ** records.  Set failQueries to make every query throw.
    *******************************************************************************/
   public static class NoPrefetchMemoryModule extends MemoryBackendModule
   {
      public static Boolean failQueries = false;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getBackendType()
      {
         return (NO_PREFETCH_TABLE_NAME);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public UpdateInterface getUpdateInterface()
      {
         return (new MemoryUpdateAction()
         {
            @Override
            public boolean supportsPreFetchQuery()
            {
               return (false);
            }
         });
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public DeleteInterface getDeleteInterface()
      {
         return (new MemoryDeleteAction()
         {
            @Override
            public boolean supportsPreFetchQuery()
            {
               return (false);
            }
         });
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QueryInterface getQueryInterface()
      {
         return (new MemoryQueryAction()
         {
            @Override
            public QueryOutput execute(QueryInput queryInput) throws QException
            {
               if(failQueries)
               {
                  throw (new QException("Expected query failure"));
               }
               return (super.execute(queryInput));
            }
         });
      }
   }
}
