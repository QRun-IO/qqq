/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.producererrors.constructorthrows;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Test producer whose no-arg constructor always throws - for testing how
 ** MetaDataProducerHelper handles errors while finding (evaluating) producer
 ** classes.
 *******************************************************************************/
public class TestThrowsInConstructorMetaDataProducer extends MetaDataProducer<QTableMetaData>
{
   public static final String NAME    = "neverProduced";
   public static final String MESSAGE = "Test producer failed in constructor";



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TestThrowsInConstructorMetaDataProducer()
   {
      throw (new IllegalStateException(MESSAGE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QTableMetaData produce(QInstance qInstance) throws QException
   {
      return new QTableMetaData().withName(NAME);
   }
}
