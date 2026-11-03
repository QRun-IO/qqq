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

package com.kingsrook.sampleapp;


import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.AdornmentLabRelationsMetaDataProducer;
import com.kingsrook.sampleapp.metadata.AdornmentLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The Adornment Lab fixture gives clients something live for every adornment,
 ** hidden fields and sections, help content, an exposed join, a composite-key
 ** association and a denied child read (QRun-IO/qqq#1350).
 *******************************************************************************/
class SampleAdornmentLabFixtureTest
{
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Every adornment type, a hidden field and section, and help in each place.
    *******************************************************************************/
   @Test
   void testMetaDataCoversEveryAdornmentHiddenThingAndHelp()
   {
      QTableMetaData table = instance.getTable(AdornmentLabTableMetaDataProducer.NAME);

      Set<AdornmentType> adornments = table.getFields().values().stream()
         .flatMap(field -> field.getAdornments() == null ? Stream.empty() : field.getAdornments().stream())
         .map(FieldAdornment::getType)
         .collect(Collectors.toSet());
      assertEquals(Set.of(AdornmentType.values()), adornments, "missing: " + Arrays.stream(AdornmentType.values()).filter(type -> !adornments.contains(type)).toList());

      assertTrue(table.getField("hiddenValue").getIsHidden());
      QFieldSection internals = table.getSections().stream().filter(section -> section.getName().equals("internals")).findFirst().orElseThrow();
      assertTrue(internals.getIsHidden());

      assertFalse(table.getHelpContent().isEmpty());
      assertTrue(table.getSections().stream().anyMatch(section -> section.getHelpContents() != null && !section.getHelpContents().isEmpty()));
      assertTrue(table.getFields().values().stream().map(QFieldMetaData::getHelpContents).anyMatch(help -> help != null && !help.isEmpty()));

      assertEquals(SampleMetaDataProvider.TABLE_NAME_CARRIER, table.getExposedJoins().get(0).getJoinTable());
      assertEquals(2, instance.getJoin(AdornmentLabRelationsMetaDataProducer.JOIN_TAG).getJoinOns().size());
   }



   /*******************************************************************************
    ** Tags join on both id and normalizedKey: Beta's tag keyed ALPHA is not Beta's.
    *******************************************************************************/
   @Test
   void testCompositeAssociationAndExposedJoinData() throws Exception
   {
      List<QRecord> records = new QueryAction().execute(new QueryInput(AdornmentLabTableMetaDataProducer.NAME)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 2))).withOrderBy(new QFilterOrderBy("id")))
         .withIncludeAssociations(true)).getRecords();
      assertEquals(List.of("first", "featured"), records.get(0).getAssociatedRecords().get("tags").stream().map(tag -> tag.getValueString("tag")).toList());
      assertEquals(List.of("second"), records.get(1).getAssociatedRecords().get("tags").stream().map(tag -> tag.getValueString("tag")).toList());
   }



   /*******************************************************************************
    ** A session without permissions (as mock sessions are) may read the locker
    ** but not its secrets.
    *******************************************************************************/
   @Test
   void testSecretsAreDeniedToTheMockUser()
   {
      assertDoesNotThrow(() -> PermissionsHelper.checkTablePermissionThrowing(new QueryInput(AdornmentLabRelationsMetaDataProducer.TABLE_LOCKER), TablePermissionSubType.READ));
      assertThrows(QPermissionDeniedException.class, () -> PermissionsHelper.checkTablePermissionThrowing(new QueryInput(AdornmentLabRelationsMetaDataProducer.TABLE_SECRET), TablePermissionSubType.READ));
   }
}
