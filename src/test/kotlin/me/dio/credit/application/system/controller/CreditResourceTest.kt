package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var creditRepository: CreditRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        const val URL: String = "/api/credits"
    }

    @BeforeEach
    fun setup() {
        customerRepository.deleteAll()
        creditRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        customerRepository.deleteAll()
        creditRepository.deleteAll()
    }

    @Test
    fun `should create a credit and return 201 status`() {
        //given
        val customer: Customer = builderCustomer(id = 1L)
        val savedCustumer: Customer = customerRepository.save(customer)
        val creditDto: CreditDto = builderCreditDto(creditValue = BigDecimal.valueOf(1000), customerId = savedCustumer?.id!!)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)
        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isMap)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value("1000"))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.dayFirstInstallment").value(LocalDate.now().plusDays(90).toString())
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallments").value(5))
            .andExpect(MockMvcResultMatchers.jsonPath("$.customerId").value(savedCustumer.id))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find all credits by customerId and return 200 status`() {
        //given
        val customer: Customer = builderCustomer(id = 1L)
        val savedCustomer: Customer = customerRepository.save(customer)

        val creditDto: CreditDto = builderCreditDto(creditValue = BigDecimal.valueOf(1000), customerId = savedCustomer?.id!!)
        creditRepository.save(creditDto.toEntity())

        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        mockMvc.perform(
            MockMvcRequestBuilders.get(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString).param("customerId", "${savedCustomer.id}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.[0].creditValue").value("1000.0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.[0].numberOfInstallments").value("5"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find all credits by creditCode and return 200 status`() {
        //given
        val customer: Customer = builderCustomer(id = 1L)
        customerRepository.save(customer)

        val creditDto: CreditDto = builderCreditDto(
            creditValue = BigDecimal.valueOf(1000),
            customerId = customer?.id!!
        )
        creditRepository.save(creditDto.toEntity())

        val creditCod = creditRepository.findById(1L).get().creditCode

        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        mockMvc.perform(
            MockMvcRequestBuilders.get("$URL/$creditCod").param("customerId", "1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").value("$creditCod"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value("1000.0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value("5"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value("1000.0"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not save a credit with unkown customer and return 409 status`() {
        //given
        val creditDto: CreditDto = builderCreditDto(creditValue = BigDecimal.valueOf(1000), customerId = 2L)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)
        //when
        //then
        mockMvc.perform(
            MockMvcRequestBuilders.post(CustomerResourceTest.URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andDo(MockMvcResultHandlers.print())
    }

    private fun builderCustomer(
        firstName: String = "Cami",
        lastName: String = "Cavalcante",
        cpf: String = "28475934625",
        email: String = "camila@email.com",
        income: BigDecimal = BigDecimal.valueOf(1000.0),
        password: String = "1234",
        zipCode: String = "000000",
        street: String = "Rua da Cami, 123",
        id: Long
    ) = Customer(
        firstName = firstName,
        lastName = lastName,
        cpf = cpf,
        email = email,
        income = income,
        password = password,
        address = Address(
            zipCode = zipCode,
            street = street
        ),
        id = id
    )

    private fun builderCreditDto(
        creditValue: BigDecimal,
        dayFirstOfInstallment: LocalDate = LocalDate.now().plusDays(90),
        numberOfInstallments: Int = 5,
        customerId: Long
    ) = CreditDto(
        creditValue = creditValue,
        dayFirstOfInstallment = dayFirstOfInstallment,
        numberOfInstallments = numberOfInstallments,
        customerId = customerId,
    )
}