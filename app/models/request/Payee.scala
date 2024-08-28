/*
 * Copyright 2024 HM Revenue & Customs
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

package models.request

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

sealed abstract class Payee

object Payee extends ConstraintReads {
  case object ExternalPaymentProvider                               extends Payee
  final case class ChildCareProvider(urn: String, postcode: String) extends Payee

  object ChildCareProvider {

    val reads: Reads[Payee] = (
      (__ \ CCP_URN_KEY).read(CCP_REG) ~
        (__ \ CCP_POSTCODE_KEY).read(POST_CODE)
    )(apply _)
  }

  val readsPayeeFromApi: Reads[Payee] =
    (__ \ PAYEE_TYPE_KEY)
      .read[String]
      .flatMap {
        case "EPP" => Reads pure ExternalPaymentProvider
        case "CCP" => ChildCareProvider.reads
        case _     => readsPayeeFailed
      }

  val readsCcpFromApi: Reads[Payee] =
    (__ \ PAYEE_TYPE_KEY)
      .read[String]
      .flatMap {
        case "CCP" => ChildCareProvider.reads
        case _     => readsPayeeFailed
      }

  lazy private val readsPayeeFailed = Reads[Payee] { _ =>
    JsError(JsPath(List(KeyPathNode(PAYEE_TYPE_KEY))), "error.payee_type")
  }

  lazy private val POST_CODE = pattern("\\s*[a-zA-Z0-9]{2,4}\\s*\\d[a-zA-Z]{2}\\s*$".r)
  lazy private val CCP_REG   = pattern(s".{1,$CCP_REG_MAX_LEN}".r)
  lazy val CCP_REG_MAX_LEN   = 20

  lazy val PAYEE_TYPE_KEY   = "payee_type"
  lazy val CCP_URN_KEY      = "ccp_reg_reference"
  lazy val CCP_POSTCODE_KEY = "ccp_postcode"
}
