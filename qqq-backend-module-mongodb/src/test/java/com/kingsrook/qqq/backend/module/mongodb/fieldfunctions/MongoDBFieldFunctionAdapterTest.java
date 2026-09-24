/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.mongodb.fieldfunctions;


import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for MongoDB FieldFunction adapter implementations.
 ** These test BSON expression generation without requiring a real database.
 *******************************************************************************/
class MongoDBFieldFunctionAdapterTest
{
   private static final Function<String, String> IDENTITY_REF = name -> "$" + name;


   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      QInstance qInstance = new QInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      qInstance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      qInstance.addTable(new QTableMetaData().withName("test").withBackendName("memory")
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      QContext.init(qInstance, new QSession());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testStringLengthExpression()
   {
      MongoDBStringLengthFunction adapter = new MongoDBStringLengthFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withFieldName("firstName");

      Object expression = adapter.getExpression("$firstName", ff, IDENTITY_REF);
      assertEquals(new Document("$strLenCP", "$firstName"), expression);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringExpressionFromOnly()
   {
      MongoDBSubStringFunction adapter = new MongoDBSubStringFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("firstName")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2));

      Object expression = adapter.getExpression("$firstName", ff, IDENTITY_REF);
      // fromIndex 2 (1-based) => 1 (0-based), no length => Integer.MAX_VALUE
      assertEquals(new Document("$substrCP", List.of("$firstName", 1, Integer.MAX_VALUE)), expression);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringExpressionFromAndLength()
   {
      MongoDBSubStringFunction adapter = new MongoDBSubStringFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("firstName")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 3, SubStringFunction.LENGTH_PARAM, 5));

      Object expression = adapter.getExpression("$firstName", ff, IDENTITY_REF);
      // fromIndex 3 (1-based) => 2 (0-based)
      assertEquals(new Document("$substrCP", List.of("$firstName", 2, 5)), expression);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateExpression()
   {
      MongoDBWeekdayOfDateFunction adapter = new MongoDBWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate");

      Object expression = adapter.getExpression("$birthDate", ff, IDENTITY_REF);

      ///////////////////////////////////////////////////////////////////////////////
      // the expression should be: {$add: [{$mod: [{$add: [{$dayOfWeek: "$birthDate"}, 5]}, 7]}, 1]}
      // which converts MongoDB's Sun=1..Sat=7 to ISO-8601 Mon=1..Sun=7
      ///////////////////////////////////////////////////////////////////////////////
      Document doc = (Document) expression;
      assertEquals("$add", doc.keySet().iterator().next());

      @SuppressWarnings("unchecked")
      List<Object> addArgs = (List<Object>) doc.get("$add");
      assertEquals(2, addArgs.size());
      assertEquals(1, addArgs.get(1));

      Document modDoc = (Document) addArgs.get(0);
      assertEquals("$mod", modDoc.keySet().iterator().next());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateOrderBySundayFirst()
   {
      MongoDBWeekdayOfDateFunction adapter = new MongoDBWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, true));

      Object orderByExpr = adapter.getExpressionForOrderBy("$birthDate", ff, IDENTITY_REF);
      Object selectExpr  = adapter.getExpression("$birthDate", ff, IDENTITY_REF);

      // order-by with sundayFirst should differ from the select expression
      assertNotEquals(selectExpr, orderByExpr);

