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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Base class for rendering qqq dashboard widgets
 **
 *******************************************************************************/
public abstract class AbstractWidgetRenderer
{
   public static final QValueFormatter   valueFormatter = new QValueFormatter();
   public static final DateTimeFormatter dateFormatter  = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());



   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract RenderWidgetOutput render(RenderWidgetInput input) throws QException;



   /*******************************************************************************
    **
    *******************************************************************************/
   protected boolean setupDropdowns(RenderWidgetInput input, QWidgetMetaData metaData, QWidgetData widgetData) throws QException
   {
      List<List<Map<String, String>>> dataList                  = new ArrayList<>();
      List<String>                    labelList                 = new ArrayList<>();
      List<String>                    nameList                  = new ArrayList<>();
      List<String>                    missingRequiredSelections = new ArrayList<>();
      for(WidgetDropdownData dropdownData : CollectionUtils.nonNullList(metaData.getDropdowns()))
      {
         if(WidgetDropdownType.DATE_PICKER.equals(dropdownData.getType()))
         {
            String name = dropdownData.getName();
            nameList.add(name);
            labelList.add(dropdownData.getLabel());
            dataList.add(new ArrayList<>());

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // sure that something has been selected, and if not, display a message that a selection needs made       //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(dropdownData.getIsRequired())
            {
               if(!input.getQueryParams().containsKey(name) || !StringUtils.hasContent(input.getQueryParams().get(name)))
               {
                  missingRequiredSelections.add(dropdownData.getLabel());
               }
            }
         }
         else
         {
            String possibleValueSourceName = dropdownData.getPossibleValueSourceName();
            if(possibleValueSourceName != null)
            {
               QPossibleValueSource possibleValueSource = QContext.getQInstance().getPossibleValueSource(possibleValueSourceName);

               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // this looks complicated, but is just look for a label in the dropdown data and if found use it,                                                                                  //
               // otherwise look for label in PVS and if found use that, otherwise just use the PVS name                                                                                          //
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               String pvsLabel = dropdownData.getLabel() != null ? dropdownData.getLabel() : (possibleValueSource.getLabel() != null ? possibleValueSource.getLabel() : possibleValueSourceName);
               labelList.add(pvsLabel);
               nameList.add(possibleValueSourceName);

               SearchPossibleValueSourceInput pvsInput = new SearchPossibleValueSourceInput();
               pvsInput.setPossibleValueSourceName(possibleValueSourceName);

               if(dropdownData.getForeignKeyFieldName() != null)
               {
                  ////////////////////////////////////////
                  // look for an id in the query params //
                  ////////////////////////////////////////
                  Integer id = null;
                  if(input.getQueryParams() != null && input.getQueryParams().containsKey("id") && StringUtils.hasContent(input.getQueryParams().get("id")))
                  {
                     id = Integer.parseInt(input.getQueryParams().get("id"));
                  }
                  if(id != null)
                  {
                     pvsInput.setDefaultQueryFilter(new QQueryFilter().withCriteria(
                        new QFilterCriteria(
                           dropdownData.getForeignKeyFieldName(),
                           QCriteriaOperator.EQUALS,
                           id)));
                  }
               }

               SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(pvsInput);

               List<Map<String, String>> dropdownOptionList = new ArrayList<>();
               dataList.add(dropdownOptionList);

               //////////////////////////////////////////
               // sort results, dedupe, and add to map //
               //////////////////////////////////////////
               Set<String> exists = new HashSet<>();
               output.getResults().removeIf(pvs -> !exists.add(pvs.getLabel()));
               for(QPossibleValue<?> possibleValue : output.getResults())
               {
                  dropdownOptionList.add(MapBuilder.of(
                     "id", String.valueOf(possibleValue.getId()),
                     "label", possibleValue.getLabel()
                  ));
               }

               ////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // because we know the dropdowns and what the field names will be when something is selected, we can make //
               // sure that something has been selected, and if not, display a message that a selection needs made       //
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(dropdownData.getIsRequired())
               {
                  if(!input.getQueryParams().containsKey(possibleValueSourceName) || !StringUtils.hasContent(input.getQueryParams().get(possibleValueSourceName)))
                  {
                     missingRequiredSelections.add(pvsLabel);
                  }
               }
            }
         }
      }

      widgetData.setDropdownNameList(nameList);
      widgetData.setDropdownLabelList(labelList);
      widgetData.setDropdownDataList(dataList);

      ////////////////////////////////////////////////////////////////////////////////
      // if there are any missing required dropdowns, build up a message to display //
      ////////////////////////////////////////////////////////////////////////////////
      if(missingRequiredSelections.size() > 0)
      {
         StringBuilder sb = new StringBuilder("Please select a ").append(StringUtils.joinWithCommasAndAnd(missingRequiredSelections));
         sb.append(" from the ").append(StringUtils.plural(missingRequiredSelections.size(), "dropdown", "dropdowns")).append(" above.");
         widgetData.setDropdownNeedsSelectedText(sb.toString());
         return (false);
      }
      else
      {
         return (true);
      }
   }
}
