package com.shinythings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awscdk.App

@DisplayName("Testing the utilities")
class UtilTests {

  @Nested
  @DisplayName("Node Context Extension")
  inner class NodeContextExtension {

    @Test
    fun `it returns the value as string`() {
      val app = App()
      app.node.setContext("someTextKey", "some text")
      assertEquals("some text", app.node["someTextKey"])
    }

    @Test
    fun `it throws NodeContextException if the value is no string`() {
      val app = App()
      app.node.setContext("someNonTextKey", 23)
      val exception = assertThrowsExactly(IllegalArgumentException::class.java) {
        app.node["someNonTextKey"]
      }
      assertEquals("The key 'someNonTextKey' is no string.", exception.message)
    }

    @Test
    fun `it throws IllegalArgumentException if the value is not set`() {
      val app = App()
      val exception = assertThrowsExactly(IllegalArgumentException::class.java) {
        app.node["nonExistentKey"]
      }
      assertEquals("The key 'nonExistentKey' does not exist.", exception.message)
    }
  }

  @DisplayName("makeEnv")
  @Nested
  inner class MakeEnvHelper {

    @Test
    fun `it configures the environment`() {
      val env = makeEnv("someAccount", "someRegion")
      assertEquals("someAccount", env.account)
      assertEquals("someRegion", env.region)
    }
  }
}
