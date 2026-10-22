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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;


/*******************************************************************************
 **
 *******************************************************************************/
public class IfInputVariableExistsFilter extends AbstractConditionalFilter
{
   private String       inputVariableName;
   private QQueryFilter filter;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public IfInputVariableExistsFilter()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public IfInputVariableExistsFilter(String inputVariableName, QQueryFilter filter)
   {
      this.inputVariableName = inputVariableName;
      this.filter = filter;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean testCondition(RenderWidgetInput renderWidgetInput)
   {
      return (StringUtils.hasContent(ValueUtils.getValueAsString(renderWidgetInput.getQueryParams().get(inputVariableName))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QQueryFilter getFilter(RenderWidgetInput renderWidgetInput)
   {
      QQueryFilter returnFilter = filter.clone();
      for(QFilterCriteria criterion : CollectionUtils.nonNullList(returnFilter.getCriteria()))
      {
         if(criterion.getValues() != null)
         {
            criterion.setValues(new MutableList<>(criterion.getValues()));
            for(int i = 0; i < criterion.getValues().size(); i++)
            {
               Serializable value = criterion.getValues().get(i);
               if(value instanceof String valueString && valueString.equals("${input." + inputVariableName + "}"))
               {
                  criterion.getValues().set(i, renderWidgetInput.getQueryParams().get(inputVariableName));
               }
            }
         }
      }

      return (returnFilter);
   }
}
