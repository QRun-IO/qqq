/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.childjoinwithoutfield.TestChildJoinWithoutFieldEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.childwidgetwithoutjoin.TestChildWidgetWithoutJoinEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.constructorthrows.TestThrowsInConstructorMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.noargconstructor.TestOnlyArgConstructorMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.notpossiblevalueenum.TestNotPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.notrecordentity.TestNotRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.produce.TestThrowsInProduceMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.produce.TestWorkingMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.tablenamenotstring.TestTableNameNotStringEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestAbstractMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestDisabledMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestImplementsMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestMetaDataProducingChildEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestMetaDataProducingPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestNoInterfacesExtendsObject;
import com.kingsrook.qqq.backend.core.model.metadata.producers.TestNoValidConstructorMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for MetaDataProducerHelper
 *******************************************************************************/
class MetaDataProducerHelperTest
{
   private static final String DISABLE_NAME_TIEBREAKER_PROPERTY = "qqq.MetaDataProducerHelper.disableNameTiebreaker";
   private static final String FAIL_ON_PRODUCER_ERROR_PROPERTY  = "qqq.metaData.failOnProducerError";

   private static final String PRODUCE_ERRORS_PACKAGE  = "com.kingsrook.qqq.backend.core.model.metadata.producererrors.produce";
   private static final String CONSTRUCTOR_THROWS_PACKAGE = "com.kingsrook.qqq.backend.core.model.metadata.producererrors.constructorthrows";



