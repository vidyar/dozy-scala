import sbt._
import sbt.Keys._

object Settings {
    lazy val coreSettings = Seq(
        organization := "uk.co.grahamcox.dozy",
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.10.2"
    )

    lazy val defaultSettings = Defaults.defaultSettings ++ 
        coreSettings ++ Seq(
            scalacOptions in Compile ++= Seq(
                "-encoding", "UTF-8",
                "-target:jvm-1.7",
                "-deprecation",
                "-feature",
                "-unchecked"
            )
        ) ++
        net.virtualvoid.sbt.graph.Plugin.graphSettings
}

object Dependencies {
    val jettyVersion = "9.0.5.v20130815"
    val slf4jVersion = "1.7.1"
    val logbackVersion = "1.0.1"

    val Resolvers = Seq (
        "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    )

    val ScalaTest = Seq(
        "org.scalatest" %% "scalatest" % "1.9.1" % "test"
    )

    val EasyMock = Seq(
        "org.easymock" % "easymock" % "3.2" % "test"
    )

    val Servlet = Seq(
        "javax.servlet" % "servlet-api" % "2.5" % "provided"
    )

    val Jetty = Seq(
        "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "container",
        "org.eclipse.jetty" % "jetty-plus" % jettyVersion % "container"
    )

    val Logging = Seq(
        "org.clapper" %% "grizzled-slf4j" % "1.0.1"
    )

    def LoggingImpl(scope: String = "runtime") = Seq(
        "org.slf4j" % "jcl-over-slf4j" % slf4jVersion % scope,
        "org.slf4j" % "jul-to-slf4j" % slf4jVersion % scope,
        "ch.qos.logback" % "logback-classic" % logbackVersion % scope,
        "ch.qos.logback" % "logback-core" % logbackVersion % scope
    )

    val Core = Logging ++ ScalaTest ++ EasyMock
}

object DozyBuild extends Build {
    lazy val root = Project(
        id = "dozy",
        base = file("."),
        aggregate = Seq(core, example),
        settings = Settings.defaultSettings
    )

    lazy val core = Project(
        id = "dozy-core",
        base = file("core"),
        settings = Settings.defaultSettings ++ Seq(
            resolvers ++= Dependencies.Resolvers,
            libraryDependencies ++= Dependencies.Core ++ 
                Dependencies.LoggingImpl("test") ++
                Dependencies.Servlet
        )
    )

    lazy val example = Project(
        id = "dozy-example",
        base = file("example"),
        dependencies = Seq(
            core % "compile"
        ),
        settings = Settings.defaultSettings ++
            com.earldouglas.xsbtwebplugin.WebPlugin.webSettings ++ Seq(
                resolvers ++= Dependencies.Resolvers,
                libraryDependencies ++= Dependencies.Core ++ 
                    Dependencies.LoggingImpl() ++ 
                    Dependencies.Jetty
        )
    )
}
