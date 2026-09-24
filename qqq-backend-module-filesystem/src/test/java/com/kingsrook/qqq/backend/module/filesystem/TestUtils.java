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

package com.kingsrook.qqq.backend.module.filesystem;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.mock.MockBackendModule;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLProcess;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.FilesystemTableMetaDataBuilder;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.processes.implementations.etl.streamed.StreamedETLFilesystemBackendStep;
import com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.importer.FilesystemImporterMetaDataTemplate;
import com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.importer.FilesystemImporterProcessMetaDataBuilder;
import com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.importer.FilesystemImporterStep;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendVariantSetting;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPTableBackendDetails;
import org.apache.commons.io.FileUtils;


/*******************************************************************************
 ** Utility methods for Filesystem module tests
 *******************************************************************************/
public class TestUtils
{
   public static final String BACKEND_NAME_LOCAL_FS           = "local-filesystem";
   public static final String BACKEND_NAME_S3                 = "s3";
   public static final String BACKEND_NAME_S3_SANS_PREFIX     = "s3sansPrefix";
   public static final String BACKEND_NAME_SFTP               = "sftp";
   public static final String BACKEND_NAME_SFTP_WITH_VARIANTS = "sftpWithVariants";
   public static final String BACKEND_NAME_MOCK               = "mock";
   public static final String BACKEND_NAME_MEMORY             = "memory";

   public static final String TABLE_NAME_PERSON_LOCAL_FS_JSON = "person-local-json";
   public static final String TABLE_NAME_PERSON_LOCAL_FS_CSV  = "person-local-csv";
   public static final String TABLE_NAME_BLOB_LOCAL_FS        = "local-blob";
   public static final String TABLE_NAME_ARCHIVE_LOCAL_FS     = "local-archive";
   public static final String TABLE_NAME_PERSON_S3            = "person-s3";
   public static final String TABLE_NAME_PERSON_SFTP          = "person-sftp";
   public static final String TABLE_NAME_BLOB_S3              = "s3-blob";
   public static final String TABLE_NAME_PERSON_MOCK          = "person-mock";
   public static final String TABLE_NAME_BLOB_S3_SANS_PREFIX  = "s3-blob-sans-prefix";
   public static final String TABLE_NAME_SFTP_FILE            = "sftp-file";
   public static final String TABLE_NAME_SFTP_FILE_VARIANTS   = "sftp-file-with-variants";
   public static final String TABLE_NAME_VARIANT_OPTIONS      = "variant-options-table";

   public static final String PROCESS_NAME_STREAMED_ETL                   = "etl.streamed";
   public static final String LOCAL_PERSON_CSV_FILE_IMPORTER_PROCESS_NAME = "localPersonCsvFileImporter";

   ///////////////////////////////////////////////////////////////////
   // shouldn't be accessed directly, as we append a counter to it. //
   ///////////////////////////////////////////////////////////////////
   public static final String BASE_PATH = "/tmp/filesystem-tests";

   //////////////////////////////////////////////////////////////////////////////
   // Used to make each test method have a unique folder path, more or less... //
   // See methods that work with it.                                           //
   //////////////////////////////////////////////////////////////////////////////
   private static int testInstanceCounter = 0;



   /*******************************************************************************
    ** Meant to be called in a @BeforeEach - increment an internal counter that will
    ** give us a unique directory name for each test method.
    *******************************************************************************/
   public static void increaseTestInstanceCounter()
   {
      testInstanceCounter++;
   }



   /*******************************************************************************
    ** Reset the counter to 0 (e.g., to let some tests have a known value).
    *******************************************************************************/
   public static void resetTestInstanceCounter()
   {
      testInstanceCounter = 0;
   }



   /*******************************************************************************
    ** Get the current value of the testInstanceCounter.  See {@link #increaseTestInstanceCounter()}
    *******************************************************************************/
   public static int getTestInstanceCounter()
   {
      return (testInstanceCounter);
   }



