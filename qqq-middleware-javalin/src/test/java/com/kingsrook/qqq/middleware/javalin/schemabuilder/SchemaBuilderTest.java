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

package com.kingsrook.qqq.middleware.javalin.schemabuilder;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.AuthenticationMetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.MetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessInitOrStepOrStatusResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.AppTreeNode;
import com.kingsrook.qqq.openapi.model.Schema;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for SchemaBuilder 
 *******************************************************************************/
class SchemaBuilderTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIncludesAOneOf()
   {
      Schema schema = new SchemaBuilder().classToSchema(AuthenticationMetaDataResponseV1.class);
      System.out.println(schema);

      Schema valuesSchema = schema.getProperties().get("values");
      List<Schema> oneOf  = valuesSchema.getOneOf();
      assertEquals(3, oneOf.size());
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUsesIncludeProperties()
   {
      Schema schema = new SchemaBuilder().classToSchema(ProcessInitOrStepOrStatusResponseV1.TypedResponse.class);
      for(Schema oneOf : schema.getOneOf())
      {
         /////////////////////////////////////////////////////////////////////////////////////////
         // all of the wrapped one-of schemas should contain these fields from the parent class //
         /////////////////////////////////////////////////////////////////////////////////////////
         assertTrue(oneOf.getProperties().containsKey("type"));
         assertTrue(oneOf.getProperties().containsKey("processUUID"));
      }
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDescriptionOnGetters()
   {
      Schema schema = new SchemaBuilder().classToSchema(MetaDataResponseV1.class);
      assertTrue(schema.getProperties().containsKey("apps"));
      assertNotNull(schema.getProperties().get("apps").getDescription());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecursive()
   {
      Schema schema = new SchemaBuilder().classToSchema(AppTreeNode.class);
      Schema childrenSchema = schema.getProperties().get("children");
      assertNotNull(childrenSchema.getItems());
      System.out.println(schema);
   }

}