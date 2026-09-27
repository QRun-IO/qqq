/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableVariantsOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;


/*******************************************************************************
 ** Response for GET /table/{tableName}/variants.
 *******************************************************************************/
public class TableVariantsResponseV1 implements TableVariantsOutputInterface, ToSchema
{
   @OpenAPIDescription("The variants the table's backend can be scoped to - send one back as tableVariant in query, count, get and export requests.  Empty (always present) when the backend does not use variants.")
   @OpenAPIListItems(value = TableVariant.class, useRef = true)
   private List<TableVariant> variants;



   /*******************************************************************************
    ** Getter for variants
    *******************************************************************************/
   public List<TableVariant> getVariants()
   {
      return (this.variants);
   }



   /*******************************************************************************
    ** Setter for variants
    *******************************************************************************/
   @Override
   public void setVariants(List<TableVariant> variants)
   {
      this.variants = variants;
   }

}
