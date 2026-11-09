package com.prog2.frontend.network

import com.prog2.frontend.catalog.CatalogError
import com.prog2.frontend.catalog.CatalogException
import com.prog2.frontend.catalog.data.CatalogCategory
import com.prog2.frontend.catalog.data.CatalogState
import com.prog2.frontend.catalog.data.KtorCatalogApi
import com.prog2.frontend.catalog.data.Professional
import com.prog2.frontend.catalog.data.ProfessionalQuery
import com.prog2.frontend.session.JwtExpiry
import com.prog2.frontend.session.MemorySessionStore
import com.prog2.frontend.session.SessionError
import com.prog2.frontend.session.SessionException
import com.prog2.frontend.session.SessionManager
import com.prog2.frontend.session.SessionStatus
import com.prog2.frontend.session.TestAuthApi
import com.prog2.frontend.session.TestClock
import com.prog2.frontend.session.testToken
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorCatalogApiTest {
    private val configuration = ApiConfiguration("https://turnos.test", "https://catalog.test")

    private fun manager() = SessionManager(TestAuthApi(), MemorySessionStore(), JwtExpiry(), TestClock())

    private val pageJson = """
        {"content":[
          {"id":15,"firstName":"Ana","lastName":"Gómez","enabled":true,
           "category":{"id":1,"name":"Clínica","enabled":true}}
        ],"page":0,"size":20,"totalElements":1,"totalPages":1,
        "catalog":{"state":"STALE","copyAvailable":true}}
    """.trimIndent()

    @Test
    fun sendsTheFourFiltersWithTheUserBearerAndParsesThePage() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { request ->
            assertEquals("catalog.test", request.url.host)
            assertEquals("/api/catalog/professionals", request.url.encodedPath)
            assertEquals("Bearer ${testToken()}", request.headers[HttpHeaders.Authorization])
            assertEquals("1", request.url.parameters["categoryId"])
            assertEquals("ana gómez", request.url.parameters["name"])
            assertEquals("false", request.url.parameters["enabled"])
            assertEquals("2026-11-09", request.url.parameters["availableOn"])
            assertEquals("2", request.url.parameters["page"])
            assertEquals("20", request.url.parameters["size"])
            respond(pageJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val page = KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager)).searchProfessionals(
                ProfessionalQuery(
                    categoryId = 1,
                    name = "ana gómez",
                    enabled = false,
                    availableOn = "2026-11-09",
                    page = 2,
                ),
            )
            assertEquals(
                listOf(Professional(15, "Ana", "Gómez", true, CatalogCategory(1, "Clínica", true))),
                page.content,
            )
            assertEquals(1, page.totalElements)
            assertEquals(CatalogState("STALE", true), page.catalog)
        } finally {
            client.close()
        }
    }

    @Test
    fun omitsUnsetFiltersAndSendsTheDefaultPagination() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { request ->
            assertNull(request.url.parameters["categoryId"])
            assertNull(request.url.parameters["name"])
            assertNull(request.url.parameters["enabled"])
            assertNull(request.url.parameters["availableOn"])
            assertEquals("0", request.url.parameters["page"])
            assertEquals("20", request.url.parameters["size"])
            respond(pageJson)
        })
        try {
            KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager)).searchProfessionals(ProfessionalQuery())
        } finally {
            client.close()
        }
    }

    @Test
    fun anEmptyPageWithTheRequiredFieldsIsAValidResult() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine {
            respond(
                """{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0,""" +
                    """"catalog":{"state":"CURRENT","copyAvailable":true}}""",
            )
        })
        try {
            val page = KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager))
                .searchProfessionals(ProfessionalQuery())
            assertTrue(page.content.isEmpty())
            assertEquals(0, page.totalElements)
            assertEquals(0, page.totalPages)
            assertEquals(CatalogState("CURRENT", true), page.catalog)
        } finally {
            client.close()
        }
    }

    @Test
    fun anIncompletePageIsAnErrorInsteadOfAnEmptyResult() = runTest {
        val cases = listOf(
            """{"content":[]}""",
            """{"page":0,"size":20,"totalElements":0,"totalPages":0,""" +
                """"catalog":{"state":"CURRENT","copyAvailable":true}}""",
            """{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}""",
        )
        for (body in cases) {
            val manager = manager()
            manager.signIn("patient", "secret", false)
            var requests = 0
            val client = ApiHttpClient().create(MockEngine {
                requests++
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            })
            try {
                val failure = assertFailsWith<CatalogException> {
                    KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager))
                        .searchProfessionals(ProfessionalQuery())
                }
                assertEquals(CatalogError.UNAVAILABLE, failure.error)
                assertEquals(1, requests)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun anEmptyCategoryListIsValidButAMissingFieldIsNot() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        var body = """{"content":[]}"""
        val client = ApiHttpClient().create(MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val api = KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager))
            assertTrue(api.categories().isEmpty())

            body = """{"catalog":{"state":"CURRENT","copyAvailable":true}}"""
            val failure = assertFailsWith<CatalogException> { api.categories() }
            assertEquals(CatalogError.UNAVAILABLE, failure.error)
        } finally {
            client.close()
        }
    }

    @Test
    fun mapsSearchErrorsWithoutRetrying() = runTest {
        val cases = listOf(
            400 to """{"code":"INVALID_SEARCH_CRITERIA"}""" to CatalogError.INVALID_CRITERIA,
            503 to """{"code":"CATALOG_COPY_UNAVAILABLE"}""" to CatalogError.COPY_UNAVAILABLE,
            503 to """{"code":"CATALOG_UNAVAILABLE"}""" to CatalogError.UNAVAILABLE,
            503 to "no-json" to CatalogError.UNAVAILABLE,
            500 to """{"code":"INTERNAL_ERROR"}""" to CatalogError.UNAVAILABLE,
            302 to "" to CatalogError.UNAVAILABLE,
            200 to "no-json" to CatalogError.UNAVAILABLE,
        )
        for ((statusAndBody, expected) in cases) {
            val (status, body) = statusAndBody
            val manager = manager()
            manager.signIn("patient", "secret", false)
            var requests = 0
            val client = ApiHttpClient().create(MockEngine {
                requests++
                respond(
                    body,
                    HttpStatusCode.fromValue(status),
                    headersOf(HttpHeaders.ContentType, "application/problem+json"),
                )
            })
            try {
                val failure = assertFailsWith<CatalogException> {
                    KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager))
                        .searchProfessionals(ProfessionalQuery())
                }
                assertEquals(expected, failure.error)
                assertEquals(1, requests)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun readsCategoriesIncludingDisabledOnes() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { request ->
            assertEquals("/api/catalog/categories", request.url.encodedPath)
            respond(
                """{"content":[{"id":1,"name":"Clínica","enabled":true},""" +
                    """{"id":2,"name":"Antigua","enabled":false}],""" +
                    """"catalog":{"state":"CURRENT","copyAvailable":true}}""",
            )
        })
        try {
            val categories = KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager)).categories()
            assertEquals(
                listOf(CatalogCategory(1, "Clínica", true), CatalogCategory(2, "Antigua", false)),
                categories,
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun a401ClosesTheSessionThroughTheExistingClient() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine { respond("", HttpStatusCode.Unauthorized) })
        try {
            val failure = assertFailsWith<SessionException> {
                KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager))
                    .searchProfessionals(ProfessionalQuery())
            }
            assertEquals(SessionError.SESSION_REQUIRED, failure.error)
            assertEquals(SessionStatus.SIGNED_OUT, manager.state.value.status)
        } finally {
            client.close()
        }
    }

    @Test
    fun categoriesReportUnavailableCopyWithTheContractCode() = runTest {
        val manager = manager()
        manager.signIn("patient", "secret", false)
        val client = ApiHttpClient().create(MockEngine {
            respond(
                """{"code":"CATALOG_COPY_UNAVAILABLE"}""",
                HttpStatusCode.ServiceUnavailable,
                headersOf(HttpHeaders.ContentType, "application/problem+json"),
            )
        })
        try {
            val failure = assertFailsWith<CatalogException> {
                KtorCatalogApi(AuthenticatedApiClient(client, configuration, manager)).categories()
            }
            assertEquals(CatalogError.COPY_UNAVAILABLE, failure.error)
            assertTrue(failure.message!!.contains("información"))
        } finally {
            client.close()
        }
    }
}
