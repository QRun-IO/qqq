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

package com.kingsrook.qqq.middleware.javalin;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Boundary checks use declared recursive metadata without executing a write.
 *******************************************************************************/
class AssociatedRecordRequestTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      QInstance instance = TestUtils.defineInstance();
      instance.addTable(new QTableMetaData().withName("node").withBackendName(TestUtils.defineMemoryBackend().getName()).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("values", QFieldType.STRING))
         .withField(new QFieldMetaData("binary", QFieldType.BLOB))
         .withAssociation(new Association().withName("children").withAssociatedTableName("node").withJoinName("nodeJoinNode")));
      instance.addJoin(new QJoinMetaData().withName("nodeJoinNode").withLeftTable("node").withRightTable("node")
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "parentId")));
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** A finite tree reaches the documented boundary; deeper JSON fails safely.
    *******************************************************************************/
   @Test
   void testDepthBoundaryAndExcessiveNesting() throws Exception
   {
      Map<String, List<QRecord>> groups = AssociatedRecordRequest.read(nested(64), new InsertInput("node"));
      for(int depth = 0; depth < 64; depth++)
      {
         assertEquals(1, groups.get("children").size());
         groups = groups.get("children").get(0).getAssociatedRecords();
      }
      assertTrue(groups.isEmpty());
      String prefix = "{\"children\":[{\"values\":{},\"associatedRecords\":".repeat(64);
      String suffix = "}]}".repeat(64);
      groups = AssociatedRecordRequest.read(prefix + "{\"children\":[]}" + suffix, new InsertInput("node"));
      for(int depth = 0; depth < 64; depth++)
      {
         groups = groups.get("children").get(0).getAssociatedRecords();
      }
      assertEquals(Map.of("children", List.of()), groups);
      for(String invalid : List.of("{\"unknown\":[]}", "{\"children\":{}}", "{\"children\":[{\"values\":{}}]}"))
      {
         assertThrows(QBadRequestException.class, () -> AssociatedRecordRequest.read(prefix + invalid + suffix, new InsertInput("node")));
      }
      assertThrows(QBadRequestException.class, () -> AssociatedRecordRequest.read(nested(65), new InsertInput("node")));
      assertThrows(QBadRequestException.class, () -> AssociatedRecordRequest.read(nested(1000), new InsertInput("node")));
   }



   /*******************************************************************************
    ** A field named values and legacy scalar arrays cannot collide with envelopes.
    *******************************************************************************/
   @Test
   void testScalarArraysAndEmptyBinaryPreserveValues() throws Exception
   {
      JSONArray array = new JSONArray().put(false).put(0).put(JSONObject.NULL).put("null");
      JSONObject values = new JSONObject().put("values", array).put("binary", new JSONObject().put("base64", ""));
      QRecord record = readValues(values);
      assertEquals(Arrays.asList(false, 0, null, "null"), record.getValue("values"));
      assertArrayEquals(new byte[0], (byte[]) record.getValue("binary"));
   }



   /*******************************************************************************
    ** Nested arrays/objects and malformed binary wrappers have no coercion fallback.
    *******************************************************************************/
   @Test
   void testInvalidValueShapes()
   {
      for(Object value : List.of(new JSONObject(), new JSONObject().put("base64", "AA=="),
         new JSONArray().put(new JSONObject()), new JSONArray().put(new JSONArray())))
      {
         assertThrows(QBadRequestException.class, () -> readValues(new JSONObject().put("values", value)));
      }
      for(Object value : List.of(new JSONObject(), new JSONObject().put("base64", 0),
         new JSONObject().put("base64", "AA==").put("name", "file")))
      {
         assertThrows(QBadRequestException.class, () -> readValues(new JSONObject().put("binary", value)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord readValues(JSONObject values) throws Exception
   {
      String json = new JSONObject().put("children", new JSONArray().put(new JSONObject().put("values", values))).toString();
      return AssociatedRecordRequest.read(json, new InsertInput("node")).get("children").get(0);
   }



   /*******************************************************************************
    ** Construct excessive input without recursively serializing it in the test.
    *******************************************************************************/
   private String nested(int depth)
   {
      return "{\"children\":[{\"values\":{},\"associatedRecords\":".repeat(depth) + "{}" + "}]}".repeat(depth);
   }
}
