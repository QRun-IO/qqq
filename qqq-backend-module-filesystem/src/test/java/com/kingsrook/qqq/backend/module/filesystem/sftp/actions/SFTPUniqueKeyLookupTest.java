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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Native declared-key lookup uses the owned SFTP server and canonical file mapping.
 *******************************************************************************/
class SFTPUniqueKeyLookupTest extends BaseSFTPTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilenameConflictAndStoredKey() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_SFTP_FILE);
      UniqueKey key = new UniqueKey("fileName");
      table.withUniqueKey(key);
      assertThat(UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("fileName", "testfile-1.txt"), null))
         .extracting(record -> record.getValueString("fileName")).containsExactly("testfile-1.txt");
      assertThat(UniqueKeyLookup.readStoredComponents(table, List.of("testfile-1.txt"), null))
         .extracting(record -> record.getValueString("fileName")).containsExactly("testfile-1.txt");
   }
}
