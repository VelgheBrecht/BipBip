name := "chisel_project"
version := "1.0"
scalaVersion := "2.12.13"

// Chisel dependencies
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5.0"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % "test" // added ScalaTest dependency
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"
libraryDependencies += "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.4"

// JNA dependency for C/C++ interop
libraryDependencies += "net.java.dev.jna" % "jna" % "5.8.0"

addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.0" cross CrossVersion.full)

scalacOptions ++= Seq(
  "-Xsource:2.13",
)

fork in run := true
javaOptions in run += s"-Djava.library.path=${baseDirectory.value}/lib"
