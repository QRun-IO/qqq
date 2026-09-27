/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Native key identity must survive copied binary values and decimal scale changes
 ** during prefetch, validation and recursive deletion bookkeeping.
 *******************************************************************************/
class DeleteTypedPrimaryKeyTest extends RDBMSActionTest
{
   private static final String PARENT = "typedDeleteParent";
   private static final String CHILD = "typedDeleteChild";



   /*******************************************************************************
    ** No physical cascade or trigger can conceal a missing framework child delete.
    *******************************************************************************/
   private void seed(QFieldType keyType) throws Exception
   {
      primeTestDatabase();
      String nativeType = keyType == QFieldType.BLOB ? "VARBINARY(16)" : "DECIMAL(20,4)";
      String target = keyType == QFieldType.BLOB ? "X'010203'" : "1.0000";
      String unrelated = keyType == QFieldType.BLOB ? "X'040506'" : "2.0000";
      executeSql("CREATE TABLE typed_delete_parent(id " + nativeType + " PRIMARY KEY, payload VARCHAR(30))");
      executeSql("CREATE TABLE typed_delete_child(id INTEGER PRIMARY KEY, parent_id " + nativeType + ", payload VARCHAR(30))");
      executeSql("INSERT INTO typed_delete_parent VALUES(" + target + ",'target'),(" + unrelated + ",'unrelated')");
      executeSql("INSERT INTO typed_delete_child VALUES(10," + target + ",'target child'),(20," + unrelated + ",'unrelated child')");
      QTableMetaData parent = new QTableMetaData().withName(PARENT).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("typed_delete_parent"))
         .withField(new QFieldMetaData("id", keyType))
         .withField(new QFieldMetaData("payload", QFieldType.STRING))
         .withAssociation(new Association().withName("owned children").withAssociatedTableName(CHILD).withJoinName("typedDeleteParentChild"));
      QTableMetaData child = new QTableMetaData().withName(CHILD).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("typed_delete_child"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentId", keyType).withBackendName("parent_id"))
         .withField(new QFieldMetaData("payload", QFieldType.STRING));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("typedDeleteParentChild").withLeftTable(PARENT).withRightTable(CHILD)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "parentId")));
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Remove only the fixture owned by this class.
    *******************************************************************************/
   @AfterEach
   void removeFixture() throws Exception
   {
      executeSql("DROP TABLE IF EXISTS typed_delete_child");
      executeSql("DROP TABLE IF EXISTS typed_delete_parent");
   }



   /*******************************************************************************
    ** JDBC returns a different byte[] instance from the caller's equivalent key.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testBinaryKeyDeletesOnlySelectedBranch(boolean filterSelection) throws Exception
   {
      assertSuccessfulDelete(QFieldType.BLOB, filterSelection);
   }



   /*******************************************************************************
    ** DECIMAL(20,4) returns scale four while the explicit caller key has scale one.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testDecimalKeyDeletesOnlySelectedBranch(boolean filterSelection) throws Exception
   {
      assertSuccessfulDelete(QFieldType.DECIMAL, filterSelection);
   }



   /*******************************************************************************
    ** A copied validation key must reject the actual parent before any child DML.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testCopiedBinaryValidationKeyPreservesEntireBranch(boolean filterSelection) throws Exception
   {
      assertRejectedDelete(QFieldType.BLOB, filterSelection);
   }



   /*******************************************************************************
    ** Numerically equal validation keys with a different scale identify one row.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testDifferentScaleValidationKeyPreservesEntireBranch(boolean filterSelection) throws Exception
   {
      assertRejectedDelete(QFieldType.DECIMAL, filterSelection);
   }



   /*******************************************************************************
    ** Storage, rather than action count alone, proves both intended deletions.
    *******************************************************************************/
   private void assertSuccessfulDelete(QFieldType keyType, boolean filterSelection) throws Exception
   {
      seed(keyType);
      List<List<String>> unrelatedParent = rows("SELECT * FROM typed_delete_parent WHERE payload='unrelated'");
      List<List<String>> unrelatedChild = rows("SELECT * FROM typed_delete_child WHERE id=20");
      DeleteInput input = input(keyType, filterSelection);
      QQueryFilter filter = input.getQueryFilter();
      Serializable key = filterSelection ? null : input.getPrimaryKeys().get(0);
      DeleteOutput output = new DeleteAction().execute(input);
      assertEquals(1, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).isNullOrEmpty();
      assertEquals(unrelatedParent, rows("SELECT * FROM typed_delete_parent ORDER BY payload"));
      assertEquals(unrelatedChild, rows("SELECT * FROM typed_delete_child ORDER BY id"));
      assertInputRestored(input, filter, key);
   }



   /*******************************************************************************
    ** The customizer returns a separate record with an equivalent key; an actual
    ** BadInput result distinguishes this denial from a false not-found result.
    *******************************************************************************/
   private void assertRejectedDelete(QFieldType keyType, boolean filterSelection) throws Exception
   {
      seed(keyType);
      QContext.getQInstance().getTable(PARENT).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(RejectEquivalentKey.class));
      Map<String, Object> before = snapshot();
      DeleteInput input = input(keyType, filterSelection);
      QQueryFilter filter = input.getQueryFilter();
      Serializable key = filterSelection ? null : input.getPrimaryKeys().get(0);
      DeleteOutput output = new DeleteAction().execute(input);
      assertEquals(0, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).singleElement().satisfies(record ->
         assertThat(record.getErrors()).singleElement().isInstanceOf(BadInputStatusMessage.class));
      assertEquals(before, snapshot());
      assertInputRestored(input, filter, key);
   }



   /*******************************************************************************
    ** Filtering uses an ordinary string field so native filter coercion cannot
    ** hide differences between independently prefetched primary-key instances.
    *******************************************************************************/
   private DeleteInput input(QFieldType keyType, boolean filterSelection)
   {
      DeleteInput input = new DeleteInput(PARENT).withInputSource(QInputSource.USER);
      if(filterSelection)
      {
         return input.withQueryFilter(new QQueryFilter(new QFilterCriteria("payload", QCriteriaOperator.EQUALS, "target")));
      }
      Serializable key = keyType == QFieldType.BLOB ? new byte[] { 1, 2, 3 } : new BigDecimal("1.0");
      return input.withPrimaryKeys(List.of(key));
   }



   /*******************************************************************************
    ** Selection remains owned by the caller after both success and rejection.
    *******************************************************************************/
   private void assertInputRestored(DeleteInput input, QQueryFilter filter, Serializable key)
   {
      assertSame(filter, input.getQueryFilter());
      if(filter != null)
      {
         assertNull(input.getPrimaryKeys());
      }
      else
      {
         assertEquals(1, input.getPrimaryKeys().size());
         assertSame(key, input.getPrimaryKeys().get(0));
      }
   }



   /*******************************************************************************
    ** Binary values are compared by bytes, not JDBC object identity.
    *******************************************************************************/
   private Map<String, Object> snapshot() throws Exception
   {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("parents", rows("SELECT * FROM typed_delete_parent ORDER BY payload"));
      result.put("children", rows("SELECT * FROM typed_delete_child ORDER BY id"));
      return result;
   }



   /*******************************************************************************
    ** Native representations preserve decimal scale and all binary bytes.
    *******************************************************************************/
   private List<List<String>> rows(String query) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int i = 1; i <= result.getMetaData().getColumnCount(); i++)
            {
               Object value = result.getObject(i);
               row.add(value instanceof byte[] bytes ? HexFormat.of().formatHex(bytes) : result.getString(i));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void executeSql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    ** Equivalent customizer keys must match the originally requested target.
    *******************************************************************************/
   public static class RejectEquivalentKey implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Return copied records rather than mutating the prefetch list in place.
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         List<QRecord> rejected = new ArrayList<>();
         for(QRecord record : records)
         {
            Serializable stored = record.getValue("id");
            Serializable equivalent = stored instanceof byte[] bytes ? bytes.clone() : ((BigDecimal) stored).stripTrailingZeros();
            QRecord result = new QRecord().withValue("id", equivalent);
            result.addError(new BadInputStatusMessage("This parent must remain"));
            rejected.add(result);
         }
         return rejected;
      }
   }
}
