/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import config.AppConfig
import models.request.*
import models.request.external.{ExternalBalanceRequest, ExternalLinkRequest, ExternalPaymentRequest}
import models.response.error.ErrorResponse.Response
import models.response.error.{ErrorResponse, ServiceErrorResponse}
import models.response.external.{ExternalBalanceResponse, ExternalLinkResponse, ExternalPaymentResponse}
import models.response.nsi.{NsiBalanceResponse, NsiLinkResponse, NsiPaymentResponse}
import play.api.http.Status
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import sttp.model.HeaderNames
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import utils.FormattedLogging

import java.net.{URI, URL, URLEncoder}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NsiConnector @Inject() (
    httpClient: HttpClientV2,
    appConfig: AppConfig
)(using ExecutionContext)
    extends BackendHeaderCarrierProvider
    with FormattedLogging
    with HeaderNames {
  import NsiConnector.{given, *}

  private def correlationIdHeader(using req: IdentifierRequest[?]): (String, String) =
    appConfig.nsiCorrelationIdHeader -> req.correlation_id.toString

  private def authorizationHeader: (String, String) = Authorization -> s"Basic ${appConfig.nsiAuthorisationToken}"

  def linkAccounts(using req: IdentifierRequest[ExternalLinkRequest]): Future[Response[ExternalLinkResponse]] =
    httpClient
      .get(linkAccountsUrl)
      .setHeader(correlationIdHeader)
      .setHeader(authorizationHeader)
      .withProxy
      .transform(_.withRequestTimeout(appConfig.nsiRequestTimeout))
      .execute[Response[NsiLinkResponse]]
      .map(_.map(_.toExternalLinkResponse))

  private def linkAccountsUrl(using req: IdentifierRequest[ExternalLinkRequest]): URL = {
    val queryString = Map[String, String](
      "eppURN"     -> req.body.epp_reg_reference.toString,
      "eppAccount" -> req.body.epp_unique_customer_id.toString,
      "parentNino" -> req.nino,
      "childDoB"   -> req.body.child_date_of_birth.toLocalDate.toString
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val childPaymentRef = encodeParam(req.body.outbound_child_payment_ref.toString)

    val url = s"${appConfig.nsiLinkAccountsUrl}/$childPaymentRef$queryString"

    new URI(url).toURL
  }

  def checkBalance(using req: IdentifierRequest[ExternalBalanceRequest]): Future[Response[ExternalBalanceResponse]] =
    httpClient
      .get(checkBalanceUrl)
      .setHeader(correlationIdHeader)
      .setHeader(authorizationHeader)
      .withProxy
      .transform(_.withRequestTimeout(appConfig.nsiRequestTimeout))
      .execute[Response[NsiBalanceResponse]]
      .map(_.map(_.toExternalBalanceResponse))

  private def checkBalanceUrl(using req: IdentifierRequest[ExternalBalanceRequest]): URL = {
    val queryString = Map(
      "eppURN"     -> req.body.epp_reg_reference,
      "eppAccount" -> req.body.epp_unique_customer_id,
      "parentNino" -> req.nino
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val childPaymentRef = encodeParam(req.body.outbound_child_payment_ref.toString)

    val url = s"${appConfig.nsiCheckBalanceUrl}/$childPaymentRef$queryString"

    new URI(url).toURL
  }

  def makePayment(using req: IdentifierRequest[ExternalPaymentRequest]): Future[Response[ExternalPaymentResponse]] =
    httpClient
      .post(makePaymentUrl)
      .setHeader(correlationIdHeader)
      .setHeader(authorizationHeader)
      .withBody(enrichedWithNino(req.body.toNsiPaymentRequest))
      .withProxy
      .transform(_.withRequestTimeout(appConfig.nsiRequestTimeout))
      .execute[Response[NsiPaymentResponse]]
      .map(_.map(_.toExternalPaymentResponse))

  private def makePaymentUrl: URL = new URI(appConfig.nsiMakePaymentUrl).toURL

}

object NsiConnector extends FormattedLogging with Status {

  private def enrichedWithNino[R](body: R)(using req: IdentifierRequest[?], writes: OWrites[R]): JsObject =
    Json.toJsObject(body) + ("parentNino" -> JsString(req.nino))

  private def encodeParam(outboundPaymentRef: String): String =
    URLEncoder.encode(outboundPaymentRef, "UTF-8").replaceAll("\\+", "%20")

  private given [A: Reads](using rh: RequestHeader): HttpReads[Response[A]] =
    (_, _, response) =>
      if (response.status / 100 == 2) {
        response.json.validate[A] match {
          case JsSuccess(result, _) =>
            logger.info(
              formattedInfoLog(
                s"NSI responded ${response.status}"
              )
            )
            Right(result)
          case JsError(jsonErrors) =>
            logger.warn(
              formattedErrorLog(
                s"NSI responded ${response.status}. Resulting in JSON validation errors - $jsonErrors - triggering ETFC3"
              )
            )
            Left(ServiceErrorResponse.ETFC3)
        }
      } else {
        response.json.validate[ErrorResponse] match {
          case JsSuccess(nsiErrorResponse, _) =>
            val message = formattedErrorLog(
              s"NSI responded ${response.status} with body ${response.body} - triggering $nsiErrorResponse"
            )
            if (nsiErrorResponse.reportAsStatus < INTERNAL_SERVER_ERROR) {
              logger.info(message)
            } else {
              logger.warn(message)
            }
            Left(nsiErrorResponse)
          case JsError(jsonErrors) =>
            logger.warn(
              formattedErrorLog(
                s"NSI responded ${response.status}. Resulting in JSON validation errors - $jsonErrors - triggering ETFC3"
              )
            )
            Left(ServiceErrorResponse.ETFC3)
        }
      }

}
