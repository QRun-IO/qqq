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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Configuration is optional, but a configured table must support storage.
 *******************************************************************************/
class ProcessOutputStorageMetaDataTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRegistrationAndValidation()
   {
      assertThat(ProcessOutputStorageMetaData.of(QContext.getQInstance())).isNull();
      ProcessOutputStorageMetaData metadata = new ProcessOutputStorageMetaData().withTableName(TestUtils.TABLE_NAME_MEMORY_STORAGE);
      metadata.addSelfToInstance(QContext.getQInstance());
      assertThat(ProcessOutputStorageMetaData.of(QContext.getQInstance())).isSameAs(metadata);
      assertThat(metadata.getTableName()).isEqualTo(TestUtils.TABLE_NAME_MEMORY_STORAGE);
      assertThat(validate(metadata).getErrors()).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidTables()
   {
      ProcessOutputStorageMetaData metadata = new ProcessOutputStorageMetaData();
      assertThat(validate(metadata).getErrors()).anyMatch(error -> error.contains("does not exist"));
      metadata.setTableName("missing");
      assertThat(validate(metadata).getErrors()).anyMatch(error -> error.contains("does not exist"));
      metadata.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertThat(validate(metadata).getErrors()).anyMatch(error -> error.contains("STRING primary key and a BLOB"));
      metadata.setTableName(TestUtils.TABLE_NAME_PERSON);
      assertThat(validate(metadata).getErrors()).anyMatch(error -> error.contains("must support storage"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QInstanceValidator validate(ProcessOutputStorageMetaData metadata)
   {
      QInstanceValidator validator = new QInstanceValidator();
      metadata.validate(QContext.getQInstance(), validator);
      return validator;
   }
}
