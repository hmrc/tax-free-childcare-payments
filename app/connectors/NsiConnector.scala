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

import com.fasterxml.jackson.core.JsonParseException
import config.AppConfig
import models.request.*
import models.response.NsiErrorResponse.{ETFC3, NsiResponse}
import models.response.*
import play.api.http.Status
import play.api.libs.json.*
import play.api.mvc.RequestHeader
import sttp.model.HeaderNames
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import utils.FormattedLogging
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import java.net.{URI, URL, URLEncoder}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class NsiConnector @Inject() (
    httpClient: HttpClientV2,
    appConfig: AppConfig
)(using ExecutionContext)
    extends BackendHeaderCarrierProvider
    with FormattedLogging
    with HeaderNames {
  import NsiConnector.{given, *}

  def linkAccounts(using req: IdentifierRequest[LinkRequest]): Future[NsiResponse[LinkResponse]] =
    httpClient
      .get(linkAccountsUrl)
      .setHeader(appConfig.nsiCorrelationIdHeader -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic ${appConfig.nsiAuthorisationToken}")
      .withProxy
      .transform(_.withRequestTimeout(appConfig.nsiRequestTimeout))
      .execute[NsiResponse[LinkResponse]]

  private def linkAccountsUrl(using req: IdentifierRequest[LinkRequest]): URL = {
    val queryString = Map(
      "eppURN"     -> req.body.sharedRequestData.epp_reg_reference,
      "eppAccount" -> req.body.sharedRequestData.epp_unique_customer_id,
      "parentNino" -> req.nino,
      "childDoB"   -> req.body.child_date_of_birth
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val childPaymentRef = encodeParam(req.body.sharedRequestData.outbound_child_payment_ref)

    val url = s"${appConfig.nsiLinkAccountsUrl}/$childPaymentRef$queryString"

    new URI(url).toURL
  }

  def checkBalance(using req: IdentifierRequest[SharedRequestData]): Future[NsiResponse[BalanceResponse]] =
    httpClient
      .get(checkBalanceUrl)
      .setHeader(appConfig.nsiCorrelationIdHeader -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic ${appConfig.nsiAuthorisationToken}")
      .withProxy
      .transform(_.withRequestTimeout(appConfig.nsiRequestTimeout))
      .execute[NsiResponse[BalanceResponse]]

  private def checkBalanceUrl(using req: IdentifierRequest[SharedRequestData]): URL = {
    val queryString = Map(
      "eppURN"     -> req.body.epp_reg_reference,
      "eppAccount" -> req.body.epp_unique_customer_id,
      "parentNino" -> req.nino
    ).map { case (k, v) => s"$k=$v" }.mkString("?", "&", "")

    val childPaymentRef = encodeParam(req.body.outbound_child_payment_ref)

    val url = s"${appConfig.nsiCheckBalanceUrl}/$childPaymentRef$queryString"

    new URI(url).toURL
  }

  def makePayment(using req: IdentifierRequest[PaymentRequest]): Future[NsiResponse[PaymentResponse]] =
    httpClient
      .post(new URI(appConfig.nsiMakePaymentUrl).toURL)
      .setHeader(appConfig.nsiCorrelationIdHeader -> req.correlation_id.toString)
      .setHeader(Authorization -> s"Basic ${appConfig.nsiAuthorisationToken}")
      .withBody(enrichedWithNino[PaymentRequest])
      .withProxy
      .transform(_.withRequestTimeout(appConfig.nsiRequestTimeout))
      .execute[NsiResponse[PaymentResponse]]

}

object NsiConnector extends FormattedLogging with Status {

  private def enrichedWithNino[R: OWrites](using req: IdentifierRequest[R]): JsObject =
    Json.toJsObject(req.body) + ("parentNino" -> JsString(req.nino))

  private def encodeParam(outboundPaymentRef: String): String =
    URLEncoder.encode(outboundPaymentRef, "UTF-8").replaceAll("\\+", "%20")

  private given httpReadsNsiResponse[A, B: Reads](
      using rh: RequestHeader,
      req: IdentifierRequest[A]
  ): HttpReads[NsiResponse[B]] =
    (_, _, response) =>
      if (response.status / 100 == 2) {
        nsiResponse(response)
      } else {
        val jsonValidatedResponse: Try[JsResult[NsiErrorResponse]] = Try(response.json.validate[NsiErrorResponse])
        jsonValidatedResponse match {
          case Success(JsSuccess(nsiErrorResponse, _)) =>
            errorResponseNsi(response.status, response.body, nsiErrorResponse)
          case Success(jsonErrors: Seq[(JsPath, Seq[JsonValidationError])]) =>
            errorResponseJson(response.status, jsonErrors)
          case Failure(exception) =>
            exceptionResponse(exception, response, req)
        }
      }

  private def nsiResponse[A: Reads](response: HttpResponse)(using rh: RequestHeader) =
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
        Left(ETFC3)
    }

  private def errorResponseNsi(status: Int, body: String, response: NsiErrorResponse)(using rh: RequestHeader) = {
    val message = formattedErrorLog(
      s"NSI responded $status with body $body - triggering $response"
    )
    if (response.reportAs < INTERNAL_SERVER_ERROR) {
      logger.info(message)
    } else {
      logger.warn(message)
    }
    Left(response)
  }

  private def errorResponseJson(
      status: Int,
      errors: Seq[(JsPath, Seq[JsonValidationError])]
  )(using rh: RequestHeader) = {
    logger.warn(
      formattedErrorLog(
        s"NSI responded $status. Resulting in JSON validation errors - $errors - triggering ETFC3"
      )
    )
    Left(ETFC3)
  }

  private def exceptionResponse(exception: Throwable, response: HttpResponse, req: IdentifierRequest[?])(
      using rh: RequestHeader
  ) =
    exception match {
      case ex: JsonParseException =>
        logger.warn(
          formattedErrorLog(
            s"NSI responded with a JsonParseException for correlation ID - ${req.correlation_id} - triggering ETFC3"
          )
        )
        Left(ETFC3)
    }

}
