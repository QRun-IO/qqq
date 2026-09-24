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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.enumeration;


import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;


/*******************************************************************************
 **
 *******************************************************************************/
public class EnumerationCountAction implements CountInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public CountOutput execute(CountInput countInput) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(countInput.getTableName());
      queryInput.setFilter(countInput.getFilter() == null ? null : countInput.getFilter().clone().withSkip(null).withLimit(null));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      CountOutput countOutput = new CountOutput();
      countOutput.setCount(queryOutput.getRecords().size());
      return (countOutput);
   }

}