   /***************************************************************************
    *
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      System.clearProperty(DISABLE_NAME_TIEBREAKER_PROPERTY);
      System.clearProperty(FAIL_ON_PRODUCER_ERROR_PROPERTY);
      QLogger.deactivateCollectingLoggerForClass(MetaDataProducerHelper.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = new QInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, "com.kingsrook.qqq.backend.core.model.metadata.producers");
      assertTrue(qInstance.getTables().containsKey(TestMetaDataProducer.NAME));
      assertTrue(qInstance.getTables().containsKey(TestImplementsMetaDataProducer.NAME));
      assertFalse(qInstance.getTables().containsKey(TestNoValidConstructorMetaDataProducer.NAME));
      assertFalse(qInstance.getTables().containsKey(TestNoInterfacesExtendsObject.NAME));
      assertFalse(qInstance.getTables().containsKey(TestAbstractMetaDataProducer.NAME));
      assertFalse(qInstance.getTables().containsKey(TestDisabledMetaDataProducer.NAME));

      /////////////////////////////////////////////
      // annotation on PVS enum -> PVS meta data //
      /////////////////////////////////////////////
      assertTrue(qInstance.getPossibleValueSources().containsKey(TestMetaDataProducingPossibleValueEnum.class.getSimpleName()));
      QPossibleValueSource enumPVS = qInstance.getPossibleValueSource(TestMetaDataProducingPossibleValueEnum.class.getSimpleName());
      assertEquals(QPossibleValueSourceType.ENUM, enumPVS.getType());
      assertEquals(2, enumPVS.getEnumValues().size());
      assertEquals(new QPossibleValue<>(1, "One"), enumPVS.getEnumValues().get(0));

      ////////////////////////////////////////////
      // annotation on table -> table meta data //
      ////////////////////////////////////////////
      assertTrue(qInstance.getTables().containsKey(TestMetaDataProducingEntity.TABLE_NAME));
      QTableMetaData table = qInstance.getTables().get(TestMetaDataProducingEntity.TABLE_NAME);
      assertEquals(TestMetaDataProducingEntity.TABLE_NAME, table.getName());
      assertEquals("id", table.getPrimaryKeyField());
      assertEquals(2, table.getFields().size());
      assertTrue(table.getField("name").getIsRequired());
      assertEquals("Customized Label", table.getLabel());

      //////////////////////////////////////////////
      // annotation on PVS table -> PVS meta data //
      //////////////////////////////////////////////
      assertTrue(qInstance.getPossibleValueSources().containsKey(TestMetaDataProducingEntity.TABLE_NAME));
      QPossibleValueSource tablePVS = qInstance.getPossibleValueSource(TestMetaDataProducingEntity.TABLE_NAME);
      assertEquals(QPossibleValueSourceType.TABLE, tablePVS.getType());
      assertEquals(TestMetaDataProducingEntity.TABLE_NAME, tablePVS.getTableName());

      //////////////////////////////////////////////////////////////////
      // annotation on parent table w/ joined child -> join meta data //
      //////////////////////////////////////////////////////////////////
      String joinName = QJoinMetaData.makeInferredJoinName(TestMetaDataProducingEntity.TABLE_NAME, TestMetaDataProducingChildEntity.TABLE_NAME);
      assertTrue(qInstance.getJoins().containsKey(joinName));
      QJoinMetaData join = qInstance.getJoin(joinName);
      assertEquals(TestMetaDataProducingEntity.TABLE_NAME, join.getLeftTable());
      assertEquals(TestMetaDataProducingChildEntity.TABLE_NAME, join.getRightTable());
      assertEquals(JoinType.ONE_TO_MANY, join.getType());
      assertEquals("id", join.getJoinOns().get(0).getLeftField());
      assertEquals("parentId", join.getJoinOns().get(0).getRightField());

      //////////////////////////////////////////////////////////////////////////////////////
      // annotation on parent table w/ joined child -> child record list widget meta data //
      //////////////////////////////////////////////////////////////////////////////////////
      assertTrue(qInstance.getWidgets().containsKey(joinName));
      QWidgetMetaDataInterface widget = qInstance.getWidget(joinName);
      assertEquals(WidgetType.CHILD_RECORD_LIST.getType(), widget.getType());
      assertEquals("Test Children", widget.getLabel());
      assertEquals(joinName, widget.getDefaultValues().get("joinName"));
      assertEquals(false, widget.getDefaultValues().get("canAddChildRecord"));
      assertNull(widget.getDefaultValues().get("manageAssociationName"));
      assertEquals(15, widget.getDefaultValues().get("maxRows"));

   }



   /*******************************************************************************
    ** Test that producers with the same sortOrder and type are sorted by class
    ** simple name (alphabetically), then by full class name, for deterministic ordering.
    *******************************************************************************/
   @Test
   void testSortByClassSimpleNameThenFullName()
   {
      ///////////////////////////////////////////////////////////////////////////
      // create producers with same sortOrder - they should sort by class name //
      ///////////////////////////////////////////////////////////////////////////
      MetaDataProducerInterface<?> producerZ = new TestProducerZebra();
      MetaDataProducerInterface<?> producerA = new TestProducerAlpha();
      MetaDataProducerInterface<?> producerM = new TestProducerMango();

      ///////////////////////////////////////////////////////
      // put them in an "unsorted" order and sort the list //
      ///////////////////////////////////////////////////////
      List<MetaDataProducerInterface<?>> producers = new ArrayList<>(List.of(producerZ, producerA, producerM));
      MetaDataProducerHelper.sortMetaDataProducers(producers);

      //////////////////////////////////////////////////////////////
      // verify they are now sorted alphabetically by simple name //
      //////////////////////////////////////////////////////////////
      assertEquals("TestProducerAlpha", producers.get(0).getClass().getSimpleName());
      assertEquals("TestProducerMango", producers.get(1).getClass().getSimpleName());
      assertEquals("TestProducerZebra", producers.get(2).getClass().getSimpleName());
   }



