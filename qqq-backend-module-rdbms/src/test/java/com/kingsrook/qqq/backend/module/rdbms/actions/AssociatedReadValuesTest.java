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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Expanded source reads retain requested values and fail instead of truncating.
 *******************************************************************************/
class AssociatedReadValuesTest extends RDBMSActionTest
{
   private static final String TABLE = "associatedReadNode";



   /*******************************************************************************
    ** Self-referencing metadata with finite data is a valid three-level tree.
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      primeTestDatabase();
      sql("CREATE TABLE associated_read_node(id INT PRIMARY KEY, parent_id INT, payload VARBINARY(20), heavy_text VARCHAR(20), hidden_value VARCHAR(20))");
      sql("INSERT INTO associated_read_node VALUES(1,NULL,X'0001FF804100','root','hidden root'),(2,1,X'0001FF804100','child','hidden child'),(3,2,X'0001FF804100','leaf','hidden leaf'),(4,NULL,X'04','outside','hidden outside')");
      QContext.getQInstance().addTable(new QTableMetaData().withName(TABLE).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("associated_read_node"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentId", QFieldType.INTEGER).withBackendName("parent_id"))
         .withField(new QFieldMetaData("payload", QFieldType.BLOB).withIsHeavy(true))
         .withField(new QFieldMetaData("heavyText", QFieldType.STRING).withBackendName("heavy_text").withIsHeavy(true))
         .withField(new QFieldMetaData("hiddenValue", QFieldType.STRING).withBackendName("hidden_value").withIsHidden(true))
         .withAssociation(new Association().withName("children").withAssociatedTableName(TABLE).withJoinName("readNodeChildren")));
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("readNodeChildren").withLeftTable(TABLE).withRightTable(TABLE)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "parentId")));
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp() throws Exception
   {
      sql("DROP TABLE IF EXISTS associated_read_node");
   }



   /*******************************************************************************
    ** Get's heavy default and explicit trusted read flags apply to descendants.
    *******************************************************************************/
   @Test
   void testHeavyValuesAndReadFlagsAcrossNamedLevels() throws Exception
   {
      GetInput input = new GetInput(TABLE).withPrimaryKey(1).withIncludeAssociations(true)
         .withAssociationNamesToInclude(List.of("children", "children.children"));
      QRecord record = new GetAction().execute(input).getRecord();
      for(String text : List.of("root", "child", "leaf"))
      {
         assertArrayEquals(new byte[] { 0, 1, -1, -128, 65, 0 }, (byte[]) record.getValue("payload"));
         assertEquals(text, record.getValueString("heavyText"));
         assertFalse(record.getValues().containsKey("hiddenValue"));
         if(!text.equals("leaf"))
         {
            record = record.getAssociatedRecords().get("children").get(0);
         }
      }
      input.withShouldOmitHiddenFields(false).withShouldGenerateDisplayValues(false).withShouldTranslatePossibleValues(false);
      record = new GetAction().execute(input).getRecord().getAssociatedRecords().get("children").get(0);
      assertEquals("hidden child", record.getValueString("hiddenValue"));
      assertTrue(record.getDisplayValues() == null || record.getDisplayValues().isEmpty());
      QueryInput light = new QueryInput(TABLE).withIncludeAssociations(true).withAssociationNamesToInclude(List.of("children", "children.children"))
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)));
      record = new QueryAction().execute(light).getRecords().get(0);
      for(int depth = 0; depth < 3; depth++)
      {
         assertFalse(record.getValues().containsKey("payload"));
         assertFalse(record.getValues().containsKey("heavyText"));
         if(depth < 2)
         {
            record = record.getAssociatedRecords().get("children").get(0);
         }
      }
   }



   /*******************************************************************************
    ** A finite self-tree and an empty root terminate despite cyclic metadata.
    *******************************************************************************/
   @Test
   void testFiniteSelfTreeAndEmptyRootTerminate() throws Exception
   {
      QRecord record = new GetAction().execute(new GetInput(TABLE).withPrimaryKey(1).withIncludeAssociations(true)).getRecord();
      assertEquals(2, record.getAssociatedRecords().get("children").get(0).getValueInteger("id"));
      QRecord leaf = record.getAssociatedRecords().get("children").get(0).getAssociatedRecords().get("children").get(0);
      assertEquals(3, leaf.getValueInteger("id"));
      assertTrue(leaf.getAssociatedRecords().get("children").isEmpty());
      QueryInput empty = new QueryInput(TABLE).withIncludeAssociations(true)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 999)));
      assertTrue(new QueryAction().execute(empty).getRecords().isEmpty());
      assertEquals(4, count());
   }



   /*******************************************************************************
    ** A real data cycle fails the expansion, leaving native rows and later reads intact.
    *******************************************************************************/
   @Test
   void testCyclicDataHasBoundedFailure() throws Exception
   {
      sql("UPDATE associated_read_node SET parent_id=3 WHERE id=1");
      QException failure = assertThrows(QException.class, () -> new GetAction().execute(new GetInput(TABLE).withPrimaryKey(1).withIncludeAssociations(true)));
      assertTrue(failure.getMessage().contains("maximum depth"), failure.getMessage());
      assertEquals(4, count());
      assertEquals(3, new GetAction().execute(new GetInput(TABLE).withPrimaryKey(1)).getRecord().getValueInteger("parentId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int count() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()); Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM associated_read_node"))
      {
         assertTrue(result.next());
         return result.getInt(1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void sql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }
}
