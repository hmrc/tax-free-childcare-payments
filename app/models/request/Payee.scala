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

import models.request.Payee.ChildCareProvider.{PostCode, Urn}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

sealed abstract class Payee

object Payee extends ConstraintReads {

  case object ExternalPaymentProvider extends Payee {

    val reads: Reads[ExternalPaymentProvider.type] = Reads.pure(ExternalPaymentProvider)

  }

  final case class ChildCareProvider(urn: Urn, postcode: PostCode) extends Payee

  object ChildCareProvider {

    val CCP_POSTCODE_KEY = "ccp_postcode"
    val CCP_URN_KEY      = "ccp_reg_reference"

    val reads: Reads[ChildCareProvider] =
      (__ \ CCP_URN_KEY)
        .read[Urn]
        .and((__ \ CCP_POSTCODE_KEY).read[PostCode])(ChildCareProvider.apply _)

    case class PostCode(value: String) extends AnyVal

    object PostCode {

      implicit val reads: Reads[PostCode]   = pattern("\\s*[a-zA-Z0-9]{2,4}\\s*\\d[a-zA-Z]{2}\\s*$".r).map(PostCode(_))
      implicit val writes: Writes[PostCode] = postCode => JsString(postCode.value)

    }

    case class Urn(value: String) extends AnyVal

    object Urn {

      val CCP_REG_MAX_LEN = 20

      implicit val reads: Reads[Urn]   = pattern(s".{1,$CCP_REG_MAX_LEN}".r).map(apply)
      implicit val writes: Writes[Urn] = reference => JsString(reference.value)
    }

  }

  val PAYEE_TYPE_KEY = "payee_type"

  val readsPayeeFromUser: Reads[Payee] =
    (__ \ PAYEE_TYPE_KEY)
      .read[String]
      .flatMap {
        case "EPP" => ExternalPaymentProvider.reads.widen
        case "CCP" => ChildCareProvider.reads.widen
        case _     => readsPayeeFailed
      }

  val readsCcpFromUser: Reads[Payee] =
    (__ \ PAYEE_TYPE_KEY)
      .read[String]
      .flatMap {
        case "CCP" => ChildCareProvider.reads.widen
        case _     => readsPayeeFailed
      }

  implicit val writesToNsi: OWrites[Payee] = {
    case ExternalPaymentProvider => Json.obj("payeeType" -> "EPP")
    case ChildCareProvider(urn, postcode) =>
      Json.obj(
        "payeeType"   -> "CCP",
        "ccpURN"      -> urn,
        "ccpPostcode" -> postcode
      )
  }

  private val readsPayeeFailed: Reads[Payee] =
    Reads[Payee](_ => JsError(JsPath(List(KeyPathNode(PAYEE_TYPE_KEY))), "error.payee_type"))

}