   /*******************************************************************************
    ** Test that the system property disables the class name tiebreaker, restoring
    ** the previous (undefined) behavior.
    *******************************************************************************/
   @Test
   void testDisableNameTiebreakerSystemProperty()
   {
      System.setProperty(DISABLE_NAME_TIEBREAKER_PROPERTY, "true");

      MetaDataProducerInterface<?> producerZ = new TestProducerZebra();
      MetaDataProducerInterface<?> producerA = new TestProducerAlpha();

      /////////////////////////////////////////////////////////////////////////////
      // with tiebreaker disabled, order should be based on insertion order      //
      // (since sortOrder and type are equal, and no further comparator applied) //
      /////////////////////////////////////////////////////////////////////////////
      List<MetaDataProducerInterface<?>> producers = new ArrayList<>(List.of(producerZ, producerA));
      MetaDataProducerHelper.sortMetaDataProducers(producers);

      //////////////////////////////////////////////////////////////////////////////
      // the order should remain as inserted (Z, A) since there's no tie-breaker. //
      // note: this relies on stable sort behavior in Java                        //
      //////////////////////////////////////////////////////////////////////////////
      assertEquals("TestProducerZebra", producers.get(0).getClass().getSimpleName());
      assertEquals("TestProducerAlpha", producers.get(1).getClass().getSimpleName());
   }



   /*******************************************************************************
    ** Test that sortOrder still takes precedence over class name.
    *******************************************************************************/
   @Test
   void testSortOrderTakesPrecedenceOverClassName()
   {
      ///////////////////////////////////////////////////////////////////////////
      // producerZ has lower sortOrder, so should come first despite "Z" > "A" //
      ///////////////////////////////////////////////////////////////////////////
      MetaDataProducerInterface<?> producerZ = new TestProducerZebra()
      {
         @Override
         public int getSortOrder()
         {
            return 100;
         }
      };
      MetaDataProducerInterface<?> producerA = new TestProducerAlpha()
      {
         @Override
         public int getSortOrder()
         {
            return 200;
         }
      };

      List<MetaDataProducerInterface<?>> producers = new ArrayList<>(List.of(producerA, producerZ));
      MetaDataProducerHelper.sortMetaDataProducers(producers);

      //////////////////////////////////////////////////////////////
      // Z should come first because it has lower sortOrder (100) //
      //////////////////////////////////////////////////////////////
      assertEquals(100, producers.get(0).getSortOrder());
      assertEquals(200, producers.get(1).getSortOrder());
   }



   /*******************************************************************************
    ** Test that when simple names are equal, full class name is used as tiebreaker.
    ** This tests the scenario mentioned in the review: com.foo.MyProducer vs com.bar.MyProducer
    *******************************************************************************/
   @Test
   void testFullClassNameTiebreakerWhenSimpleNamesMatch()
   {
      /////////////////////////////////////////////////////////////////////////////////
      // create two producers from different inner classes that have the same simple //
      // name pattern (anonymous classes extending the same base)                    //
      // We'll use the outer class structure to create predictable full names        //
      /////////////////////////////////////////////////////////////////////////////////
      MetaDataProducerInterface<?> producerFromAlpha = new TestProducerAlpha() {};
      MetaDataProducerInterface<?> producerFromZebra = new TestProducerZebra() {};

      //////////////////////////////////////////////////////////////////////////////
      // both are anonymous classes, so their simple names will be empty strings. //
      // the full name will include the outer class and a number suffix.          //
      // this exercises the full-name tiebreaker when simple names are equal.     //
      //////////////////////////////////////////////////////////////////////////////
      assertEquals(producerFromAlpha.getClass().getSimpleName(), producerFromZebra.getClass().getSimpleName());

      List<MetaDataProducerInterface<?>> producers = new ArrayList<>(List.of(producerFromZebra, producerFromAlpha));
      MetaDataProducerHelper.sortMetaDataProducers(producers);

      /////////////////////////////////////////////////////////////////////////////////////
      // after sorting, they should be in a deterministic order based on full class name //
      // the key assertion is that sorting is stable and deterministic                   //
      /////////////////////////////////////////////////////////////////////////////////////
      String firstName  = producers.get(0).getClass().getName();
      String secondName = producers.get(1).getClass().getName();
      assertTrue(firstName.compareTo(secondName) <= 0,
         "Expected first producer's full name [" + firstName + "] to sort before or equal to second [" + secondName + "]");
   }


