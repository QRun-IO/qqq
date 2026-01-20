/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.menus.items;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/*******************************************************************************
 ** Unit test for QMenuItemDownloadFile 
 *******************************************************************************/
class QMenuItemDownloadFileTest extends BaseTest
{

   /*******************************************************************************
    * basic test of cloning, because, cloning...
    *******************************************************************************/
   @Test
   void testClone()
   {
      QMenuItemDownloadFile menuItemDownloadFile = (QMenuItemDownloadFile) new QMenuItemDownloadFile()
         .withFieldName("myFile")
         .withLabel("My File")
         .withIcon(new QIcon().withName("download"));

      QMenuItemDownloadFile clone = menuItemDownloadFile.clone();

      assertEquals(menuItemDownloadFile.getFieldName(), clone.getFieldName());

      clone.setFieldName("yourField");
      assertNotEquals(menuItemDownloadFile.getFieldName(), clone.getFieldName());

      clone.getIcon().setName("upload");
      assertNotEquals(menuItemDownloadFile.getIcon().getName(), clone.getIcon().getName());
   }

}