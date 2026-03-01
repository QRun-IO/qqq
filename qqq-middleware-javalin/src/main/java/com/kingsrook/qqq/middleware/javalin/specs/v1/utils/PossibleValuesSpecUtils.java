/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;


/*******************************************************************************
 ** Shared utilities for possible values spec definitions.
 *******************************************************************************/
public class PossibleValuesSpecUtils
{

   /***************************************************************************
    ** Build the shared request body schema used by all possible values endpoints.
    ***************************************************************************/
   public static RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();

      properties.put("searchTerm", new Schema()
         .withDescription("Text to search for within possible value labels")
         .withType(Type.STRING));

      properties.put("ids", new Schema()
         .withDescription("List of specific ids to look up")
         .withType(Type.ARRAY)
         .withItems(new Schema().withType(Type.STRING)));

      properties.put("values", new Schema()
         .withDescription("Map of other field values, used for filter interpolation on possible value source filters")
         .withType(Type.OBJECT));

      properties.put("useCase", new Schema()
         .withDescription("Use case for possible value search filtering. Controls behavior when filter values are missing.")
         .withType(Type.STRING)
         .withEnumValues(List.of("FORM", "FILTER")));

      properties.put("filter", new Schema()
         .withDescription("Optional default filter to apply to the possible value search")
         .withRef("#/components/schemas/QueryFilter"));

      return new RequestBody()
         .withContent(Map.of(
            ContentType.APPLICATION_JSON.getMimeType(), new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withProperties(properties))
         ));
   }

}