   /*******************************************************************************
    ** Meant to run both after and before test methods - makes sure the file system
    ** is empty for the path under the instance.
    *******************************************************************************/
   public static void cleanInstanceFiles() throws IOException
   {
      FileUtils.deleteDirectory(new File(BASE_PATH + File.separator + testInstanceCounter));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QInstance defineInstance() throws QException
   {
      QInstance qInstance = new QInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), defineAuthentication());
      qInstance.addBackend(defineLocalFilesystemBackend());
      qInstance.addTable(defineLocalFilesystemJSONPersonTable());
      qInstance.addTable(defineLocalFilesystemCSVPersonTable());
      qInstance.addTable(defineLocalFilesystemBlobTable());
      qInstance.addTable(defineLocalFilesystemArchiveTable());
      qInstance.addBackend(defineS3Backend());
      qInstance.addBackend(defineS3BackendSansPrefix());
      qInstance.addTable(defineS3CSVPersonTable());
      qInstance.addTable(defineSFTPCSVPersonTable());
      qInstance.addTable(defineS3BlobTable());
      qInstance.addTable(defineS3BlobSansPrefixTable());
      qInstance.addBackend(defineMockBackend());
      qInstance.addBackend(defineMemoryBackend());
      qInstance.addTable(defineMockPersonTable());
      qInstance.addProcess(defineStreamedLocalCsvToMockETLProcess());

      QBackendMetaData sftpBackend = defineSFTPBackend();
      qInstance.addBackend(sftpBackend);
      qInstance.addTable(defineSFTPFileTable(sftpBackend));

      QBackendMetaData sftpBackendWithVariants = defineSFTPBackendWithVariants();
      qInstance.addBackend(sftpBackendWithVariants);
      qInstance.addTable(defineSFTPFileTableWithVariants(sftpBackendWithVariants));
      qInstance.addTable(defineVariantOptionsTable());

      definePersonCsvImporter(qInstance);

