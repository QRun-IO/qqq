/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QKeyBasedFieldMapping;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for BasicETLProcess
 *******************************************************************************/
class StreamedETLProcessTest extends BaseTest
{

   /*******************************************************************************
    ** Simplest happy path
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      RunProcessInput request = new RunProcessInput();
      request.setProcessName(StreamedETLProcess.PROCESS_NAME);
      request.addValue(StreamedETLProcess.FIELD_SOURCE_TABLE, TestUtils.defineTablePerson().getName());
      request.addValue(StreamedETLProcess.FIELD_DESTINATION_TABLE, TestUtils.definePersonFileTable().getName());
      request.addValue(StreamedETLProcess.FIELD_MAPPING_JSON, "");

      RunProcessOutput result = new RunProcessAction().execute(request);
      assertNotNull(result);
      ///////////////////////////////////////////////////////////////////////
      // since this is streamed, assert there are no records in the output //
      ///////////////////////////////////////////////////////////////////////
      assertTrue(result.getRecords().isEmpty());
      assertTrue(result.getException().isEmpty());
   }



   /*******************************************************************************
    ** Basic example of doing a mapping transformation
    *******************************************************************************/
   @Test
   public void testMappingTransformation() throws QException
   {
      RunProcessInput request = new RunProcessInput();
      request.setProcessName(StreamedETLProcess.PROCESS_NAME);
      request.addValue(StreamedETLProcess.FIELD_SOURCE_TABLE, TestUtils.definePersonFileTable().getName());
      request.addValue(StreamedETLProcess.FIELD_DESTINATION_TABLE, TestUtils.defineTableIdAndNameOnly().getName());

      ///////////////////////////////////////////////////////////////////////////////////////
      // define our mapping from destination-table field names to source-table field names //
      ///////////////////////////////////////////////////////////////////////////////////////
      QKeyBasedFieldMapping mapping = new QKeyBasedFieldMapping().withMapping("name", "firstName");
      request.addValue(StreamedETLProcess.FIELD_MAPPING_JSON, JsonUtils.toJson(mapping));

      RunProcessOutput result = new RunProcessAction().execute(request);
      assertNotNull(result);
      ///////////////////////////////////////////////////////////////////////
      // since this is streamed, assert there are no records in the output //
      ///////////////////////////////////////////////////////////////////////
      assertTrue(result.getRecords().isEmpty());
      assertTrue(result.getException().isEmpty());
   }

}