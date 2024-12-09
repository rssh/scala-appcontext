
import scala.scalanative.build._


ThisBuild/version := "0.1.0"
ThisBuild/versionScheme := Some("semver-spec")
ThisBuild/resolvers ++= Opts.resolver.sonatypeOssSnapshots



val sharedSettings = Seq(
    organization := "com.github.rssh",
    scalaVersion := "3.3.4",
    name := "appcontext",
    scalacOptions ++= Seq(
               "-Xcheck-macros", 
    ),
    libraryDependencies += "org.scalameta" %%% "munit" % "1.0.0" % Test
)



lazy val root = project
  .in(file("."))
  .aggregate(appcontext.js, appcontext.jvm, appcontext.native)
  .settings(
    git.remoteRepo := "git@github.com:rssh/proofspace-appcontext.git",
    publishArtifact := false,
  ).disablePlugins(MimaPlugin)


lazy val appcontext = crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .in(file("."))
    .settings(sharedSettings)
    .disablePlugins(SitePreviewPlugin)
    .jvmSettings(
        Compile / doc / scalacOptions := Seq("-groups",  
                "-source-links:shared=github://rssh/scala-appcontext/master#shared",
                "-source-links:jvm=github://rssh/scala-appcontext/master#jvm"),
        libraryDependencies += "com.github.sbt" % "junit-interface" % "0.13.3" % "test",
        mimaFailOnNoPrevious := false
    ).jsSettings(
        scalaJSUseMainModuleInitializer := true,
        Compile / doc / scalacOptions := Seq("-groups",  
                "-source-links:shared=github://rssh/scala-appcontext/master#shared",
                "-source-links:js=github://rssh/scala-appcontext/master#js"),
        mimaFailOnNoPrevious := false
    ).nativeSettings(
    )


