/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.model;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.EsbTestBase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbInstanceMetaData enrichment (${env.*} interpolation), and
 ** for keeping ESB meta-data out of frontend meta-data.
 *******************************************************************************/
class EsbInstanceMetaDataTest extends EsbTestBase
{
   private static final String SECRET_PASSWORD            = "s3cret-broker-password";
   private static final String SECRET_MANAGEMENT_PASSWORD = "s3cret-management-password";



   /*******************************************************************************
    ** All six connection fields are ${env.*} interpolated during enrichment
    ** (which QInstanceValidator runs before validating).
    *******************************************************************************/
   @Test
   void testEnvInterpolation() throws Exception
   {
      Map.Entry<String, String> envVar      = findAnEnvironmentVariable();
      String                    envVariable = "${env." + envVar.getKey() + "}";

      QInstance qInstance = defineInstance();
      EsbInstanceMetaData.of(qInstance).withProvider(new QEsbProviderMetaData()
         .withName("fromEnv")
         .withType(EsbProviderType.RABBITMQ)
         .withUrl(envVariable)
         .withUsername(envVariable)
         .withPassword(envVariable)
         .withManagementUrl(envVariable)
         .withManagementUsername(envVariable)
         .withManagementPassword(envVariable));

      new QInstanceValidator().validate(qInstance);

      QEsbProviderMetaData provider = EsbInstanceMetaData.of(qInstance).getProvider("fromEnv");
      assertThat(provider.getUrl()).isEqualTo(envVar.getValue());
      assertThat(provider.getUsername()).isEqualTo(envVar.getValue());
      assertThat(provider.getPassword()).isEqualTo(envVar.getValue());
      assertThat(provider.getManagementUrl()).isEqualTo(envVar.getValue());
      assertThat(provider.getManagementUsername()).isEqualTo(envVar.getValue());
      assertThat(provider.getManagementPassword()).isEqualTo(envVar.getValue());

      ///////////////////////////////////////////////////////////////////
      // literal values and the embedded broker's provider are left as //
      // they were; unset optional fields stay unset                   //
      ///////////////////////////////////////////////////////////////////
      QEsbProviderMetaData artemis = EsbInstanceMetaData.of(qInstance).getProvider(PROVIDER_NAME);
      assertThat(artemis.getUrl()).isEqualTo(getBrokerUrl());
      assertThat(artemis.getUsername()).isNull();
      assertThat(artemis.getManagementUrl()).isNull();
   }



   /*******************************************************************************
    ** An env var that isn't set leaves the url empty, which fails validation.
    *******************************************************************************/
   @Test
   void testMissingEnvVariableFailsValidation()
   {
      QInstance qInstance = defineInstance();
      EsbInstanceMetaData.of(qInstance).withProvider(new QEsbProviderMetaData()
         .withName("fromEnv")
         .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
         .withUrl("${env.QQQ_ESB_TEST_VARIABLE_THAT_IS_NOT_SET}"));

      assertThatThrownBy(() -> new QInstanceValidator().validate(qInstance))
         .isInstanceOf(QInstanceValidationException.class)
         .satisfies(e -> assertThat(((QInstanceValidationException) e).getReasons()).anySatisfy(reason -> assertThat(reason).startsWith("ESB provider fromEnv is missing a url")));
   }



   /*******************************************************************************
    ** ESB table meta-data is not in full or partial frontend table meta-data,
    ** and ESB process meta-data is not in frontend process meta-data.
    *******************************************************************************/
   @Test
   void testAbsentFromFrontendTableAndProcessMetaData() throws Exception
   {
      QInstance qInstance = defineInstanceWithSecrets();
      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());

      QTableMetaData table = qInstance.getTable(TABLE_NAME_ORDER);
      assertThat(table.getSupplementalMetaData(EsbTableMetaData.TYPE)).isNotNull();

