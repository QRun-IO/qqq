/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
