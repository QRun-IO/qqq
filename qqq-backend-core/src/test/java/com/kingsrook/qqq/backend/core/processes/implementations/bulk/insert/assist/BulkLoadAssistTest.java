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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist;


import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for BulkLoadAssist (assisted bulk load: profile, suggest, dry run).
 *******************************************************************************/
class BulkLoadAssistTest extends BaseTest
{
   private static final String TABLE = TestUtils.TABLE_NAME_PERSON_MEMORY;

   private static final String MESSY_CSV = """
      First Name,Last Name,Birth Date,E-mail,No Of Shoes,Notes
      John,Doe,1980-01-01,john@doe.com,2,first note
      Jane,Roe,1981-02-02,jane@roe.com,many,second note
      Jim,Poe,1982-03-03,jim@poe.com,4,third note
      """;

   private static final String NO_HEADER_CSV = """
      John,1980-01-01,7
      Jane,1981-02-02,8
      """;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
      BulkLoadUploadSources.clear();
   }



   /*******************************************************************************
    ** Store files in the memory storage table and register a source that knows
    ** them by ref.
    *******************************************************************************/
   private void upload(Map<String, String> refsToReferences, Map<String, String> contents) throws Exception
   {
      for(Map.Entry<String, String> entry : contents.entrySet())
      {
         StorageInput storageInput = new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(entry.getKey());
         try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
         {
            outputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
         }
      }
      BulkLoadUploadSources.register(ref ->
      {
         String reference = refsToReferences.get(ref);
         if(reference == null)
         {
            throw (new QNotFoundException("Upload not found."));
         }
         return (new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(reference));
      });
   }



   /*******************************************************************************
    ** Classify birthDate and noOfShoes as PHI, email as PUBLIC.
    *******************************************************************************/
   private void classify()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TABLE);
      table.getField("birthDate").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
      table.getField("noOfShoes").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
      table.getField("email").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PUBLIC));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoUploadSourceAndUnknownTable()
   {
      assertThatThrownBy(() -> new BulkLoadAssist().suggestMapping(TABLE, "ref-1", false))
         .isInstanceOf(QUserFacingException.class)
         .hasMessage(BulkLoadUploadSources.NOT_CONFIGURED_MESSAGE);
      assertThatThrownBy(() -> new BulkLoadAssist().suggestMapping("notATable", "ref-1", false))
         .isInstanceOf(QNotFoundException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProfileHeaderGuessAndPhiRule() throws Exception
   {
      classify();
      upload(Map.of("ref-1", "uploads/1/people.csv", "ref-2", "uploads/2/raw.csv"), Map.of("uploads/1/people.csv", MESSY_CSV, "uploads/2/raw.csv", NO_HEADER_CSV));

      BulkLoadAssistModels.UploadProfile standard = new BulkLoadAssist().profile(TABLE, "ref-1", BulkLoadAiRoute.STANDARD, 3);
      assertThat(standard.fileName()).isEqualTo("people.csv");
      assertThat(standard.hasHeaderRow()).isTrue();
      assertThat(standard.rowsProfiled()).isEqualTo(3);
      Map<String, BulkLoadAssistModels.ColumnProfile> byHeader = new HashMap<>();
      standard.columns().forEach(c -> byHeader.put(c.header(), c));

      /////////////////////////////////////////////////////////////////
      // unclassified mapped column: samples; PHI: none; PUBLIC:      //
      // samples; unmapped (Notes): none (fail closed)                //
      /////////////////////////////////////////////////////////////////
      assertThat(byHeader.get("First Name").mappedFieldName()).isEqualTo("firstName");
      assertThat(byHeader.get("First Name").sampleValues()).containsExactly("John", "Jane", "Jim");
      assertThat(byHeader.get("Birth Date").classification()).isEqualTo("PHI");
      assertThat(byHeader.get("Birth Date").samplesAllowed()).isFalse();
      assertThat(byHeader.get("Birth Date").sampleValues()).isEmpty();
      assertThat(byHeader.get("Birth Date").inferredType()).isEqualTo("DATE");
      assertThat(byHeader.get("Birth Date").patternClass()).isEqualTo("DATE_LIKE");
      assertThat(byHeader.get("E-mail").patternClass()).isEqualTo("EMAIL_LIKE");
      assertThat(byHeader.get("Notes").mappedFieldName()).isNull();
      assertThat(byHeader.get("Notes").sampleValues()).isEmpty();
      assertThat(byHeader.get("No Of Shoes").inferredType()).isEqualTo("STRING");
      assertThat(byHeader.get("No Of Shoes").sampleValues()).isEmpty();
      assertThat(standard.toString()).doesNotContain("1980-01-01").doesNotContain("first note").doesNotContain("many");

      BulkLoadAssistModels.UploadProfile hipaa = new BulkLoadAssist().profile(TABLE, "ref-1", BulkLoadAiRoute.HIPAA, 2);
      hipaa.columns().forEach(c -> byHeader.put(c.header(), c));
      assertThat(byHeader.get("Birth Date").sampleValues()).containsExactly("1980-01-01", "1981-02-02");
      assertThat(byHeader.get("Notes").sampleValues()).containsExactly("first note", "second note");

      /////////////////////////////////////////////////////////////
      // nothing in row 1 maps: no header row, columns lettered  //
      /////////////////////////////////////////////////////////////
      BulkLoadAssistModels.UploadProfile raw = new BulkLoadAssist().profile(TABLE, "ref-2", BulkLoadAiRoute.STANDARD, 3);
      assertThat(raw.hasHeaderRow()).isFalse();
      assertThat(raw.rowsProfiled()).isEqualTo(2);
      assertThat(raw.columns()).extracting(BulkLoadAssistModels.ColumnProfile::header).containsExactly("Column A", "Column B", "Column C");
      assertThat(raw.columns().get(2).inferredType()).isEqualTo("INTEGER");
      assertThat(raw.columns()).allMatch(c -> c.sampleValues().isEmpty());

      assertThatThrownBy(() -> new BulkLoadAssist().profile(TABLE, "nope", BulkLoadAiRoute.STANDARD, 3)).isInstanceOf(QNotFoundException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuggestMapping() throws Exception
   {
      upload(Map.of("ref-1", "uploads/1/people.csv"), Map.of("uploads/1/people.csv", MESSY_CSV));
      BulkLoadAssistModels.Suggestion suggestion = new BulkLoadAssist().suggestMapping(TABLE, "ref-1", false);
      assertThat(suggestion.profile().getFieldList()).extracting(BulkLoadProfileField::getFieldName).contains("firstName", "lastName", "birthDate", "noOfShoes");
      assertThat(suggestion.unmappedColumns()).extracting(BulkLoadAssistModels.ColumnRef::header).contains("Notes");
      assertThat(suggestion.valueMappingFields()).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateSampleAndFullStoreNothing() throws Exception
   {
      upload(Map.of("ref-1", "uploads/1/people.csv"), Map.of("uploads/1/people.csv", MESSY_CSV));
      BulkLoadProfile profile = new BulkLoadAssist().suggestMapping(TABLE, "ref-1", false).profile();

      BulkLoadAssistModels.Validation sample = new BulkLoadAssist().validate(TABLE, "ref-1", profile, BulkLoadAssist.Mode.SAMPLE, BulkLoadAiRoute.STANDARD);
      assertThat(sample.previewRecordCount()).isEqualTo(3);
      assertThat(sample.errorCount()).isEqualTo(1);
      assertThat(sample.okCount()).isEqualTo(2);
      assertThat(sample.errors()).hasSize(1);
      assertThat(sample.errors().get(0).rowNumbers()).isEqualTo("Row 3");
      assertThat(sample.errors().get(0).fieldName()).isEqualTo("noOfShoes");
      assertThat(sample.errors().get(0).errorCode()).isEqualTo("VALUE_TYPE");
      assertThat(sample.errors().get(0).message()).contains("No Of Shoes");
      assertThat(sample.summaryLines()).anyMatch(l -> l.bulletsOfText().contains("Row 3 [many]"));

      BulkLoadAssistModels.Validation full = new BulkLoadAssist().validate(TABLE, "ref-1", profile, BulkLoadAssist.Mode.FULL, BulkLoadAiRoute.STANDARD);
      assertThat(full.mode()).isEqualTo("FULL");
      assertThat(full.errorCount()).isEqualTo(1);
      assertThat(full.okCount()).isEqualTo(2);
      assertThat(full.summaryLines()).anyMatch(l -> "ERROR".equals(l.status()) && l.message().contains("No Of Shoes"));
      assertThat(full.jobId()).isNull();

      //////////////////////////////
      // a dry run stores nothing //
      //////////////////////////////
      assertThat(TestUtils.queryTable(TABLE)).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateMasksClassifiedValues() throws Exception
   {
      classify();
      upload(Map.of("ref-1", "uploads/1/people.csv"), Map.of("uploads/1/people.csv", MESSY_CSV));
      BulkLoadProfile profile = new BulkLoadAssist().suggestMapping(TABLE, "ref-1", false).profile();

      BulkLoadAssistModels.Validation standard = new BulkLoadAssist().validate(TABLE, "ref-1", profile, BulkLoadAssist.Mode.SAMPLE, BulkLoadAiRoute.STANDARD);
      assertThat(standard.errors().get(0).rowNumbers()).isEqualTo("Row 3");
      assertThat(standard.toString()).doesNotContain("many");
      BulkLoadAssistModels.Validation full = new BulkLoadAssist().validate(TABLE, "ref-1", profile, BulkLoadAssist.Mode.FULL, BulkLoadAiRoute.STANDARD);
      assertThat(full.toString()).doesNotContain("many");

      BulkLoadAssistModels.Validation hipaa = new BulkLoadAssist().validate(TABLE, "ref-1", profile, BulkLoadAssist.Mode.SAMPLE, BulkLoadAiRoute.HIPAA);
      assertThat(hipaa.toString()).contains("Row 3 [many]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateRejectsBadProfilesAndNoPermission() throws Exception
   {
      upload(Map.of("ref-1", "uploads/1/people.csv"), Map.of("uploads/1/people.csv", MESSY_CSV));
      BulkLoadProfile profile = new BulkLoadAssist().suggestMapping(TABLE, "ref-1", false).profile();

      assertThatThrownBy(() -> new BulkLoadAssist().validate(TABLE, "ref-1", new BulkLoadProfile(), BulkLoadAssist.Mode.SAMPLE, BulkLoadAiRoute.STANDARD))
         .isInstanceOf(QUserFacingException.class);
      BulkLoadProfile bad = new BulkLoadAssist().suggestMapping(TABLE, "ref-1", false).profile();
      bad.getFieldList().get(0).setFieldName("noSuchField");
      assertThatThrownBy(() -> new BulkLoadAssist().validate(TABLE, "ref-1", bad, BulkLoadAssist.Mode.SAMPLE, BulkLoadAiRoute.STANDARD))
         .isInstanceOf(QUserFacingException.class).hasMessageContaining("noSuchField");

      QContext.getQInstance().getProcess(TABLE + ".bulkInsert").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      QContext.getQSession().setPermissions(Set.of());
      assertThatThrownBy(() -> new BulkLoadAssist().validate(TABLE, "ref-1", profile, BulkLoadAssist.Mode.SAMPLE, BulkLoadAiRoute.STANDARD))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new BulkLoadAssist().profile(TABLE, "ref-1", BulkLoadAiRoute.STANDARD, 3))
         .isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRouteAndLetters()
   {
      assertThat(BulkLoadAiRoute.fromHeader("HIPAA")).isEqualTo(BulkLoadAiRoute.HIPAA);
      assertThat(BulkLoadAiRoute.fromHeader(" hipaa ")).isEqualTo(BulkLoadAiRoute.HIPAA);
      assertThat(BulkLoadAiRoute.fromHeader("standard")).isEqualTo(BulkLoadAiRoute.STANDARD);
      assertThat(BulkLoadAiRoute.fromHeader(null)).isEqualTo(BulkLoadAiRoute.STANDARD);
      assertThat(BulkLoadAssist.columnLetter(0)).isEqualTo("A");
      assertThat(BulkLoadAssist.columnLetter(25)).isEqualTo("Z");
      assertThat(BulkLoadAssist.columnLetter(26)).isEqualTo("AA");
   }
}
