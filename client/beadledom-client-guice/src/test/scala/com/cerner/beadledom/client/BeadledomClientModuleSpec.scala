package com.cerner.beadledom.client

import com.google.inject._
import org.scalatest.{BeforeAndAfter, FunSpec, MustMatchers}

/**
  * @author John Leacox
  */
class BeadledomClientModuleSpec extends FunSpec with MustMatchers with BeforeAndAfter {

  def getTestBindingInjector(modules: List[Module]): Injector = {
    val module = new AbstractModule {
      override def configure(): Unit = {
        install(BeadledomClientModule.`with`(classOf[TestBindingAnnotation]))
        modules.foreach(m => install(m))
      }
    }
    Guice.createInjector(module)
  }

  describe("BeadledomClientModule") {
    describe("#with") {
      it("throws an IllegalArgumentException for non-binding annotations") {
        intercept[IllegalArgumentException] {
          BeadledomClientModule.`with`(classOf[Override])
        }
      }

      it("returns a new BeadledomClientModule for a BindingAnnotation") {
        val module = BeadledomClientModule.`with`(classOf[TestBindingAnnotation])
        module must not be null
      }

      it("returns a new BeadledomClientModule for a Qualifier") {
        val module = BeadledomClientModule.`with`(classOf[TestQualifier])
        module must not be null
      }
    }

    it("binds a lifecycle hook that closes the client") {
      val injector = getTestBindingInjector(List())
      val client = injector
          .getInstance(
            Key.get(classOf[BeadledomClient], classOf[TestBindingAnnotation]))
      val lifecycleHook = injector.getInstance(
        Key.get(classOf[BeadledomClientLifecycleHook], classOf[TestBindingAnnotation]))

      client.isClosed mustBe false
      lifecycleHook.preDestroy()
      client.isClosed mustBe true
    }

    it("provides a BeadledomClient for the specified annotation") {
      val injector = getTestBindingInjector(List())
      val client = injector
          .getInstance(
            Key.get(classOf[BeadledomClient], classOf[TestBindingAnnotation]))

      client must not be null
    }

    it("provides a BeadledomClient for the specified annotation with BeadledomClientConfiguration")
    {
      val correlationIdName = "bd-correlation-id"

      val testModule = new AbstractModule {
        override def configure(): Unit = {
          val config: BeadledomClientConfiguration = BeadledomClientConfiguration
              .builder()
              .connectionPoolSize(1)
              .connectionTimeoutMillis(2)
              .correlationIdName(correlationIdName)
              .build()

          bind(classOf[BeadledomClientConfiguration])
              .annotatedWith(classOf[TestBindingAnnotation])
              .toInstance(config)
        }
      }

      val injector = getTestBindingInjector(List(testModule))

      val clientBuilder = injector
          .getInstance(Key.get(classOf[BeadledomClientBuilder], classOf[TestBindingAnnotation]))

      val config = clientBuilder.getBeadledomClientConfiguration

      config.connectionPoolSize mustBe 1
      config.connectionTimeoutMillis mustBe 2 * 1000
      config.correlationIdName mustBe correlationIdName

      classOf[BeadledomClient].isAssignableFrom(clientBuilder.build.getClass) mustBe true
    }

    it("fallsback to the default correlation Id if it is not set in beadledomClientConfiguration") {
      val testModule = new AbstractModule {
        override def configure(): Unit = {
          val config: BeadledomClientConfiguration = BeadledomClientConfiguration
              .builder()
              .build()

          bind(classOf[BeadledomClientConfiguration])
              .annotatedWith(classOf[TestBindingAnnotation])
              .toInstance(config)
        }
      }

      val injector = getTestBindingInjector(List(testModule))

      val clientBuilder = injector
          .getInstance(Key.get(classOf[BeadledomClientBuilder], classOf[TestBindingAnnotation]))

      val config = clientBuilder.getBeadledomClientConfiguration

      config.correlationIdName mustBe CorrelationIdContext.DEFAULT_HEADER_NAME
    }
  }
}
