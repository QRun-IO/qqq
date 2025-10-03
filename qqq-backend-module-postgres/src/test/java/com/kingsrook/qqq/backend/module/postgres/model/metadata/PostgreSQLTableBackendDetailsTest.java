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

package com.kingsrook.qqq.backend.module.postgres.model.metadata;


import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for PostgreSQLTableBackendDetails 
 *******************************************************************************/
class PostgreSQLTableBackendDetailsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      PostgreSQLTableBackendDetails tableBackendDetails = (PostgreSQLTableBackendDetails) new PostgreSQLTableBackendDetails()
         .withTableName("originalTableName")
         .withBackendType("originalBackendType");

      PostgreSQLTableBackendDetails clone = (PostgreSQLTableBackendDetails) tableBackendDetails.clone();

      assertEquals("originalTableName", tableBackendDetails.getTableName());
      assertEquals("originalTableName", clone.getTableName());
      assertEquals("originalBackendType", tableBackendDetails.getBackendType());
      assertEquals("originalBackendType", clone.getBackendType());

      clone.setTableName("newTableName");
      clone.setBackendType("newBackendType");

      assertEquals("originalTableName", tableBackendDetails.getTableName());
      assertEquals("newTableName", clone.getTableName());
      assertEquals("originalBackendType", tableBackendDetails.getBackendType());
      assertEquals("newBackendType", clone.getBackendType());
   }

}