      // order-by should be: {$mod: [isoExpr, 7]}
      Document doc = (Document) orderByExpr;
      assertEquals("$mod", doc.keySet().iterator().next());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateOrderByMondayFirst()
   {
      MongoDBWeekdayOfDateFunction adapter = new MongoDBWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, false));

      Object orderByExpr = adapter.getExpressionForOrderBy("$birthDate", ff, IDENTITY_REF);
      Object selectExpr  = adapter.getExpression("$birthDate", ff, IDENTITY_REF);

      // without sundayFirst, order-by should be the same as select
      assertEquals(selectExpr, orderByExpr);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeExpression()
   {
      MongoDBWeekdayOfDateTimeFunction adapter = new MongoDBWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "America/Chicago"));

      Object expression = adapter.getExpression("$createDate", ff, IDENTITY_REF);
      Document doc = (Document) expression;

      // should be: {$add: [{$mod: [{$add: [{$dayOfWeek: {date: "$createDate", timezone: "America/Chicago"}}, 5]}, 7]}, 1]}
      assertEquals("$add", doc.keySet().iterator().next());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeOrderBySundayFirst()
   {
      MongoDBWeekdayOfDateTimeFunction adapter = new MongoDBWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "America/Chicago",
            WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, true));

      Object orderByExpr = adapter.getExpressionForOrderBy("$createDate", ff, IDENTITY_REF);
      Object selectExpr  = adapter.getExpression("$createDate", ff, IDENTITY_REF);

      assertNotEquals(selectExpr, orderByExpr);

      Document doc = (Document) orderByExpr;
      assertEquals("$mod", doc.keySet().iterator().next());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeOrderByMondayFirst()
   {
      MongoDBWeekdayOfDateTimeFunction adapter = new MongoDBWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "America/Chicago",
            WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, false));

      Object orderByExpr = adapter.getExpressionForOrderBy("$createDate", ff, IDENTITY_REF);
      Object selectExpr  = adapter.getExpression("$createDate", ff, IDENTITY_REF);

      assertEquals(selectExpr, orderByExpr);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeWithExplicitTimezone()
   {
      MongoDBWeekdayOfDateTimeFunction adapter = new MongoDBWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "Europe/London"));

      Object expression = adapter.getExpression("$createDate", ff, IDENTITY_REF);

      // verify that the timezone is embedded in the expression
      String exprString = expression.toString();
      assertTrue(exprString.contains("Europe/London"), "Expression should contain the explicit timezone");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeDefaultsToSessionTimezone()
   {
      QContext.getQInstance().setDefaultTimeZoneId("America/New_York");

      MongoDBWeekdayOfDateTimeFunction adapter = new MongoDBWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false));

      Object expression = adapter.getExpression("$createDate", ff, IDENTITY_REF);
      String exprString = expression.toString();
      assertTrue(exprString.contains("America/New_York"), "Expression should contain the instance default timezone");
   }



   /***************************************************************************
    ** Test that the default getExpressionForOrderBy on the interface delegates
    ** to getExpression (exercised via StringLength, which doesn't override it).
    ***************************************************************************/
   @Test
   void testDefaultGetExpressionForOrderByDelegatesToGetExpression()
   {
      MongoDBFieldFunctionAdapterInterface adapter = new MongoDBStringLengthFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withFieldName("firstName");

      Object selectExpr  = adapter.getExpression("$firstName", ff, IDENTITY_REF);
      Object orderByExpr = adapter.getExpressionForOrderBy("$firstName", ff, IDENTITY_REF);
      assertEquals(selectExpr, orderByExpr);
   }



   /***************************************************************************
    ** Test that when zoneIdFromFieldName is set, the generated expression uses
    ** $ifNull to try the per-document field value first, falling back to the
    ** static timezone.
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeWithZoneIdFromFieldName()
   {
      MongoDBWeekdayOfDateTimeFunction adapter = new MongoDBWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME, "homeTown",
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"));

      //////////////////////////////////////////////////////////////////////////////////
      // use a mapper that simulates a backend field name differing from the QQQ name //
      //////////////////////////////////////////////////////////////////////////////////
      Function<String, String> fieldNameToRef = name -> "$" + ("homeTown".equals(name) ? "home_town" : name);

      Object expression = adapter.getExpression("$createDate", ff, fieldNameToRef);
      String exprString = expression.toString();

      ///////////////////////////////////////////////////////////////////////////////
      // the timezone portion of the $dayOfWeek should use $ifNull to try the     //
      // per-document field reference first, falling back to the static timezone.  //
      ///////////////////////////////////////////////////////////////////////////////
      assertTrue(exprString.contains("$ifNull"), "Expression should contain $ifNull for field-based timezone");
      assertTrue(exprString.contains("$home_town"), "Expression should reference the timezone field's backend name");
      assertTrue(exprString.contains("UTC"), "Expression should contain the fallback static timezone");
   }



   /***************************************************************************
    ** Test that when zoneIdFromFieldName is set, the ORDER BY expression also
    ** uses $ifNull for the timezone and applies sundayFirst modulo.
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeOrderByWithZoneIdFromFieldNameSundayFirst()
   {
      MongoDBWeekdayOfDateTimeFunction adapter = new MongoDBWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME, "homeTown",
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC",
            WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, true));

      Object orderByExpr = adapter.getExpressionForOrderBy("$createDate", ff, IDENTITY_REF);
      String exprString  = orderByExpr.toString();

      assertTrue(exprString.contains("$ifNull"), "Order-by expression should contain $ifNull for field-based timezone");
      assertTrue(exprString.contains("$homeTown"), "Order-by expression should reference the timezone field");

      // should be wrapped in $mod for sundayFirst
      Document doc = (Document) orderByExpr;
      assertEquals("$mod", doc.keySet().iterator().next());
   }

}
