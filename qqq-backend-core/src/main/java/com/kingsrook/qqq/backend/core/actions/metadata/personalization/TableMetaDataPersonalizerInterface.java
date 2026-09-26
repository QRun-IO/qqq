/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.metadata.personalization;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.metadata.SupplementalCustomizerType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 * interface for objects that personalize TableMetaData for (user) actions.
 *
 * e.g., to hide fields from a subset of users - as the query action passes the
 * table being queried through this class, so removing a field from the table
 * should effectively hide such a field's data.
 *
 * An application's implementation class needs to be registered in a QInstance
 * via:
 * <code>
 * qInstance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE,
 *    new QCodeReference(SomeImplementation.class));
 * </code>
 *******************************************************************************/
public interface TableMetaDataPersonalizerInterface
{
   /***************************************************************************
    * SupplementalCustomizerType reference used to refer to instances of this interface.
    ***************************************************************************/
   SupplementalCustomizerType CUSTOMIZER_TYPE = () -> TableMetaDataPersonalizerInterface.class;

   /***************************************************************************
    * It is vitally important that the {@link QTableMetaData} returned is a clone
    * if it has any changes, to avoid changing the meta data for the whole application!
    ***************************************************************************/
   QTableMetaData execute(TableMetaDataPersonalizerInput tableMetaDataPersonalizerInput) throws QException;

}