      for(boolean full : List.of(true, false))
      {
         QFrontendTableMetaData frontendTable = new QFrontendTableMetaData(new AbstractActionInput(), qInstance.getBackendForTable(TABLE_NAME_ORDER), table, full, full);
         assertThat(frontendTable.getSupplementalTableMetaData()).as("full=" + full).satisfiesAnyOf(
            map -> assertThat(map).isNull(),
            map -> assertThat(map).doesNotContainKey(EsbTableMetaData.TYPE));
         assertThat(JsonUtils.toJson(frontendTable)).doesNotContain("orderEvents");
      }

      QProcessMetaData process = qInstance.getProcess(PROCESS_NAME_SYNC_ORDER);
      assertThat(process.getSupplementalMetaData(EsbProcessMetaData.TYPE)).isNotNull();
      QFrontendProcessMetaData frontendProcess = new QFrontendProcessMetaData(new AbstractActionInput(), process, true);
      assertThat(JsonUtils.toJson(frontendProcess)).doesNotContain("orderEvents");
   }



   /*******************************************************************************
    ** The instance-level ESB meta-data is part of the meta-data action's output
    ** (as all supplemental instance meta-data is) - but its providers and
    ** destinations (with urls and passwords) must not be serialized.
    *******************************************************************************/
   @Test
   void testProvidersAndSecretsAbsentFromMetaDataOutput() throws Exception
   {
      QInstance qInstance = defineInstanceWithSecrets();
      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());

      MetaDataOutput metaDataOutput = new MetaDataAction().execute(new MetaDataInput());
      String         json           = JsonUtils.toJson(metaDataOutput);

      assertThat(json).doesNotContain(SECRET_PASSWORD);
      assertThat(json).doesNotContain(SECRET_MANAGEMENT_PASSWORD);
      assertThat(json).doesNotContain(getBrokerUrl());
      assertThat(json).doesNotContain("orderEvents");

      String esbJson = JsonUtils.toJson(EsbInstanceMetaData.of(qInstance));
      assertThat(esbJson).doesNotContain(SECRET_PASSWORD);
      assertThat(esbJson).doesNotContain("orderEvents");
      assertThat(JsonUtils.toJson(EsbInstanceMetaData.of(qInstance).getProvider(PROVIDER_NAME))).doesNotContain(SECRET_PASSWORD).doesNotContain(SECRET_MANAGEMENT_PASSWORD);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QInstance defineInstanceWithSecrets()
   {
      QInstance qInstance = defineInstance();
      EsbInstanceMetaData.of(qInstance).getProvider(PROVIDER_NAME)
         .withUsername("brokerUser")
         .withPassword(SECRET_PASSWORD)
         .withManagementUrl("http://localhost:8161/console/jolokia")
         .withManagementUsername("managementUser")
         .withManagementPassword(SECRET_MANAGEMENT_PASSWORD);
      EsbInstanceMetaData.of(qInstance).withDestination(new QEsbDestinationMetaData()
         .withName("orderEvents")
         .withType(EsbDestinationType.TOPIC)
         .withProviderName(PROVIDER_NAME));

      EsbTableMetaData.ofOrWithNew(qInstance.getTable(TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication().withDestinationName("orderEvents").withEvents(List.of(EsbTableEvent.INSERT)));
      EsbProcessMetaData.ofOrWithNew(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER))
         .withPublication(new EsbProcessPublication().withDestinationName("orderEvents").withEvents(List.of(EsbProcessEvent.COMPLETED)))
         .withTrigger(new EsbTrigger().withDestinationName("orderEvents"));
      return (qInstance);
   }



   /*******************************************************************************
    ** Pick an environment variable that is set (with a value), to interpolate.
    *******************************************************************************/
   private static Map.Entry<String, String> findAnEnvironmentVariable()
   {
      return (System.getenv().entrySet().stream()
         .filter(e -> e.getKey().matches("[A-Za-z_][A-Za-z0-9_]*") && StringUtils.hasContent(e.getValue()) && !e.getValue().contains("??"))
         .sorted(Map.Entry.comparingByKey())
         .findFirst()
         .orElseThrow(() -> new IllegalStateException("No environment variables are set; cannot test ${env.*} interpolation")));
   }

}
