package com.prog2.frontend.catalog.data

import com.prog2.frontend.catalog.CatalogError
import com.prog2.frontend.catalog.CatalogException
import com.prog2.frontend.network.ApiResponse
import com.prog2.frontend.network.apiJson
import com.prog2.frontend.network.withApiFallback
import com.prog2.frontend.network.AuthenticatedApiClient
import com.prog2.frontend.network.Backend
import kotlinx.serialization.Serializable

/**
 * Consume las dos consultas de CU-04 con el cliente autenticado existente.
 * El 401 lo resuelve ese cliente; aquí solo se interpretan los demás estados.
 */
class KtorCatalogApi(private val api: AuthenticatedApiClient) : CatalogApi {
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
        return withApiFallback({ throw CatalogException(CatalogError.UNAVAILABLE) }) {
            apiJson.decodeFromString<ProfessionalPage>(response.body)
        }
    }

    override suspend fun categories(): List<CatalogCategory> {
        val response = api.get(Backend.CATALOG, "/api/catalog/categories")
        if (response.status != 200) throw failureFor(response)
        return withApiFallback({ throw CatalogException(CatalogError.UNAVAILABLE) }) {
            apiJson.decodeFromString<ProfessionalCategoriesResponse>(response.body).content
        }
    }

    private fun failureFor(response: ApiResponse): CatalogException = when (response.status) {
        400 -> CatalogException(CatalogError.INVALID_CRITERIA)
        503 -> CatalogException(
            if (response.problem?.code == "CATALOG_COPY_UNAVAILABLE") {
                CatalogError.COPY_UNAVAILABLE
            } else {
                CatalogError.UNAVAILABLE
            },
        )
        else -> CatalogException(CatalogError.UNAVAILABLE)
    }
}

@Serializable
private class ProfessionalCategoriesResponse(val content: List<CatalogCategory>)
