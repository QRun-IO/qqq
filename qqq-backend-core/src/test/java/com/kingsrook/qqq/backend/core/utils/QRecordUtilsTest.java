/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for QRecordUtils 
 *******************************************************************************/
class QRecordUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetChangedFields()
   {
      QFieldMetaData id   = new QFieldMetaData("id", QFieldType.INTEGER);
      QFieldMetaData name = new QFieldMetaData("name", QFieldType.STRING);

      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(null, null, null));
      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(new QRecord(), null, null));
      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(null, new QRecord(), null));
      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(null, null, List.of(id)));
      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(new QRecord(), new QRecord(), List.of(id)));
      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(new QRecord().withValue("id", 1), new QRecord().withValue("id", 1), List.of(id)));

      //////////////////////////////////////////////////////////////////
      // show that we ignore fields that aren't in the list of fields //
      //////////////////////////////////////////////////////////////////
      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(new QRecord().withValue("id", 1), new QRecord().withValue("id", 2), List.of(name)));

      ////////////////////////////////////////////////////////////
      // show that we'll "type-convert" the values, so 1 == "1" //
      ////////////////////////////////////////////////////////////
      assertEquals(Collections.emptyList(), QRecordUtils.getChangedFields(new QRecord().withValue("id", 1), new QRecord().withValue("id", "1"), List.of(id)));

      assertEquals(List.of(id), QRecordUtils.getChangedFields(new QRecord().withValue("id", 1), new QRecord().withValue("id", 2), List.of(id)));
      assertEquals(List.of(id), QRecordUtils.getChangedFields(new QRecord(), new QRecord().withValue("id", 2), List.of(id)));
      assertEquals(List.of(id), QRecordUtils.getChangedFields(null, new QRecord().withValue("id", 2), List.of(id)));
      assertEquals(List.of(id), QRecordUtils.getChangedFields(new QRecord().withValue("id", 1), new QRecord(), List.of(id)));
      assertEquals(List.of(id), QRecordUtils.getChangedFields(new QRecord().withValue("id", 1), null, List.of(id)));
      assertEquals(List.of(id, name), QRecordUtils.getChangedFields(new QRecord().withValue("id", 1).withValue("name", "Bob"), new QRecord().withValue("id", 2).withValue("name", "N."), List.of(id, name)));
   }

}