   /*******************************************************************************
    ** Fail-fast is off by default, and is turned on by either the QInstance flag
    ** or the system property.
    *******************************************************************************/
   @Test
   void testIsFailOnProducerError()
   {
      assertFalse(MetaDataProducerHelper.isFailOnProducerError(null));
      assertFalse(MetaDataProducerHelper.isFailOnProducerError(new QInstance()));
      assertFalse(new QInstance().getFailOnMetaDataProducerError());

      assertTrue(MetaDataProducerHelper.isFailOnProducerError(new QInstance().withFailOnMetaDataProducerError(true)));

      QInstance qInstance = new QInstance();
      qInstance.setFailOnMetaDataProducerError(true);
      assertTrue(qInstance.getFailOnMetaDataProducerError());
      assertTrue(MetaDataProducerHelper.isFailOnProducerError(qInstance));

      System.setProperty(FAIL_ON_PRODUCER_ERROR_PROPERTY, "true");
      assertTrue(MetaDataProducerHelper.isFailOnProducerError(null));
      assertTrue(MetaDataProducerHelper.isFailOnProducerError(new QInstance()));
      assertTrue(MetaDataProducerHelper.isFailOnProducerError(new QInstance().withFailOnMetaDataProducerError(false)));

      System.setProperty(FAIL_ON_PRODUCER_ERROR_PROPERTY, "false");
      assertFalse(MetaDataProducerHelper.isFailOnProducerError(new QInstance()));
   }



   /*******************************************************************************
    ** By default, a producer that throws from produce is logged as a warning,
    ** and the producers after it still run.
    *******************************************************************************/
   @Test
   void testProduceErrorIsLoggedByDefault() throws QException
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(MetaDataProducerHelper.class);

