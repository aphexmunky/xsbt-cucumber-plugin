package templemore.sbt.cucumber

import scala.collection.JavaConverters._
import java.lang.reflect.InvocationTargetException
import java.util.Properties

import cucumber.runtime.Runtime
import cucumber.runtime.Env
import cucumber.runtime.RuntimeOptions
import cucumber.runtime.model.CucumberFeature
import gherkin.formatter.Formatter
import gherkin.formatter.Reporter

import collection.JavaConversions._

class CucumberLauncher(debug: (String) => Unit, error: (String) => Unit) {
  
  def apply(cucumberArguments: Array[String],
            testClassLoader: ClassLoader): Int = {
    debug("Cucumber arguments: " + cucumberArguments.mkString(" "))
    val runtime = buildRuntime(System.getProperties, cucumberArguments, testClassLoader)
    runCucumber(runtime).asInstanceOf[Byte].intValue
  }

  private def runCucumber(runtime: CucumberRuntime) = try { 
    runtime.run
    runtime.exitStatus
  } catch {
    case e: InvocationTargetException => {
      val cause = if ( e.getCause == null ) e else e.getCause
      error("Error running cucumber. Cause: " + cause.getMessage)
      throw cause
    }
  }

  case class CucumberRuntime(runtime: Runtime, options: RuntimeOptions, loader: AnyRef, 
                             formatter: Formatter, reporter: Reporter) {
    private val loaderClass = loader.getClass.getInterfaces()(0)

    def exitStatus = runtime.exitStatus

    def run = {
      val featureList = (classOf[RuntimeOptions].getMethod("cucumberFeatures", loaderClass)
                                                .invoke(options, loader)
                                                .asInstanceOf[java.util.List[CucumberFeature]]
                                                .asScala)
      featureList foreach { feature => feature.run(formatter, reporter, runtime) }
    }
  }

  private def buildRuntime(properties: Properties, 
                           arguments: Array[String], 
                           classLoader: ClassLoader): CucumberRuntime = try {
    def buildLoader(clazz: Class[_]) = 
      clazz.getConstructor(classOf[ClassLoader]).newInstance(classLoader).asInstanceOf[AnyRef]
  
    val loaderClass = loadCucumberClasses(classLoader)

    val options = new RuntimeOptions(new Env, arguments.toList: java.util.List[String])
    val loader = buildLoader(loaderClass)

    val runtimeConstructor = classOf[Runtime].getConstructor(loaderClass.getInterfaces()(0), classOf[ClassLoader], classOf[RuntimeOptions])
    val runtime = runtimeConstructor.newInstance(loader, classLoader, options).asInstanceOf[Runtime]

    CucumberRuntime(runtime, options, loader, 
                    options.formatter(classLoader), options.reporter(classLoader))
  } catch {
    case e: Exception => 
      error("Unable to construct cucumber runtime. Please report this as an error. (Details: " + e.getMessage + ")")
      throw e
  }

  private def loadCucumberClasses(classLoader: ClassLoader) = try {
    classLoader.loadClass("cucumber.runtime.io.MultiLoader")
  } catch {
    case e: ClassNotFoundException =>
      error("Unable to load Cucumber classes. Please check your project dependencies. (Details: " + e.getMessage + ")")
      throw e
  }
}