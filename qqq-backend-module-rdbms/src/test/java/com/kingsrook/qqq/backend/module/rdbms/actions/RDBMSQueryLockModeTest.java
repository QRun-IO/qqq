/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.backend.module.rdbms.actions;


import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryLockMode;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.RDBMSBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** QueryLockMode on a dialect without FOR UPDATE SKIP LOCKED (the H2 test
 ** database, base strategy): the module says it is unsupported, and a query
 ** that asks for it fails clearly instead of running without the locks.
 ** (PostgreSQL support is tested in the postgres module.)
 *******************************************************************************/
class RDBMSQueryLockModeTest extends RDBMSActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void unsupportedDialectRefusesSkipLocked()
   {
      QBackendMetaData backend = QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      assertThat(new RDBMSBackendModule().supportsQueryLockMode(backend, QueryLockMode.FOR_UPDATE_SKIP_LOCKED)).isFalse();
      assertThat(new RDBMSBackendModule().supportsQueryLockMode(backend, null)).isFalse();

      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON)
         .withLockMode(QueryLockMode.FOR_UPDATE_SKIP_LOCKED).withFilter(new QQueryFilter().withLimit(1))))
         .rootCause().hasMessageContaining("does not support FOR UPDATE SKIP LOCKED");
   }
}