      QInstance qInstance = new QInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, PRODUCE_ERRORS_PACKAGE);

      assertTrue(qInstance.getTables().containsKey(TestWorkingMetaDataProducer.NAME));
      assertTrue(collectingLogger.getCollectedMessages().stream().anyMatch(m -> m.getMessage().contains("error executing metaDataProducer")));
   }



   /*******************************************************************************
    ** With the QInstance flag on, a producer that throws from produce stops
    ** processing with a QException naming the producer.
    *******************************************************************************/
   @Test
   void testProduceErrorThrowsWithInstanceFlag()
   {
      QInstance  qInstance = new QInstance().withFailOnMetaDataProducerError(true);
      QException exception = assertThrows(QException.class, () -> MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, PRODUCE_ERRORS_PACKAGE));

      assertThat(exception.getMessage()).contains(TestThrowsInProduceMetaDataProducer.class.getName());
      assertEquals(TestThrowsInProduceMetaDataProducer.MESSAGE, exception.getCause().getMessage());
      assertFalse(qInstance.getTables().containsKey(TestWorkingMetaDataProducer.NAME));
   }



   /*******************************************************************************
    ** With the system property on, a producer that throws from produce stops
    ** processing with a QException.
    *******************************************************************************/
   @Test
   void testProduceErrorThrowsWithSystemProperty()
   {
      System.setProperty(FAIL_ON_PRODUCER_ERROR_PROPERTY, "true");

      QInstance  qInstance = new QInstance();
      QException exception = assertThrows(QException.class, () -> MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, PRODUCE_ERRORS_PACKAGE));

      assertThat(exception.getMessage()).contains(TestThrowsInProduceMetaDataProducer.class.getName());
      assertFalse(qInstance.getTables().containsKey(TestWorkingMetaDataProducer.NAME));
   }



   /*******************************************************************************
    ** By default, a class that fails while being evaluated as a producer (here,
    ** its constructor throws) is logged as a warning and skipped.
    *******************************************************************************/
   @Test
   void testConstructorErrorIsLoggedByDefault() throws QException
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(MetaDataProducerHelper.class);

      assertEquals(0, MetaDataProducerHelper.findProducers(CONSTRUCTOR_THROWS_PACKAGE).size());

      QInstance qInstance = new QInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, CONSTRUCTOR_THROWS_PACKAGE);
      assertFalse(qInstance.getTables().containsKey(TestThrowsInConstructorMetaDataProducer.NAME));

      assertTrue(collectingLogger.getCollectedMessages().stream().anyMatch(m -> m.getMessage().contains("Error evaluating a possible meta-data producer class")));
   }



   /*******************************************************************************
    ** With the QInstance flag on, a class that fails while being evaluated as a
    ** producer stops processing with a QException naming the class.
    *******************************************************************************/
   @Test
   void testConstructorErrorThrowsWithInstanceFlag()
   {
      QInstance  qInstance = new QInstance().withFailOnMetaDataProducerError(true);
      QException exception = assertThrows(QException.class, () -> MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, CONSTRUCTOR_THROWS_PACKAGE));

      assertThat(exception.getMessage()).contains(TestThrowsInConstructorMetaDataProducer.class.getName());
      Throwable rootCause = ExceptionUtils.getRootCause(exception);
      assertInstanceOf(IllegalStateException.class, rootCause);
      assertEquals(TestThrowsInConstructorMetaDataProducer.MESSAGE, rootCause.getMessage());
   }



   /*******************************************************************************
    ** With the system property on, findProducers (which takes no QInstance)
    ** throws for a class that fails while being evaluated as a producer.
    *******************************************************************************/
   @Test
   void testConstructorErrorThrowsWithSystemProperty()
   {
      System.setProperty(FAIL_ON_PRODUCER_ERROR_PROPERTY, "true");

      QException exception = assertThrows(QException.class, () -> MetaDataProducerHelper.findProducers(CONSTRUCTOR_THROWS_PACKAGE));
      assertThat(exception.getMessage()).contains(TestThrowsInConstructorMetaDataProducer.class.getName());
   }



   /***************************************************************************
    * Classes that MetaDataProducerHelper finds but can't use as producers,
    * without any exception being thrown - with the warning it logs for each.
    ***************************************************************************/
   static Stream<Arguments> unusableProducerClasses()
   {
      return (Stream.of(
         Arguments.of(TestOnlyArgConstructorMetaDataProducer.class, "does not have a no-arg constructor"),
         Arguments.of(TestNotRecordEntity.class, "but which is not a QRecordEntity"),
         Arguments.of(TestTableNameNotStringEntity.class, "whose TABLE_NAME field is not a String"),
         Arguments.of(TestNotPossibleValueEnum.class, "but which is not a PossibleValueEnum"),
         Arguments.of(TestChildWidgetWithoutJoinEntity.class, "requested to produce a ChildRecordListWidget, but not produce a Join"),
         Arguments.of(TestChildJoinWithoutFieldEntity.class, "Could not find field in")
      ));
   }



   /*******************************************************************************
    ** By default, a class that can't be used as a producer is logged as a
    ** warning, and what it would have produced is dropped.
    *******************************************************************************/
   @ParameterizedTest
   @MethodSource("unusableProducerClasses")
   void testUnusableProducerClassIsLoggedByDefault(Class<?> unusableClass, String expectedWarning) throws QException
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(MetaDataProducerHelper.class);

      QInstance qInstance = new QInstance();
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, unusableClass.getPackageName());

      assertTrue(qInstance.getTables().isEmpty());
      assertTrue(qInstance.getPossibleValueSources().isEmpty());
      assertTrue(qInstance.getJoins().isEmpty());
      assertTrue(qInstance.getWidgets().isEmpty());
      assertTrue(collectingLogger.getCollectedMessages().stream().anyMatch(m -> Level.WARN.equals(m.getLevel()) && m.getMessage().contains(expectedWarning)));
   }



   /*******************************************************************************
    ** With the QInstance flag on, a class that can't be used as a producer stops
    ** processing with a QException naming the class, caused by the message that
    ** would have been the warning.
    *******************************************************************************/
   @ParameterizedTest
   @MethodSource("unusableProducerClasses")
   void testUnusableProducerClassThrowsWithInstanceFlag(Class<?> unusableClass, String expectedWarning)
   {
      QInstance  qInstance = new QInstance().withFailOnMetaDataProducerError(true);
      QException exception = assertThrows(QException.class, () -> MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, unusableClass.getPackageName()));

      assertThat(exception.getMessage()).contains(unusableClass.getName());
      assertThat(ExceptionUtils.getRootCause(exception).getMessage()).contains(expectedWarning);
   }



   /*******************************************************************************
    ** With the system property on, findProducers (which takes no QInstance)
    ** throws for a class that can't be used as a producer.
    *******************************************************************************/
   @ParameterizedTest
   @MethodSource("unusableProducerClasses")
   void testUnusableProducerClassThrowsWithSystemProperty(Class<?> unusableClass, String expectedWarning)
   {
      System.setProperty(FAIL_ON_PRODUCER_ERROR_PROPERTY, "true");

      QException exception = assertThrows(QException.class, () -> MetaDataProducerHelper.findProducers(unusableClass.getPackageName()));
      assertThat(exception.getMessage()).contains(unusableClass.getName());
      assertThat(ExceptionUtils.getRootCause(exception).getMessage()).contains(expectedWarning);
   }



   /*******************************************************************************
    ** A QBitMetaDataProducer finds its producers using the QInstance it is
    ** producing into, so the QInstance flag applies to the qbit's package too.
    *******************************************************************************/
   @Test
   void testQBitProducerUsesInstanceFlag() throws QException
   {
      MetaDataProducerMultiOutput output = new FailingQBitMetaDataProducer().produce(new QInstance());
      assertEquals(0, output.getEach(QTableMetaData.class).size());

      QInstance  qInstance = new QInstance().withFailOnMetaDataProducerError(true);
      QException exception = assertThrows(QException.class, () -> new FailingQBitMetaDataProducer().produce(qInstance));
      assertThat(exception.getMessage()).contains(TestThrowsInConstructorMetaDataProducer.class.getName());
   }



   /*******************************************************************************
    ** The static table meta-data customizer is cleared even when processing
    ** throws, so it doesn't leak into the next call.
    *******************************************************************************/
   @Test
   void testTableMetaDataCustomizerClearedWhenProcessingThrows()
   {
      QInstance qInstance = new QInstance().withFailOnMetaDataProducerError(true);
      assertThrows(QException.class, () -> MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, PRODUCE_ERRORS_PACKAGE, (instance, table) -> table));
      assertNull(new MetaDataProducerHelper().getTableMetaDataCustomizer());
   }



   /***************************************************************************
    * Test qbit producer, whose producers come from the package with a class
    * that fails while being evaluated as a producer.
    ***************************************************************************/
   private static class FailingQBitMetaDataProducer implements QBitMetaDataProducer<QBitConfig>
   {
      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public QBitConfig getQBitConfig()
      {
         return (new QBitConfig()
         {
         });
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public QBitMetaData getQBitMetaData()
      {
         return (new QBitMetaData()
            .withGroupId("test.com.kingsrook.qbits")
            .withArtifactId("failingQBit")
            .withVersion("0.1.0"));
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public String getPackageNameForFindingMetaDataProducers()
      {
         return (CONSTRUCTOR_THROWS_PACKAGE);
      }
   }




   /***************************************************************************
    * Test producer class - named to sort first alphabetically
    ***************************************************************************/
   private static class TestProducerAlpha implements MetaDataProducerInterface<QTableMetaData>
   {
      @Override
      public QTableMetaData produce(QInstance qInstance)
      {
         return new QTableMetaData().withName("alpha");
      }
   }



   /***************************************************************************
    * Test producer class - named to sort in the middle alphabetically
    ***************************************************************************/
   private static class TestProducerMango implements MetaDataProducerInterface<QTableMetaData>
   {
      @Override
      public QTableMetaData produce(QInstance qInstance)
      {
         return new QTableMetaData().withName("mango");
      }
   }



   /***************************************************************************
    * Test producer class - named to sort last alphabetically
    ***************************************************************************/
   private static class TestProducerZebra implements MetaDataProducerInterface<QTableMetaData>
   {
      @Override
      public QTableMetaData produce(QInstance qInstance)
      {
         return new QTableMetaData().withName("zebra");
      }
   }

}