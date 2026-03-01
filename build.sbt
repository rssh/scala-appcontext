
import scala.scalanative.build._


ThisBuild/version := "0.4.0-SNAPSHOT"
ThisBuild/versionScheme := Some("semver-spec")
ThisBuild/resolvers ++= Opts.resolver.sonatypeOssSnapshots
ThisBuild/publishTo := localStaging.value



val sharedSettings = Seq(
    organization := "com.github.rssh",
    //scalaVersion := "3.6.4-RC1-bin-SNAPSHOT",
    scalaVersion := "3.3.7",
    scalacOptions ++= Seq(
               "-Xcheck-macros", 
               "-Wvalue-discard", 
               "-Wnonunit-statement", 
    ),
    libraryDependencies += "org.scalameta" %%% "munit" % "1.2.3" % Test
)



lazy val root = project
  .in(file("."))
  .aggregate(appcontext.js, appcontext.jvm, appcontext.native)
  .settings(
    git.remoteRepo := "git@github.com:rssh/proofspace-appcontext.git",
    publishArtifact := false,
  ).disablePlugins(MimaPlugin)


lazy val appcontext = crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .in(file("core"))
    .settings(sharedSettings)
    .settings(
       name := "appcontext",
       libraryDependencies += "io.github.dotty-cps-async" %%% "dotty-cps-async" % "1.3.0" % "optional",
    )
    .disablePlugins(SitePreviewPlugin)
    .jvmSettings(
        Compile / doc / scalacOptions := Seq("-groups",
                "-source-links:shared=github://rssh/scala-appcontext/master#shared",
                "-source-links:jvm=github://rssh/scala-appcontext/master#jvm"),
        libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.3" % "test",
        libraryDependencies += "org.typelevel" %% "cats-effect" % "3.6.3" % "test",
        libraryDependencies += "io.github.dotty-cps-async" %% "cps-async-connect-cats-effect" % "1.3.0" % "test",
        mimaFailOnNoPrevious := false,
    ).jsSettings(
        scalaJSUseMainModuleInitializer := true,
        Compile / doc / scalacOptions := Seq("-groups",  
                "-source-links:shared=github://rssh/scala-appcontext/master#shared",
                "-source-links:js=github://rssh/scala-appcontext/master#js"),
        mimaFailOnNoPrevious := false
    ).nativeSettings(
    )


