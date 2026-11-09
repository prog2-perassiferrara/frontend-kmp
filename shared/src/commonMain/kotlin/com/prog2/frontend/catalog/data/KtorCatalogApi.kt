package com.prog2.frontend.catalog.data

import com.prog2.frontend.catalog.CatalogError
import com.prog2.frontend.catalog.CatalogException
import com.prog2.frontend.network.ApiResponse
import com.prog2.frontend.network.AuthenticatedApiClient
import com.prog2.frontend.network.Backend
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Consume las dos consultas de CU-04 con el cliente autenticado existente.
 * El 401 lo resuelve ese cliente; aquí solo se interpretan los demás estados.
 */
class KtorCatalogApi(private val api: AuthenticatedApiClient) : CatalogApi {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun searchProfessionals(query: ProfessionalQuery): ProfessionalPage {
        val params = buildMap {
            query.categoryId?.let { put("categoryId", it.toString()) }
            query.name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
            query.enabled?.let { put("enabled", it.toString()) }
            query.availableOn?.takeIf { it.isNotBlank() }?.let { put("availableOn", it) }
            put("page", query.page.toString())
            put("size", query.size.toString())
        }
        val response = api.get(Backend.CATALOG, "/api/catalog/professionals", params)
        if (response.status != 200) throw failureFor(response)
        return decode {
            json.decodeFromString<ProfessionalPageResponse>(response.body).toPage()
        }
    }

    override suspend fun categories(): List<CatalogCategory> {
        val response = api.get(Backend.CATALOG, "/api/catalog/categories")
        if (response.status != 200) throw failureFor(response)
        return decode {
            json.decodeFromString<ProfessionalCategoriesResponse>(response.body).content.map { it.toCategory() }
        }
    }

    private fun failureFor(response: ApiResponse): CatalogException = when (response.status) {
        400 -> CatalogException(CatalogError.INVALID_CRITERIA)
        503 -> CatalogException(
            if (problemCode(response.body) == "CATALOG_COPY_UNAVAILABLE") {
                CatalogError.COPY_UNAVAILABLE
            } else {
                CatalogError.UNAVAILABLE
            },
        )
        else -> CatalogException(CatalogError.UNAVAILABLE)
    }

    private fun problemCode(body: String): String? = try {
        json.decodeFromString<CatalogProblem>(body).code
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    // Una respuesta declarada 200 pero ilegible no demuestra una búsqueda vacía.
    private inline fun <T> decode(block: () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        throw CatalogException(CatalogError.UNAVAILABLE)
    }
}

@Serializable
private class ProfessionalPageResponse(
    val content: List<ProfessionalResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val catalog: CatalogStateResponse,
) {
    fun toPage() = ProfessionalPage(
        content = content.map { it.toProfessional() },
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
        catalog = catalog.toState(),
    )
}

@Serializable
private class ProfessionalResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val enabled: Boolean,
    val category: CategoryResponse,
) {
    fun toProfessional() = Professional(id, firstName, lastName, enabled, category.toCategory())
}

@Serializable
private class ProfessionalCategoriesResponse(val content: List<CategoryResponse>)

@Serializable
private class CategoryResponse(val id: Long, val name: String, val enabled: Boolean) {
    fun toCategory() = CatalogCategory(id, name, enabled)
}

@Serializable
private class CatalogStateResponse(val state: String, val copyAvailable: Boolean) {
    fun toState() = CatalogState(state, copyAvailable)
}

@Serializable
private class CatalogProblem(val code: String? = null)
