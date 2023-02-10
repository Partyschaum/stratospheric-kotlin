package de.shinythings.todoapp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(IndexController::class)
class IndexControllerTest(
    @Autowired
    private val mockMvc: MockMvc
) {

    @Test
    fun shouldReturnIndexPage() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("index"))
    }
}
