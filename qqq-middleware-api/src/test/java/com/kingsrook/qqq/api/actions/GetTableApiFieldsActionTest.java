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

package com.kingsrook.qqq.api.actions;


import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.DATE_TIME;
import static com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for GetTableApiFieldsAction
 *******************************************************************************/
class GetTableApiFieldsActionTest extends BaseTest
{
   private static final String TABLE_NAME = "testTable";

   Function<List<? extends QFieldMetaData>, Set<String>> fieldListToNameSet = l -> l.stream().map(f -> f.getName()).collect(Collectors.toSet());



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<? extends QFieldMetaData> getFields(String tableName, String version) throws QException
   {
      return (getFields(TestUtils.API_NAME, tableName, version));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<? extends QFieldMetaData> getFields(String apiName, String tableName, String version) throws QException
   {
      return (getFields(apiName, tableName, version, QInputSource.SYSTEM));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<? extends QFieldMetaData> getFields(String apiName, String tableName, String version, InputSource inputSource) throws QException
   {
      return new GetTableApiFieldsAction().execute(new GetTableApiFieldsInput().withApiName(apiName).withTableName(tableName).withVersion(version).withInputSource(inputSource)).getFields();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAdditions() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion("1")))
         .withField(new QFieldMetaData("a", STRING)) // inherit versionRange from the table
         .withField(new QFieldMetaData("b", STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion("1"))))
         .withField(new QFieldMetaData("c", STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion("2"))))
         .withField(new QFieldMetaData("d", STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion("3"))))
      );
      new QInstanceEnricher(qInstance).enrich();

      assertEquals(Set.of("a", "b"), fieldListToNameSet.apply(getFields(TABLE_NAME, "1")));
      assertEquals(Set.of("a", "b", "c"), fieldListToNameSet.apply(getFields(TABLE_NAME, "2")));
      assertEquals(Set.of("a", "b", "c", "d"), fieldListToNameSet.apply(getFields(TABLE_NAME, "3")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRemoval() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion("1")
            .withRemovedApiField(new QFieldMetaData("c", STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion("1").withFinalVersion("2"))))
         ))
         .withField(new QFieldMetaData("a", STRING)) // inherit versionRange from the table
         .withField(new QFieldMetaData("b", STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion("1"))))
         // we used to have "c" here... now it's in the removed list above!
         .withField(new QFieldMetaData("d", STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion("3"))))
      );
      new QInstanceEnricher(qInstance).enrich();

      assertEquals(Set.of("a", "b", "c"), fieldListToNameSet.apply(getFields(TABLE_NAME, "1")));
      assertEquals(Set.of("a", "b", "c"), fieldListToNameSet.apply(getFields(TABLE_NAME, "2")));
      assertEquals(Set.of("a", "b", "d"), fieldListToNameSet.apply(getFields(TABLE_NAME, "3")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablesNotFound() throws QException
   {
      String    tableNameVersion2plus = "tableNameVersion2plus";
      QInstance qInstance             = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData()
         .withName(tableNameVersion2plus)
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion("2")))
         .withField(new QFieldMetaData("a", STRING)));

      String tableNameVersion2through4 = "tableNameVersion2through4";
      qInstance.addTable(new QTableMetaData()
         .withName(tableNameVersion2through4)
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion("2").withFinalVersion("4")))
         .withField(new QFieldMetaData("a", STRING)));

      String tableNameNoApis = "tableNameNoApis";
      qInstance.addTable(new QTableMetaData()
         .withName(tableNameNoApis)
         .withField(new QFieldMetaData("a", STRING)));

      new QInstanceEnricher(qInstance).enrich();

      assertThatThrownBy(() -> getFields("no-such-table", "1")).isInstanceOf(QNotFoundException.class);

      assertThatThrownBy(() -> getFields(tableNameVersion2plus, "1")).isInstanceOf(QNotFoundException.class);
      getFields(tableNameVersion2plus, "2");
      assertThatThrownBy(() -> getFields("noSuchApi", tableNameVersion2plus, "2")).isInstanceOf(QNotFoundException.class);

      assertThatThrownBy(() -> getFields(tableNameVersion2through4, "1")).isInstanceOf(QNotFoundException.class);
      getFields(tableNameVersion2through4, "2");
      getFields(tableNameVersion2through4, "3");
      getFields(tableNameVersion2through4, "4");
      assertThatThrownBy(() -> getFields(tableNameVersion2through4, "5")).isInstanceOf(QNotFoundException.class);

      assertThatThrownBy(() -> getFields(tableNameNoApis, "1")).isInstanceOf(QNotFoundException.class);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // test the withDoCheckTableApiVersion input flag.                                                   //
      // set up an input that'll fail (verify it fails) - then set the flag to false and make sure no fail //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      GetTableApiFieldsInput input = new GetTableApiFieldsInput().withApiName(TestUtils.API_NAME).withTableName(tableNameVersion2through4).withVersion("1");
      assertThatThrownBy(() -> new GetTableApiFieldsAction().execute(input));
      new GetTableApiFieldsAction().execute(input.withDoCheckTableApiVersion(false));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablePersonalization() throws QException
   {
      ///////////////////////////////////////////////////
      // set up our table with just a createDate field //
      ///////////////////////////////////////////////////
      QInstance qInstance = QContext.getQInstance();
      String version = "1";
      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion(version)))
         .withField(new QFieldMetaData("createDate", DATE_TIME)));
      new QInstanceEnricher(qInstance).enrich();

      //////////////////////////////////////////////////////////////
      // make sure we get it (since no personalization is active) //
      //////////////////////////////////////////////////////////////
      assertThat(fieldListToNameSet.apply(getFields(TestUtils.API_NAME, TABLE_NAME, version, QInputSource.SYSTEM))).contains("createDate");
      assertThat(fieldListToNameSet.apply(getFields(TestUtils.API_NAME, TABLE_NAME, version, QInputSource.USER))).contains("createDate");

      try
      {
         TestUtils.TablePersonalizer.register(QContext.getQInstance());

         //////////////////////////////////////////////////////////////////////////
         // with personalization active, a user-request shouldn't get it (though //
         // system still should, based on rules inside the personalizer)         //
         //////////////////////////////////////////////////////////////////////////
         assertThat(fieldListToNameSet.apply(getFields(TestUtils.API_NAME, TABLE_NAME, version, QInputSource.SYSTEM))).contains("createDate");
         assertThat(fieldListToNameSet.apply(getFields(TestUtils.API_NAME, TABLE_NAME, version, QInputSource.USER))).doesNotContain("createDate");
      }
      finally
      {
         TestUtils.TablePersonalizer.unregister(QContext.getQInstance());
      }
   }

}