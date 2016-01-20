import sbt.Keys._

name := "scala-alltop-to-rss"

version := "1.0"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers in ThisBuild += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= deps

scalaVersion := "2.11.7"

lazy val deps = {
  val akkaV = "2.4.1"
  val sparkV = "1.6.0"
  val akkaStreamV = "1.0"
  Seq(
   "org.slf4j"               % "slf4j-nop"                           % "1.7.13",
    "org.jsoup"              %  "jsoup"                              % "1.8.3",
    "com.typesafe.play"      %% "play-ws"                            % "2.4.4",
    "com.typesafe.play"      %% "play-json"                          % "2.4.4",
    "com.gravity"            %% "goose"                              % "2.1.25-SNAPSHOT",
    "com.lihaoyi"            %% "upickle"                            % "0.3.6",
    "com.typesafe.akka"      %% "akka-stream-experimental"           % akkaStreamV,
    "com.typesafe.akka"      %% "akka-http-core-experimental"        % akkaStreamV,
    "org.apache.spark"       %% "spark-core"                         % sparkV,
    "org.apache.spark"       %% "spark-sql"                          % sparkV
  )
}

