import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.7.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"  %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.beachape" %% "enumeratum"                % "1.7.4"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-30" % bootstrapVersion,
    "com.vladsch.flexmark" % "flexmark-all"           % "0.64.8",
    "org.scalatestplus"   %% "scalacheck-1-17"        % "3.2.18.0"
  ).map(_ % "test")

  val it: Seq[ModuleID] = Seq.empty
}
