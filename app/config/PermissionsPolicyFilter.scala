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

package config

import scala.concurrent.ExecutionContext

import com.google.inject.{Inject, Singleton}
import utils.HeaderNames

import play.api.Configuration
import play.api.mvc.{EssentialAction, EssentialFilter}

@Singleton
class PermissionsPolicyFilter @Inject() (conf: Configuration)(implicit ec: ExecutionContext)
    extends EssentialFilter with HeaderNames {

  def apply(next: EssentialAction): EssentialAction = EssentialAction { request =>
    next(request).map(
      _.withHeaders(PERMISSIONS_POLICY -> permissionsPolicy)
    )
  }

  private val permissionsPolicy = conf
    .get[Map[String, String]]("config.permissionsPolicy")
    .map { case (feature, allowlist) => s"$feature=$allowlist" }
    .mkString(", ")
}
