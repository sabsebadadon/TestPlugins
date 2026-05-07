package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.app // This fixes the 'get' error
import org.jsoup.nodes.Element

class CinevoProvider : MainAPI() {
    override var mainUrl = "https://cinevo.site"
    override var name = "Cinevo"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Latest",
        "$mainUrl/movies" to "Movies",
        "$mainUrl/series" to "Series"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) request.data else "${request.data}?page=$page"
        val response = app.get(url).document
        val home = response.select("div.movie-card, div.item, article.poster").mapNotNull {
            val title = it.selectFirst("h2, h3, .title")?.text()?.trim() ?: return@mapNotNull null
            val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            newMovieSearchResponse(title, href, TvType.Movie)
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/$query"
        val response = app.get(url).document
        return response.select("div.movie-card, div.item, article.poster").mapNotNull {
            val title = it.selectFirst("h2, h3, .title")?.text()?.trim() ?: return@mapNotNull null
            val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            newMovieSearchResponse(title, href, TvType.Movie)
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val title = "Cinevo Result"
        return newMovieLoadResponse(title, url, TvType.Movie, url)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val response = app.get(data).document
        response.select("iframe").forEach { 
            val link = it.attr("src")
            if (link.contains("http")) {
                loadExtractor(link, data, subtitleCallback, callback)
            }
        }
        return true
    }
}
