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
