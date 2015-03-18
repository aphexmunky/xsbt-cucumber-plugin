import sbt._
import Keys._

object Settings {
  val buildOrganization = "templemore"
  val buildScalaVersion = "2.10.2"
  val crossBuildScalaVersions = Seq("2.10.2", "2.11.6")
  val buildVersion      = "0.8.0"

  val buildSettings = Defaults.defaultSettings ++
                      Seq (organization  := buildOrganization,
                           scalaVersion  := buildScalaVersion,
                           version       := buildVersion,
                           scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8"),
                           publishTo     := Some(Resolver.file("file",  new File("deploy-repo"))))
}

object Dependencies {

  private val CucumberVersion = "1.2.2"

  def cucumberJvm(scalaVersion: String) = "info.cukes" %% "cucumber-scala" % CucumberVersion % "compile"

  val testInterface = "org.scala-tools.testing" % "test-interface" % "0.5" % "compile"
}

object Build extends Build {
  import Dependencies._
  import Settings._

  lazy val parentProject = Project("sbt-cucumber-parent", file ("."), settings = buildSettings)

  lazy val pluginProject = Project("sbt-cucumber-plugin", file ("plugin"), settings = buildSettings ++
               Seq(crossScalaVersions := Seq.empty, sbtPlugin := true))

  lazy val integrationProject = Project ("sbt-cucumber-integration", file ("integration"),
    settings = buildSettings ++ 
               Seq(crossScalaVersions := crossBuildScalaVersions,
               libraryDependencies <+= scalaVersion { sv => cucumberJvm(sv) },
               libraryDependencies += testInterface))
}

