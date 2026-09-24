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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Table extension hooks exercised against the sample's native Field Lab store.
 *******************************************************************************/
class SampleCustomizerContractTest
{
   private static final String TABLE = "fieldLab";
   private static final List<String> EVENTS = new ArrayList<>();
   private static String mode;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
      EVENTS.clear();
      mode = "ordinary";
      for(TableCustomizers customizer : TableCustomizers.values())
      {
         QContext.getQInstance().getTable(TABLE).withCustomizer(customizer, new QCodeReference(Hooks.class));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      EVENTS.clear();
      mode = null;
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWriteReadAndMetadataHooks() throws Exception
   {
      QRecord inserted = insert("Original");
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      Integer id = inserted.getValueInteger("id");
      assertEquals("Original inserted", storedName(id));
      assertTrue(EVENTS.indexOf("preInsert") < EVENTS.indexOf("postInsert"));
      QRecord updated = new UpdateAction().execute(new UpdateInput(TABLE)
         .withRecord(new QRecord().withValue("id", id).withValue("name", "Changed"))).getRecords().get(0);
      assertTrue(updated.getErrors().isEmpty(), updated.getErrorsAsString());
      assertEquals("Changed updated", storedName(id));
      assertTrue(EVENTS.indexOf("preUpdate") < EVENTS.indexOf("postUpdate"));
      assertEquals("Presented Changed updated", GetAction.execute(TABLE, id).getValueString("name"));
      assertEquals("Changed updated", storedName(id));
      TableMetaDataInput metadata = new TableMetaDataInput();
      metadata.setTableName(TABLE);
      assertTrue(new TableMetaDataAction().execute(metadata).getTable().getFields().containsKey("sampleHint"));
      assertFalse(QContext.getQInstance().getTable(TABLE).getFields().containsKey("sampleHint"));
      assertEquals(1, new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(id)).getDeletedRecordCount());
      assertEquals(null, storedName(id));
      assertTrue(EVENTS.indexOf("preDelete") < EVENTS.indexOf("postDelete"));
      assertTrue(EVENTS.containsAll(List.of("preInsert", "postInsert", "preUpdate", "postUpdate", "postQuery", "preDelete", "postDelete", "metadata")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRejectedAndThrowingCustomizerCannotWrite() throws Exception
   {
      mode = "reject";
      QRecord rejected = insert("Rejected");
      assertFalse(rejected.getErrors().isEmpty());
      assertEquals(0, storedCount());
      mode = "throw";
      assertThrows(QException.class, () -> insert("Thrown"));
      assertEquals(0, storedCount());
      mode = "ordinary";
      assertTrue(insert("Recovery").getErrors().isEmpty());
      assertEquals(1, storedCount());
      assertEquals(TABLE, QContext.getQInstance().getTable(TABLE).getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWrongCustomizerTypeIsRejected() throws Exception
   {
      QInstance invalid = SampleMetaDataProvider.defineTestInstance();
      invalid.getTable(TABLE).withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(String.class));
      assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(invalid));
      assertEquals(0, storedCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insert(String name) throws QException
   {
      return new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord().withValue("name", name))).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String storedName(Integer id) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT name FROM field_lab WHERE id = " + id))
      {
         return rows.next() ? rows.getString(1) : null;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int storedCount() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM field_lab"))
      {
         assertTrue(rows.next());
         return rows.getInt(1);
      }
   }



   /*******************************************************************************
    ** Observable sample customization; mutations and presentation stay distinct.
    *******************************************************************************/
   public static class Hooks implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean preview) throws QException
      {
         EVENTS.add("preInsert");
         if("throw".equals(mode))
         {
            throw new QException("Owned sample customizer failure");
         }
         for(QRecord record : records)
         {
            if("reject".equals(mode))
            {
               record.addError(new BadInputStatusMessage("Owned sample rejection"));
            }
            else
            {
               record.setValue("name", record.getValueString("name") + " inserted");
            }
         }
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput input, List<QRecord> records)
      {
         EVENTS.add("postInsert");
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords)
      {
         EVENTS.add("preUpdate");
         records.forEach(record -> record.setValue("name", record.getValueString("name") + " updated"));
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postUpdate(UpdateInput input, List<QRecord> records, Optional<List<QRecord>> oldRecords)
      {
         EVENTS.add("postUpdate");
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         EVENTS.add("postQuery");
         records.forEach(record -> record.setValue("name", "Presented " + record.getValueString("name")));
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean preview)
      {
         EVENTS.add("preDelete");
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postDelete(DeleteInput input, List<QRecord> records)
      {
         EVENTS.add("postDelete");
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void postMetaDataAction(TableMetaDataInput input, TableMetaDataOutput output)
      {
         EVENTS.add("metadata");
         output.getTable().getFields().put("sampleHint", new QFrontendFieldMetaData(new QFieldMetaData("sampleHint", QFieldType.STRING)));
      }
   }
}
