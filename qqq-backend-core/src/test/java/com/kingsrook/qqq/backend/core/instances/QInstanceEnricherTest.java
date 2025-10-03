/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.enrichment.testplugins.TestEnricherPlugin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.utils.TestUtils.APP_NAME_GREETINGS;
import static com.kingsrook.qqq.backend.core.utils.TestUtils.APP_NAME_MISCELLANEOUS;
import static com.kingsrook.qqq.backend.core.utils.TestUtils.APP_NAME_PEOPLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QInstanceEnricher
 **
 *******************************************************************************/
class QInstanceEnricherTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QInstanceEnricher.removeAllEnricherPlugins();
   }



   /*******************************************************************************
    ** Test that a table missing a label gets the default label applied (name w/ UC-first).
    **
    *******************************************************************************/
   @Test
   public void test_nullTableLabelComesFromName()
   {
      QInstance      qInstance   = TestUtils.defineInstance();
      QTableMetaData personTable = qInstance.getTable("person");
      personTable.setLabel(null);
      assertNull(personTable.getLabel());
      new QInstanceEnricher(qInstance).enrich();
      assertEquals("Person", personTable.getLabel());
   }



   /*******************************************************************************
    ** Test that a table missing a label and a name doesn't NPE, but just keeps
    ** the name & label both null.
    **
    *******************************************************************************/
   @Test
   public void test_nullNameGivesNullLabel()
   {
      QInstance      qInstance   = TestUtils.defineInstance();
      QTableMetaData personTable = qInstance.getTable("person");
      personTable.setLabel(null);
      personTable.setName(null);
      assertNull(personTable.getLabel());
      assertNull(personTable.getName());
      new QInstanceEnricher(qInstance).enrich();
      assertNull(personTable.getLabel());
      assertNull(personTable.getName());
   }



   /*******************************************************************************
    ** Test that a field missing a label gets the default label applied (name w/ UC-first)
    **
    *******************************************************************************/
   @Test
   public void test_nullFieldLabelComesFromName()
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QFieldMetaData idField   = qInstance.getTable("person").getField("id");
      idField.setLabel(null);
      assertNull(idField.getLabel());
      new QInstanceEnricher(qInstance).enrich();
      assertEquals("Id", idField.getLabel());
   }



   /*******************************************************************************
    ** Test that a field missing a label gets the default label applied (name w/ UC-first)
    ** w/ Id stripped from the end, because it's a PVS
    **
    *******************************************************************************/
   @Test
   public void test_nullFieldLabelComesFromNameWithoutIdForPossibleValues()
   {
      QInstance      qInstance        = TestUtils.defineInstance();
      QFieldMetaData homeStateIdField = qInstance.getTable("person").getField("homeStateId");
      assertNull(homeStateIdField.getLabel());
      new QInstanceEnricher(qInstance).enrich();
      assertEquals("Home State", homeStateIdField.getLabel());
   }



   /*******************************************************************************
    ** Test that a fieldSection missing a label gets the default label applied (name w/ UC-first).
    **
    *******************************************************************************/
   @Test
   public void test_nullFieldSectionLabelComesFromName()
   {
      QInstance      qInstance   = TestUtils.defineInstance();
      QTableMetaData personTable = qInstance.getTable("person");
      personTable.setSections(List.of(new QFieldSection()
         .withName("test")
         .withTier(Tier.T1)
         .withFieldNames(new ArrayList<>(personTable.getFields().keySet()))
      ));

      new QInstanceEnricher(qInstance).enrich();
      assertEquals("Test", personTable.getSections().get(0).getLabel());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetInferredFieldBackendNames()
   {
      QTableMetaData table = new QTableMetaData()
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("firstName", QFieldType.INTEGER))
         .withField(new QFieldMetaData("nonstandard", QFieldType.INTEGER).withBackendName("whateverImNon_standard"));
      QInstanceEnricher.setInferredFieldBackendNames(table);
      assertEquals("id", table.getField("id").getBackendName());
      assertEquals("first_name", table.getField("firstName").getBackendName());
      assertEquals("whateverImNon_standard", table.getField("nonstandard").getBackendName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetInferredFieldBackendNamesEdgeCases()
   {
      ///////////////////////////////////////////////////////////////
      // make sure none of these cases throw (but all should warn) //
      ///////////////////////////////////////////////////////////////
      QInstanceEnricher.setInferredFieldBackendNames(null);
      QInstanceEnricher.setInferredFieldBackendNames(new QTableMetaData());
      QInstanceEnricher.setInferredFieldBackendNames(new QTableMetaData().withFields(Collections.emptyMap()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNameToLabel()
   {
      assertEquals("Address 2", QInstanceEnricher.nameToLabel("address2"));
      assertEquals("Field 20", QInstanceEnricher.nameToLabel("field20"));
      assertEquals("Something USA", QInstanceEnricher.nameToLabel("somethingUSA"));
      assertEquals("Number 1 Dad", QInstanceEnricher.nameToLabel("number1Dad"));
      assertEquals("Number 417 Dad", QInstanceEnricher.nameToLabel("number417Dad"));

      assertEquals("Default Wms System Id", QInstanceEnricher.nameToLabel("defaultWmsSystemId"));
      QInstanceEnricher.addLabelMapping("\\bWms\\b", "WMS");
      assertEquals("Default WMS System Id", QInstanceEnricher.nameToLabel("defaultWmsSystemId"));
      QInstanceEnricher.clearLabelMappings();

      assertEquals("Api Client Id", QInstanceEnricher.nameToLabel("apiClientId"));
      QInstanceEnricher.addLabelMapping("\\bApi\\b", "API");
      assertEquals("API Client Id", QInstanceEnricher.nameToLabel("apiClientId"));
      QInstanceEnricher.clearLabelMappings();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInferBackendName()
   {
      assertEquals("id", QInstanceEnricher.inferBackendName("id"));
      assertEquals("word_another_word_more_words", QInstanceEnricher.inferBackendName("wordAnotherWordMoreWords"));
      assertEquals("l_ul_ul_ul", QInstanceEnricher.inferBackendName("lUlUlUl"));
      assertEquals("starts_upper", QInstanceEnricher.inferBackendName("StartsUpper"));
      assertEquals("tla_first", QInstanceEnricher.inferBackendName("TLAFirst"));
      assertEquals("word_then_tla_in_middle", QInstanceEnricher.inferBackendName("wordThenTLAInMiddle"));
      assertEquals("end_with_tla", QInstanceEnricher.inferBackendName("endWithTLA"));
      assertEquals("tla_and_another_tla", QInstanceEnricher.inferBackendName("TLAAndAnotherTLA"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInferNameFromBackendName()
   {
      assertEquals("id", QInstanceEnricher.inferNameFromBackendName("id"));
      assertEquals("wordAnotherWordMoreWords", QInstanceEnricher.inferNameFromBackendName("word_another_word_more_words"));
      assertEquals("lUlUlUl", QInstanceEnricher.inferNameFromBackendName("l_ul_ul_ul"));
      assertEquals("tlaFirst", QInstanceEnricher.inferNameFromBackendName("tla_first"));
      assertEquals("wordThenTlaInMiddle", QInstanceEnricher.inferNameFromBackendName("word_then_tla_in_middle"));
      assertEquals("endWithTla", QInstanceEnricher.inferNameFromBackendName("end_with_tla"));
      assertEquals("tlaAndAnotherTla", QInstanceEnricher.inferNameFromBackendName("tla_and_another_tla"));
      assertEquals("allCaps", QInstanceEnricher.inferNameFromBackendName("ALL_CAPS"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGenerateAppSections()
   {
      QInstance qInstance = TestUtils.defineInstance();
      new QInstanceEnricher(qInstance).enrich();
      assertNotNull(qInstance.getApp(APP_NAME_GREETINGS).getSections());
      assertEquals(1, qInstance.getApp(APP_NAME_GREETINGS).getSections().size(), "App should automatically have one section");
      assertEquals(0, qInstance.getApp(APP_NAME_GREETINGS).getSections().get(0).getTables().size(), "Section should not have tables");
      assertEquals(2, qInstance.getApp(APP_NAME_GREETINGS).getSections().get(0).getProcesses().size(), "Section should have two processes");
      assertEquals(qInstance.getApp(APP_NAME_GREETINGS).getName(), qInstance.getApp(APP_NAME_GREETINGS).getSections().get(0).getName(), "Section name should default to app's");

      assertNotNull(qInstance.getApp(APP_NAME_PEOPLE).getSections());
      assertEquals(1, qInstance.getApp(APP_NAME_PEOPLE).getSections().size(), "App should automatically have one section");
      assertEquals(2, qInstance.getApp(APP_NAME_PEOPLE).getSections().get(0).getTables().size(), "Section should have two tables");
      assertEquals(0, qInstance.getApp(APP_NAME_PEOPLE).getSections().get(0).getProcesses().size(), "Section should not have processes");
      assertEquals(qInstance.getApp(APP_NAME_PEOPLE).getName(), qInstance.getApp(APP_NAME_PEOPLE).getSections().get(0).getName(), "Section name should default to app's");

      assertNotNull(qInstance.getApp(APP_NAME_MISCELLANEOUS).getSections());
      assertEquals(1, qInstance.getApp(APP_NAME_MISCELLANEOUS).getSections().size(), "App should automatically have one section");
      assertEquals(1, qInstance.getApp(APP_NAME_MISCELLANEOUS).getSections().get(0).getTables().size(), "Section should have one table");
      assertEquals(1, qInstance.getApp(APP_NAME_MISCELLANEOUS).getSections().get(0).getProcesses().size(), "Section should have one process");
      assertEquals(qInstance.getApp(APP_NAME_MISCELLANEOUS).getName(), qInstance.getApp(APP_NAME_MISCELLANEOUS).getSections().get(0).getName(), "Section name should default to app's");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppSectionMembersBecomeAppChildren()
   {
      QInstance qInstance = new QInstance();
      qInstance.addTable(new QTableMetaData().withName("table1"));
      qInstance.addProcess(new QProcessMetaData().withName("process1"));
      qInstance.addApp(new QAppMetaData().withName("app1")
         .withSection(new QAppSection().withTable("table1").withProcess("process1")));

      /////////////////////////////////////////////////////
      // first, show that the list of children was empty //
      /////////////////////////////////////////////////////
      assertThat(qInstance.getApp("app1").getChildren()).isNullOrEmpty();

      /////////////////////////////
      // now enrich the instance //
      /////////////////////////////
      new QInstanceEnricher(qInstance).enrich();

      ///////////////////////////////////////////////////////////////
      // and now the table & process should be children of the app //
      ///////////////////////////////////////////////////////////////
      assertThat(qInstance.getApp("app1").getChildren())
         .contains(qInstance.getTable("table1"), qInstance.getProcess("process1"));

      //////////////////////////////////////////////////////////////////
      // make sure that re-enhancement doesn't duplicate the children //
      //////////////////////////////////////////////////////////////////
      new QInstanceEnricher(qInstance).enrich();
      assertThat(qInstance.getApp("app1").getChildren()).hasSize(2);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      // add a non-existing table - make sure we don't blow up, and in this case, it won't be added as a child //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      qInstance.getApp("app1").getSections().get(0).withTable("notATable");
      new QInstanceEnricher(qInstance).enrich();
      assertThat(qInstance.getApp("app1").getChildren()).hasSize(2);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInferredRecordLabelFormat()
   {
      QInstance      qInstance = TestUtils.defineInstance();
      QTableMetaData table     = qInstance.getTable("person").withRecordLabelFormat(null).withRecordLabelFields(new ArrayList<>());
      new QInstanceEnricher(qInstance).enrich();
      assertNull(table.getRecordLabelFormat());

      qInstance = TestUtils.defineInstance();
      table = qInstance.getTable("person").withRecordLabelFormat(null).withRecordLabelFields("firstName");
      new QInstanceEnricher(qInstance).enrich();
      assertEquals("%s", table.getRecordLabelFormat());

      qInstance = TestUtils.defineInstance();
      table = qInstance.getTable("person").withRecordLabelFormat(null).withRecordLabelFields("firstName", "lastName");
      new QInstanceEnricher(qInstance).enrich();
      assertEquals("%s %s", table.getRecordLabelFormat());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddTablePvsAdornment()
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // first make sure the adornment doesn't get added for favoriteShapeId, because it isn't in any apps //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QInstance      qInstance       = TestUtils.defineInstance();
         QTableMetaData personTable     = qInstance.getTable("person");
         QFieldMetaData favoriteShapeId = personTable.getField("favoriteShapeId");
         new QInstanceEnricher(qInstance).enrich();
         assertNull(favoriteShapeId.getAdornments());
      }

      ////////////////////////////////////////////////////////////////////
      // then put shape table in an app, re-run, and see it get adorned //
      ////////////////////////////////////////////////////////////////////
      {
         QInstance      qInstance  = TestUtils.defineInstance();
         QTableMetaData shapeTable = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
         QAppMetaData   miscApp    = qInstance.getApp(APP_NAME_MISCELLANEOUS);
         miscApp.addChild(shapeTable);

         QTableMetaData personTable     = qInstance.getTable("person");
         QFieldMetaData favoriteShapeId = personTable.getField("favoriteShapeId");
         new QInstanceEnricher(qInstance).enrich();
         assertNotNull(favoriteShapeId.getAdornments());
         Optional<FieldAdornment> optionalAdornment = favoriteShapeId.getAdornments().stream().filter(a -> a.getType().equals(AdornmentType.LINK)).findFirst();
         assertTrue(optionalAdornment.isPresent());
         FieldAdornment adornment = optionalAdornment.get();
         assertEquals("shape", adornment.getValues().get(AdornmentType.LinkValues.TO_RECORD_FROM_TABLE));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExposedJoinPaths()
   {
      //////////////////////////
      // no join path => fail //
      //////////////////////////
      {
         QInstance qInstance = TestUtils.defineInstance();
         qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin().withJoinTable("B")));
         qInstance.addTable(newTable("B", "id", "aId"));
         assertThatThrownBy(() -> new QInstanceEnricher(qInstance).enrich())
            .rootCause()
            .hasMessageContaining("Could not infer a joinPath for table [A], exposedJoin to [B]")
            .hasMessageContaining("No join connections between these tables exist in this instance.");
      }

      /////////////////////////////////
      // multiple join paths => fail //
      /////////////////////////////////
      {
         QInstance qInstance = TestUtils.defineInstance();
         qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin().withJoinTable("B")));
         qInstance.addTable(newTable("B", "id", "aId1", "aId2"));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB1").withJoinOn(new JoinOn("id", "aId1")).withType(JoinType.ONE_TO_ONE));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB2").withJoinOn(new JoinOn("id", "aId2")).withType(JoinType.ONE_TO_ONE));
         assertThatThrownBy(() -> new QInstanceEnricher(qInstance).enrich())
            .rootCause()
            .hasMessageContaining("Could not infer a joinPath for table [A], exposedJoin to [B]")
            .hasMessageContaining("2 join connections exist")
            .hasMessageContaining("\nAB1\n")
            .hasMessageContaining("\nAB2.");

         ////////////////////////////////////////////
         // but if you specify a path, you're good //
         ////////////////////////////////////////////
         qInstance.getTable("A").getExposedJoins().get(0).setJoinPath(List.of("AB2"));
         new QInstanceEnricher(qInstance).enrich();
         assertEquals("B", qInstance.getTable("A").getExposedJoins().get(0).getLabel());
      }

      /////////////////////////////////
      // multiple join paths => fail //
      /////////////////////////////////
      {
         QInstance qInstance = TestUtils.defineInstance();
         qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin().withJoinTable("C")));
         qInstance.addTable(newTable("B", "id", "aId"));
         qInstance.addTable(newTable("C", "id", "bId", "aId"));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB").withJoinOn(new JoinOn("id", "aId")).withType(JoinType.ONE_TO_ONE));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("B").withRightTable("C").withName("BC").withJoinOn(new JoinOn("id", "bId")).withType(JoinType.ONE_TO_ONE));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("C").withName("AC").withJoinOn(new JoinOn("id", "aId")).withType(JoinType.ONE_TO_ONE));
         assertThatThrownBy(() -> new QInstanceEnricher(qInstance).enrich())
            .rootCause()
            .hasMessageContaining("Could not infer a joinPath for table [A], exposedJoin to [C]")
            .hasMessageContaining("2 join connections exist")
            .hasMessageContaining("\nAB, BC\n")
            .hasMessageContaining("\nAC.");

         ////////////////////////////////////////////
         // but if you specify a path, you're good //
         ////////////////////////////////////////////
         qInstance.getTable("A").getExposedJoins().get(0).setJoinPath(List.of("AB", "BC"));
         new QInstanceEnricher(qInstance).enrich();
         assertEquals("C", qInstance.getTable("A").getExposedJoins().get(0).getLabel());
      }

      //////////////////////////////////////////////////////////////////////////////////////
      // even if you specify a bogus path, Enricher doesn't care - see validator to care. //
      //////////////////////////////////////////////////////////////////////////////////////
      {
         QInstance qInstance = TestUtils.defineInstance();
         qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin().withJoinTable("B").withJoinPath(List.of("not-a-join"))));
         qInstance.addTable(newTable("B", "id", "aId"));
         new QInstanceEnricher(qInstance).enrich();
      }

      ////////////////////////////////////
      // one join path => great success //
      ////////////////////////////////////
      {
         QInstance qInstance = TestUtils.defineInstance();
         qInstance.addTable(newTable("A", "id")
            .withExposedJoin(new ExposedJoin().withJoinTable("B"))
            .withExposedJoin(new ExposedJoin().withJoinTable("C")));
         qInstance.addTable(newTable("B", "id", "aId"));
         qInstance.addTable(newTable("C", "id", "bId"));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB").withJoinOn(new JoinOn("id", "aId")).withType(JoinType.ONE_TO_ONE));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("B").withRightTable("C").withName("BC").withJoinOn(new JoinOn("id", "bId")).withType(JoinType.ONE_TO_ONE));

         new QInstanceEnricher(qInstance).enrich();

         ExposedJoin exposedJoinAB = qInstance.getTable("A").getExposedJoins().get(0);
         assertEquals("B", exposedJoinAB.getLabel());
         assertEquals(List.of("AB"), exposedJoinAB.getJoinPath());

         ExposedJoin exposedJoinAC = qInstance.getTable("A").getExposedJoins().get(1);
         assertEquals("C", exposedJoinAC.getLabel());
         assertEquals(List.of("AB", "BC"), exposedJoinAC.getJoinPath());
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData newTable(String tableName, String... fieldNames)
   {
      QTableMetaData tableMetaData = new QTableMetaData()
         .withName(tableName)
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField(fieldNames[0]);

      for(String fieldName : fieldNames)
      {
         tableMetaData.addField(new QFieldMetaData(fieldName, QFieldType.INTEGER));
      }

      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCreateDateAndModifyDateBehaviors()
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.addTable(newTable("A", "id", "createDate", "modifyDate"));
      QTableMetaData table = qInstance.getTable("A");

      ////////////////////////////////////////////////
      // make sure behavior wasn't there by default //
      ////////////////////////////////////////////////
      assertNull(table.getField("createDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertNull(table.getField("modifyDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));

      //////////////////////////////////////////////////////////////////
      // make sure if config'ing off the adding of the behavior works //
      //////////////////////////////////////////////////////////////////
      new QInstanceEnricher(qInstance)
         .withConfigAddDynamicDefaultValuesToFieldsNamedCreateDateAndModifyDate(false)
         .enrich();
      assertNull(table.getField("createDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertNull(table.getField("modifyDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));

      /////////////////////////////////////////////////////////////////////////////////////////////
      // make sure default value for the config (e.g., in a new enricher) is to add the behavior //
      /////////////////////////////////////////////////////////////////////////////////////////////
      new QInstanceEnricher(qInstance).enrich();
      assertEquals(DynamicDefaultValueBehavior.CREATE_DATE, table.getField("createDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
      assertEquals(DynamicDefaultValueBehavior.MODIFY_DATE, table.getField("modifyDate").getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOptionalProcessSteps()
   {
      QInstance        qInstance = TestUtils.defineInstance();
      QProcessMetaData process   = new QProcessMetaData();
      process.setName("test");
      process.withStepList(List.of(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(TestUtils.IncreaseBirthdateStep.class))));
      process.withOptionalStep(new QFrontendStepMetaData()
         .withName("screen")
         .withViewField(new QFieldMetaData("myField", QFieldType.STRING)));
      qInstance.addProcess(process);

      new QInstanceEnricher(qInstance).enrich();

      assertEquals("My Field", qInstance.getProcess("test").getFrontendStep("screen").getViewFields().get(0).getLabel());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldPlugIn()
   {
      QInstance qInstance = TestUtils.defineInstance();

      QInstanceEnricher.addEnricherPlugin(new TestEnricherPlugin(true));

      new QInstanceEnricher(qInstance).enrich();

      qInstance.getTables().values().forEach(table -> table.getFields().values().forEach(field -> assertThat(field.getLabel()).endsWith("Plugged")));
      qInstance.getProcesses().values().forEach(process -> process.getInputFields().forEach(field -> assertThat(field.getLabel()).endsWith("Plugged")));
      qInstance.getProcesses().values().forEach(process -> process.getOutputFields().forEach(field -> assertThat(field.getLabel()).endsWith("Plugged")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPluginIsEnabled()
   {
      /////////////////////////////////////////////////////////////////////
      // add the plugin - but in a disabled state (param to constructor) //
      /////////////////////////////////////////////////////////////////////
      QInstanceEnricher.addEnricherPlugin(new TestEnricherPlugin(false));

      QInstance qInstance = TestUtils.defineInstance();
      new QInstanceEnricher(qInstance).enrich();

      qInstance.getTables().values().forEach(table -> table.getFields().values().forEach(field -> assertThat(field.getLabel()).doesNotEndWith("Plugged")));
      qInstance.getProcesses().values().forEach(process -> process.getInputFields().forEach(field -> assertThat(field.getLabel()).doesNotEndWith("Plugged")));
      qInstance.getProcesses().values().forEach(process -> process.getOutputFields().forEach(field -> assertThat(field.getLabel()).doesNotEndWith("Plugged")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDiscoverAndAddPlugins() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new QInstanceEnricher(qInstance).enrich();
      qInstance.getTables().values().forEach(table -> table.getFields().values().forEach(field -> assertThat(field.getLabel()).doesNotEndWith("Plugged")));

      qInstance = TestUtils.defineInstance();
      QInstanceEnricher.discoverAndAddPluginsInPackage(getClass().getPackageName() + ".enrichment.testplugins");
      new QInstanceEnricher(qInstance).enrich();
      qInstance.getTables().values().forEach(table -> table.getFields().values().forEach(field -> assertThat(field.getLabel()).endsWith("Plugged")));
   }

}
