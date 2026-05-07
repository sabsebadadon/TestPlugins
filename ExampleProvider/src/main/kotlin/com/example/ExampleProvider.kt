package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class CinevoProvider : MainAPI() {

    override var mainUrl = "https://cinevo.site"
    override var name = "Cinevo"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Latest",
        "$mainUrl/movies" to "Movies",
        "$mainUrl/series" to "Series"
    )

    // =========================
    // HOME PAGE
    // =========================
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val url =
            if (page == 1) request.data
            else "${request.data}?page=$page"

        val document = app.get(url).document

        val home = document.select("div.movie-card, div.item, article.poster").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            request.name,
            home
        )
    }

    // =========================
    // SEARCH
    // =========================
    override suspend fun search(query: String): List<SearchResponse> {

        val url = "$mainUrl/search/$query"

        val document = app.get(url).document

        return document.select("div.movie-card, div.item, article.poster")
            .mapNotNull {
                it.toSearchResult()
            }
    }

    // =========================
    // LOAD DETAILS PAGE
    // =========================
    override suspend fun load(url: String): LoadResponse {

        val document = app.get(url).document

        val title =
            document.selectFirst("h1.title, h1.entry-title")?.text()?.trim()
                ?: throw ErrorLoadingException("No title found")

        val poster =
            document.selectFirst("img.poster, div.poster img")
                ?.attr("src")

        val description =
            document.selectFirst("div.description, div.synopsis, p.plot")
                ?.text()

        val year =
            document.selectFirst("span.year")
                ?.text()
                ?.toIntOrNull()

        val tags =
            document.select("div.genres a, span.genre")
                .map { it.text() }

        val trailer =
            document.selectFirst("iframe.trailer")
                ?.attr("src")

        val recommendations =
            document.select("div.related article, div.recommendations .item")
                .mapNotNull {
                    it.toSearchResult()
                }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.recommendations = recommendations
            this.trailerUrl = trailer
        }
    }

    // =========================
    // EXTRACT VIDEO LINKS
    // =========================
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document

        // iframe players
        val iframes = document.select("iframe")

        iframes.forEach { iframe ->

            val link = iframe.attr("src")

            if (link.isNotBlank()) {

                loadExtractor(
                    fixUrl(link),
                    data,
                    subtitleCallback,
                    callback
                )
            }
        }

        // direct video sources
        document.select("video source").forEach { source ->

            val videoUrl = source.attr("src")

            if (videoUrl.isNotBlank()) {

                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        fixUrl(videoUrl),
                        "",
                        Qualities.Unknown.value,
                        false
                    )
                )
            }
        }

        return true
    }

    // =========================
    // PARSE SEARCH CARD
    // =========================
    private fun Element.toSearchResult(): SearchResponse? {

        val title =
            this.selectFirst("h2, h3, .title")
                ?.text()
                ?.trim()
                ?: return null

        val href =
            this.selectFirst("a")
                ?.attr("href")
                ?: return null

        val poster =
            this.selectFirst("img")
                ?.attr("src")

        return newMovieSearchResponse(
            title,
            fixUrl(href),
            TvType.Movie
        ) {
            this.posterUrl = poster
        }
    }
}