      return (qInstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QTableMetaData defineVariantOptionsTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_VARIANT_OPTIONS)
         .withBackendName(defineMemoryBackend().getName())
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("basePath", QFieldType.STRING));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void definePersonCsvImporter(QInstance qInstance)
   {
      String importBaseName = "personImporter";
      FilesystemImporterProcessMetaDataBuilder filesystemImporterProcessMetaDataBuilder = (FilesystemImporterProcessMetaDataBuilder) new FilesystemImporterProcessMetaDataBuilder()
         .withSourceTableName(TABLE_NAME_PERSON_LOCAL_FS_CSV)
         .withFileFormat("csv")
         .withArchiveFileEnabled(true)
         .withArchiveTableName(TABLE_NAME_ARCHIVE_LOCAL_FS)
         .withArchivePath("archive-of/personImporterFiles")
         .withName(LOCAL_PERSON_CSV_FILE_IMPORTER_PROCESS_NAME);

      FilesystemImporterMetaDataTemplate filesystemImporterMetaDataTemplate = new FilesystemImporterMetaDataTemplate(qInstance, importBaseName, BACKEND_NAME_MEMORY, filesystemImporterProcessMetaDataBuilder, table -> table.withAuditRules(QAuditRules.defaultInstanceLevelNone()));
      filesystemImporterMetaDataTemplate.getImporterProcessMetaDataBuilder().withInputFieldDefaultValue(FilesystemImporterStep.FIELD_MOVE_UNSTRUCTURED_FIELDS_TO_VALUES_JSON, true);
      filesystemImporterMetaDataTemplate.addToInstance(qInstance);
   }



   /*******************************************************************************
    ** Define the authentication used in standard tests - using 'mock' type.
    **
    *******************************************************************************/
   public static QAuthenticationMetaData defineAuthentication()
   {
      return new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static FilesystemBackendMetaData defineLocalFilesystemBackend()
   {
      return (new FilesystemBackendMetaData()
         .withBasePath(BASE_PATH + File.separator + testInstanceCounter)
         .withName(BACKEND_NAME_LOCAL_FS));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineLocalFilesystemJSONPersonTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSON_LOCAL_FS_JSON)
         .withLabel("Person")
         .withBackendName(defineLocalFilesystemBackend().getName())
         .withPrimaryKeyField("id")
         .withFields(defineCommonPersonTableFields())
         .withBackendDetails(new FilesystemTableBackendDetails()
            .withBasePath("persons")
            .withRecordFormat(RecordFormat.JSON)
            .withCardinality(Cardinality.MANY)
            .withGlob("*.json")
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QFieldMetaData> defineCommonPersonTableFields()
   {
      return (List.of(
         new QFieldMetaData("id", QFieldType.INTEGER),
         new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date"),
         new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date"),
         new QFieldMetaData("firstName", QFieldType.STRING).withBackendName("first_name"),
         new QFieldMetaData("lastName", QFieldType.STRING).withBackendName("last_name"),
         new QFieldMetaData("birthDate", QFieldType.DATE).withBackendName("birth_date"),
         new QFieldMetaData("email", QFieldType.STRING)
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineLocalFilesystemCSVPersonTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSON_LOCAL_FS_CSV)
         .withLabel("Person")
         .withBackendName(defineLocalFilesystemBackend().getName())
         .withPrimaryKeyField("id")
         .withFields(defineCommonPersonTableFields())
         .withBackendDetails(new FilesystemTableBackendDetails()
            .withBasePath("persons-csv")
            .withRecordFormat(RecordFormat.CSV)
            .withCardinality(Cardinality.MANY)
            .withGlob("*.csv")
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineLocalFilesystemBlobTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_BLOB_LOCAL_FS)
         .withLabel("Blob")
         .withBackendName(defineLocalFilesystemBackend().getName())
         .withPrimaryKeyField("fileName")
         .withField(new QFieldMetaData("fileName", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB))
         .withBackendDetails(new FilesystemTableBackendDetails()
            .withBasePath("blobs")
            .withCardinality(Cardinality.ONE)
            .withFileNameFieldName("fileName")
            .withContentsFieldName("contents")
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineLocalFilesystemArchiveTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_ARCHIVE_LOCAL_FS)
         .withLabel("Archive")
         .withBackendName(defineLocalFilesystemBackend().getName())
         .withPrimaryKeyField("fileName")
         .withField(new QFieldMetaData("fileName", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB))
         .withBackendDetails(new FilesystemTableBackendDetails()
            .withBasePath("archive")
            .withCardinality(Cardinality.ONE)
            .withFileNameFieldName("fileName")
            .withContentsFieldName("contents")
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineS3BlobTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_BLOB_S3)
         .withLabel("Blob S3")
         .withBackendName(defineS3Backend().getName())
         .withPrimaryKeyField("fileName")
         .withField(new QFieldMetaData("fileName", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB))
         .withBackendDetails(new S3TableBackendDetails()
            .withContentTypeStrategy(S3TableBackendDetails.ContentTypeStrategy.BASED_ON_FILE_NAME)
            .withBasePath("blobs")
            .withCardinality(Cardinality.ONE)
            .withFileNameFieldName("fileName")
            .withContentsFieldName("contents")
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineS3BlobSansPrefixTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_BLOB_S3_SANS_PREFIX)
         .withLabel("Blob S3")
         .withBackendName(defineS3BackendSansPrefix().getName())
         .withPrimaryKeyField("fileName")
         .withField(new QFieldMetaData("fileName", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB))
         .withBackendDetails(new S3TableBackendDetails()
            .withCardinality(Cardinality.ONE)
            .withFileNameFieldName("fileName")
            .withContentsFieldName("contents")
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static S3BackendMetaData defineS3Backend()
   {
      return (new S3BackendMetaData()
         .withBucketName(BaseS3Test.BUCKET_NAME)
         .withBasePath(BaseS3Test.TEST_FOLDER)
         .withName(BACKEND_NAME_S3));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static S3BackendMetaData defineS3BackendSansPrefix()
   {
      return (new S3BackendMetaData()
         .withBucketName(BaseS3Test.BUCKET_NAME_FOR_SANS_PREFIX_BACKEND)
         .withName(BACKEND_NAME_S3_SANS_PREFIX));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineSFTPCSVPersonTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSON_SFTP)
         .withLabel("Person SFTP Table")
         .withBackendName(BACKEND_NAME_SFTP)
         .withPrimaryKeyField("id")
         .withFields(defineCommonPersonTableFields())
         .withBackendDetails(new SFTPTableBackendDetails()
            .withRecordFormat(RecordFormat.CSV)
            .withCardinality(Cardinality.MANY)
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineS3CSVPersonTable()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSON_S3)
         .withLabel("Person S3 Table")
         .withBackendName(defineS3Backend().getName())
         .withPrimaryKeyField("id")
         .withFields(defineCommonPersonTableFields())
         .withBackendDetails(new S3TableBackendDetails()
            .withRecordFormat(RecordFormat.CSV)
            .withCardinality(Cardinality.MANY)
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QBackendMetaData defineMockBackend()
   {
      return (new QBackendMetaData()
         .withBackendType(MockBackendModule.class)
         .withName(BACKEND_NAME_MOCK));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QBackendMetaData defineMemoryBackend()
   {
      return (new QBackendMetaData()
         .withBackendType(MemoryBackendModule.class)
         .withName(BACKEND_NAME_MEMORY));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineMockPersonTable()
   {
      return (new QTableMetaData()
         .withName(TABLE_NAME_PERSON_MOCK)
         .withLabel("Person Mock Table")
         .withBackendName(BACKEND_NAME_MOCK)
         .withPrimaryKeyField("id")
         .withFields(defineCommonPersonTableFields()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QProcessMetaData defineStreamedLocalCsvToMockETLProcess() throws QException
   {
      QProcessMetaData qProcessMetaData = new StreamedETLProcess().defineProcessMetaData();
      qProcessMetaData.setName(PROCESS_NAME_STREAMED_ETL);
      QBackendStepMetaData backendStep = qProcessMetaData.getBackendStep(StreamedETLProcess.FUNCTION_NAME_ETL);
      backendStep.setCode(new QCodeReference(StreamedETLFilesystemBackendStep.class));

      backendStep.getInputMetaData().getFieldThrowing(StreamedETLProcess.FIELD_SOURCE_TABLE).setDefaultValue(TABLE_NAME_PERSON_LOCAL_FS_CSV);
      backendStep.getInputMetaData().getFieldThrowing(StreamedETLProcess.FIELD_DESTINATION_TABLE).setDefaultValue(TABLE_NAME_PERSON_MOCK);

      return (qProcessMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QSession getMockSession() throws QException
   {
      MockAuthenticationModule mockAuthenticationModule = new MockAuthenticationModule();
      return (mockAuthenticationModule.createSession(defineInstance(), null));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QTableMetaData defineSFTPFileTable(QBackendMetaData sftpBackend)
   {
      return new FilesystemTableMetaDataBuilder()
         .withBasePath(BaseSFTPTest.TABLE_FOLDER)
         .withBackend(sftpBackend)
         .withName(TABLE_NAME_SFTP_FILE)
         .buildStandardCardinalityOneTable()
         .withLabel("SFTP Files");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QBackendMetaData defineSFTPBackend()
   {
      return (new SFTPBackendMetaData()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withHostName(BaseSFTPTest.HOST_NAME)
         .withPort(BaseSFTPTest.getCurrentPort())
         .withBasePath(BaseSFTPTest.BACKEND_FOLDER)
         .withName(BACKEND_NAME_SFTP));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QTableMetaData defineSFTPFileTableWithVariants(QBackendMetaData sftpBackend)
   {
      return new FilesystemTableMetaDataBuilder()
         .withBasePath(BaseSFTPTest.TABLE_FOLDER)
         .withBackend(sftpBackend)
         .withName(TABLE_NAME_SFTP_FILE_VARIANTS)
         .buildStandardCardinalityOneTable()
         .withLabel("SFTP Files");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QBackendMetaData defineSFTPBackendWithVariants()
   {
      return (new SFTPBackendMetaData()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withHostName(BaseSFTPTest.HOST_NAME)
         .withPort(BaseSFTPTest.getCurrentPort())

         ////////////////////////////////////
         // only get basePath from variant //
         ////////////////////////////////////
         .withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig()
            .withOptionsTableName(TABLE_NAME_VARIANT_OPTIONS)
            .withVariantTypeKey(TABLE_NAME_VARIANT_OPTIONS)
            .withBackendSettingSourceFieldNameMap(Map.of(
               SFTPBackendVariantSetting.BASE_PATH, "basePath"
            ))
         )
         .withName(BACKEND_NAME_SFTP_WITH_VARIANTS));
   }
}
