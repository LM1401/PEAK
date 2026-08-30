package com.example.peak.data.remote.recognition

import com.example.peak.domain.model.BrandIdentityType

object BrandCatalog {
    val brands = listOf(
        BrandDefinition(
            key = "lucasfilm",
            displayName = "Lucasfilm",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STUDIO,
            whitelistedCompanyIds = setOf(1),
            namePatterns = listOf(Regex("lucasfilm"))
        ),
        BrandDefinition(
            key = "disney",
            displayName = "Disney",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STUDIO,
            whitelistedCompanyIds = setOf(2, 3, 6125),
            whitelistedNetworkIds = setOf(2, 2739),
            whitelistedProviderIds = setOf(337),
            namePatterns = listOf(Regex("disney"), Regex("walt disney"), Regex("pixar"))
        ),
        BrandDefinition(
            key = "sony_pictures",
            displayName = "Sony Pictures",
            tier = BrandTier.RECOGNISED,
            whitelistedCompanyIds = setOf(34),
            namePatterns = listOf(Regex("^sony$"), Regex("sony pictures"))
        ),
        BrandDefinition(
            key = "columbia",
            displayName = "Sony Pictures",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(5),
            namePatterns = listOf(Regex("columbia"))
        ),
        BrandDefinition(
            key = "universal",
            displayName = "Universal",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(33),
            namePatterns = listOf(Regex("^universal$"))
        ),
        BrandDefinition(
            key = "warner_bros",
            displayName = "Warner Bros.",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(174),
            namePatterns = listOf(Regex("warner bros"), Regex("warner brothers"))
        ),
        BrandDefinition(
            key = "amc",
            displayName = "AMC",
            tier = BrandTier.PREMIER,
            whitelistedNetworkIds = setOf(174),
            namePatterns = listOf(Regex("^amc$"))
        ),
        BrandDefinition(
            key = "paramount",
            displayName = "Paramount",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(4),
            namePatterns = listOf(Regex("paramount"))
        ),
        BrandDefinition(
            key = "mgm",
            displayName = "MGM Studios",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(21),
            namePatterns = listOf(Regex("mgm"), Regex("metro goldwyn mayer"))
        ),
        BrandDefinition(
            key = "amazon_mgm",
            displayName = "Amazon MGM Studios",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(210099),
            namePatterns = listOf(Regex("amazon mgm")),
            priority = 1
        ),
        BrandDefinition(
            key = "20th_century",
            displayName = "20th Century",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(25, 127928),
            namePatterns = listOf(Regex("20th century"))
        ),
        BrandDefinition(
            key = "marvel",
            displayName = "Marvel",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(420),
            namePatterns = listOf(Regex("marvel"))
        ),
        BrandDefinition(
            key = "dc",
            displayName = "DC",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(429, 184898),
            namePatterns = listOf(Regex("^dc$"), Regex("dc studios"))
        ),
        BrandDefinition(
            key = "a24",
            displayName = "A24",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(41077),
            namePatterns = listOf(Regex("^a24$"))
        ),
        BrandDefinition(
            key = "neon",
            displayName = "NEON",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(90733),
            namePatterns = listOf(Regex("^neon$"))
        ),
        BrandDefinition(
            key = "lionsgate",
            displayName = "Lionsgate",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(1632, 35, 6920),
            namePatterns = listOf(Regex("lionsgate"), Regex("lions gate"))
        ),
        BrandDefinition(
            key = "new_line",
            displayName = "New Line Cinema",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(12),
            namePatterns = listOf(Regex("new line"))
        ),
        BrandDefinition(
            key = "dreamworks",
            displayName = "DreamWorks",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(521),
            namePatterns = listOf(Regex("dreamworks"))
        ),
        BrandDefinition(
            key = "legendary",
            displayName = "Legendary",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(923),
            namePatterns = listOf(Regex("legendary"))
        ),
        BrandDefinition(
            key = "blumhouse",
            displayName = "Blumhouse",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(3172),
            namePatterns = listOf(Regex("blumhouse"))
        ),
        BrandDefinition(
            key = "focus_features",
            displayName = "Focus Features",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(10146),
            namePatterns = listOf(Regex("focus features"))
        ),
        BrandDefinition(
            key = "searchlight",
            displayName = "Searchlight Pictures",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(127929),
            namePatterns = listOf(Regex("searchlight"))
        ),
        BrandDefinition(
            key = "miramax",
            displayName = "Miramax",
            tier = BrandTier.RECOGNISED,
            whitelistedCompanyIds = setOf(14),
            namePatterns = listOf(Regex("miramax"))
        ),
        BrandDefinition(
            key = "nat_geo",
            displayName = "National Geographic",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(43),
            namePatterns = listOf(Regex("national geographic"))
        ),
        BrandDefinition(
            key = "netflix",
            displayName = "Netflix Original",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedCompanyIds = setOf(178464),
            whitelistedNetworkIds = setOf(213),
            whitelistedProviderIds = setOf(8),
            namePatterns = listOf(Regex("netflix"))
        ),
        BrandDefinition(
            key = "amazon",
            displayName = "Prime Video",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedCompanyIds = setOf(20580),
            whitelistedNetworkIds = setOf(1024),
            whitelistedProviderIds = setOf(9, 10, 119),
            namePatterns = listOf(Regex("amazon"), Regex("prime video")),
            networkDisplayName = "Prime Exclusive"
        ),
        BrandDefinition(
            key = "apple",
            displayName = "Apple TV+",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedCompanyIds = setOf(194232),
            whitelistedNetworkIds = setOf(2552),
            whitelistedProviderIds = setOf(350),
            namePatterns = listOf(Regex("apple"))
        ),
        BrandDefinition(
            key = "hbo",
            displayName = "HBO",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.NETWORK,
            whitelistedNetworkIds = setOf(49),
            whitelistedProviderIds = setOf(384),
            namePatterns = listOf(Regex("^hbo$"))
        ),
        BrandDefinition(
            key = "showtime",
            displayName = "Showtime",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.NETWORK,
            whitelistedNetworkIds = setOf(67),
            namePatterns = listOf(Regex("showtime"))
        ),
        BrandDefinition(
            key = "fx",
            displayName = "FX",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.NETWORK,
            whitelistedNetworkIds = setOf(88),
            namePatterns = listOf(Regex("^fx$"))
        ),
        BrandDefinition(
            key = "hulu",
            displayName = "Hulu",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedNetworkIds = setOf(453),
            whitelistedProviderIds = setOf(15),
            namePatterns = listOf(Regex("hulu"))
        )
    )
}
