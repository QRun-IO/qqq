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

package com.kingsrook.qqq.backend.core.actions.values;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueDependencyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Dependency discovery preserves order across explicit and inferred sources.
 *******************************************************************************/
class ContingentPossibleValueHelperTest
{
   /*******************************************************************************
    ** Single, multiple, nested and fallback references are all discovered.
    *******************************************************************************/
   @Test
   void dependencies()
   {
      QFieldMetaData field = new QFieldMetaData();
      assertThat(ContingentPossibleValueHelper.getDependencyFieldNames(field)).isEmpty();
      assertThat(ContingentPossibleValueHelper.isContingent(field)).isFalse();
      field.withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("a", QCriteriaOperator.EQUALS, "${input.a}")));
      assertThat(ContingentPossibleValueHelper.getDependencyFieldNames(field)).containsExactly("a");
      field.getPossibleValueSourceFilter().addCriteria(new QFilterCriteria("b", QCriteriaOperator.IN, List.of("${input.b}??${processValues.b}", "${input.a}", "${currentUser}", "${processValues.c}")));
      field.getPossibleValueSourceFilter().addSubFilter(new QQueryFilter(new QFilterCriteria("c", QCriteriaOperator.EQUALS, "${input.c}??${input.d}")));
      field.withPossibleValueSourceDependsOn(new ArrayList<>(List.of("explicit", "a")));
      assertThat(ContingentPossibleValueHelper.getDependencyFieldNames(field)).containsExactly("explicit", "a", "b", "c", "d");
      assertThat(ContingentPossibleValueHelper.isContingent(field)).isTrue();
      QFieldMetaData clone = field.clone();
      clone.getPossibleValueSourceDependsOn().add("extra");
      assertThat(field.getPossibleValueSourceDependsOn()).containsExactly("explicit", "a");
   }



   /*******************************************************************************
    ** Virtual field fluent setters retain the subtype and metadata.
    *******************************************************************************/
   @Test
   void virtualFieldsAndDefaults()
   {
      QVirtualFieldMetaData field = new QVirtualFieldMetaData()
         .withPossibleValueSourceDependsOn(List.of("a"))
         .withPossibleValueSourceDependencyBehavior(PossibleValueDependencyBehavior.SHOW_ALL_UNTIL_SET)
         .withValidatePossibleValueSourceOnWrite(false);
      assertThat(field.getValidatePossibleValueSourceOnWrite()).isFalse();
      assertThat(ContingentPossibleValueHelper.getSearchUseCase(field, "form")).isEqualTo(PossibleValueSearchFilterUseCase.FILTER);
      field.setPossibleValueSourceDependencyBehavior(null);
      field.setValidatePossibleValueSourceOnWrite(true);
      field.setPossibleValueSourceDependsOn(List.of("b"));
      assertThat(ContingentPossibleValueHelper.getSearchUseCase(field, "filter")).isEqualTo(PossibleValueSearchFilterUseCase.FILTER);
      assertThat(ContingentPossibleValueHelper.getSearchUseCase(field, "invalid")).isEqualTo(PossibleValueSearchFilterUseCase.FORM);
      assertThat(ContingentPossibleValueHelper.hasMissingExplicitDependency(field, Map.of(), null)).isTrue();
      assertThat(ContingentPossibleValueHelper.hasMissingExplicitDependency(field, Map.of("b", ""), "form")).isTrue();
      assertThat(ContingentPossibleValueHelper.hasMissingExplicitDependency(field, Map.of("b", new ArrayList<>()), "form")).isTrue();
      assertThat(ContingentPossibleValueHelper.hasMissingExplicitDependency(field, Map.of("b", 1), "form")).isFalse();
      assertThat(ContingentPossibleValueHelper.hasMissingExplicitDependency(field, Map.of(), "filter")).isFalse();
   }



   /*******************************************************************************
    ** A collection substituted into IN is flattened, including nested filters.
    *******************************************************************************/
   @Test
   void collectionInterpolation() throws Exception
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("a", QCriteriaOperator.IN, "${input.a}"));
      filter.interpretValues(Map.of("input", Map.of("a", new ArrayList<>(List.of(1, 2)))), PossibleValueSearchFilterUseCase.FORM);
      assertThat(filter.getCriteria().get(0).getValues()).containsExactly(1, 2);
      QQueryFilter empty = new QQueryFilter(new QFilterCriteria("a", QCriteriaOperator.IN, "${input.a}"));
      empty.interpretValues(Map.of("input", Map.of("a", new ArrayList<>())), PossibleValueSearchFilterUseCase.FILTER);
      assertThat(empty.getCriteria().get(0).getOperator()).isEqualTo(QCriteriaOperator.TRUE);
   }
}
