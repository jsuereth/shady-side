import com.typesafe.sbt.license.{DepModuleInfo}

val dottyVersion = "0.19.0-RC1"
val lwjglVersion = "3.2.2"
val lwjglNatives: String = 
    sys.props("os.name") match {
        case null => "natives-linux"
        case x if x.toLowerCase contains "windows" => "natives-windows"
        case x if x.toLowerCase contains "mac" => "natives-osx"
        case _ => "natives-linux"
    }

def lwjgl(name: String) = "org.lwjgl" % name % lwjglVersion
def lwjglNative(name: String) = lwjgl(name) classifier lwjglNatives
def junit = "com.novocode" % "junit-interface" % "0.11" % "test"
def findBugs = "com.google.code.findbugs" % "jsr305" % "3.0.2"

def commonSettings: Seq[Setting[_]] = Seq(
    organizationName := "Google LLC",
    startYear := Some(2019),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    organization := "com.jsuereth.shadyside",
    git.baseVersion := "0.1",
    scalaVersion := dottyVersion,
    licenseReportTitle := "third_party_licenses",
    licenseReportDir := baseDirectory.value / "third_party",
    licenseReportTypes := Seq(MarkDown),
    licenseReportNotes := {
      case DepModuleInfo(group, id, version) if group contains "org.lwjgl" => "Lightweight Java Game Library"
      case DepModuleInfo(group, id, version) if id contains "junit" => "Used for testing"
      case DepModuleInfo(group, id, version) if id contains "jsr305" => "Required to compile, transitive dep."
    },
    libraryDependencies += junit
)

// Our linear algebra library.  Should NOT depend on OpenGL in any way.
// TODO - sbt-jmh showing off how bad we are at fast math.
val math = project.settings(commonSettings:_*).enablePlugins(GitVersioning)
// Our library for passing data into/out of OpenGL.
// This is meant to abstract over Plain-old-data classes and make it seamless
// to shove mat4's, vec3's, VAOs and Samplers (texture buffers) around.
val io = project.dependsOn(math).settings(commonSettings:_*).settings(
    libraryDependencies += lwjgl("lwjgl"),
    libraryDependencies += lwjgl("lwjgl-opengl")
).enablePlugins(GitVersioning)

// The shader DSL.
val shader = project.dependsOn(math, io).settings(commonSettings:_*).settings(
    libraryDependencies += lwjgl("lwjgl"),
    libraryDependencies += lwjgl("lwjgl-opengl")
).enablePlugins(GitVersioning)

// A scene-graph library built on all the other components.   Used mostly
// so we can demonstrate each bit.
val scene = project.dependsOn(math, io).settings(commonSettings:_*).settings(
    libraryDependencies += lwjgl("lwjgl")
).enablePlugins(GitVersioning)

// An example project that renders a scene with a cartoon shader.
val example = project.dependsOn(shader, scene).settings(commonSettings:_*).settings(
    libraryDependencies += lwjgl("lwjgl"),
    libraryDependencies += lwjgl("lwjgl-glfw"),
    libraryDependencies += lwjgl("lwjgl-opengl"),
    libraryDependencies += lwjglNative("lwjgl-glfw"),
    libraryDependencies += lwjglNative("lwjgl"),
    libraryDependencies += lwjglNative("lwjgl-opengl"),
    // Yay for this silently failing unpickling.
    libraryDependencies += findBugs % "provided"
).enablePlugins(GitVersioning)