/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (servicesConfig: ServicesConfig) {

  private val nsiBaseUrl: String          = servicesConfig.baseUrl("nsi")
  private val nsiRootPath: String         = servicesConfig.getString("microservice.services.nsi.rootPath")
  private val nsiLinkAccountsPath: String = servicesConfig.getString("microservice.services.nsi.linkAccounts")
  private val nsiCheckBalancePath: String = servicesConfig.getString("microservice.services.nsi.checkBalance")
  private val nsiMakePaymentPath: String  = servicesConfig.getString("microservice.services.nsi.makePayment")

  val nsiLinkAccountsUrl: String = s"$nsiBaseUrl$nsiRootPath$nsiLinkAccountsPath"
  val nsiCheckBalanceUrl: String = s"$nsiBaseUrl$nsiRootPath$nsiCheckBalancePath"
  val nsiMakePaymentUrl: String  = s"$nsiBaseUrl$nsiRootPath$nsiMakePaymentPath"

  val nsiCorrelationIdHeader: String = servicesConfig.getString("microservice.services.nsi.correlationIdHeader")
  val nsiAuthorisationToken: String  = servicesConfig.getString("microservice.services.nsi.token")

}
