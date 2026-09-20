package com.example.peak.data.remote.recognition

import com.example.peak.domain.model.BrandIdentityType
import com.example.peak.domain.model.BrandPresentationStyle

object BrandCatalog {
    val brands = listOf(
        BrandDefinition(
            key = "lucasfilm",
            displayName = "Lucasfilm",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STUDIO,
            whitelistedCompanyIds = setOf(1),
            namePatterns = listOf(Regex("lucasfilm")),
            presentationName = "Lucasfilm",
            presentationStyle = BrandPresentationStyle.PRODUCTION
        ),
        BrandDefinition(
            key = "disney",
            displayName = "Disney",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STUDIO,
            whitelistedCompanyIds = setOf(2, 3, 6125),
            whitelistedNetworkIds = setOf(2, 2739),
            whitelistedProviderIds = setOf(337),
            namePatterns = listOf(Regex("disney"), Regex("walt disney"), Regex("pixar")),
            presentationName = "Disney",
            presentationStyle = BrandPresentationStyle.PRODUCTION
        ),
        BrandDefinition(
            key = "sony_pictures",
            displayName = "Sony Pictures",
            tier = BrandTier.RECOGNISED,
            whitelistedCompanyIds = setOf(34),
            namePatterns = listOf(Regex("^sony$"), Regex("sony pictures")),
            presentationName = "Sony Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "columbia",
            displayName = "Sony Pictures",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(5),
            namePatterns = listOf(Regex("columbia")),
            presentationName = "Sony Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "universal",
            displayName = "Universal",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(33),
            namePatterns = listOf(Regex("^universal$")),
            presentationName = "Universal Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "warner_bros",
            displayName = "Warner Bros.",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(174),
            namePatterns = listOf(Regex("warner bros"), Regex("warner brothers")),
            presentationName = "Warner Bros. Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "amc",
            displayName = "AMC",
            tier = BrandTier.PREMIER,
            whitelistedNetworkIds = setOf(174),
            namePatterns = listOf(Regex("^amc$")),
            presentationName = "AMC",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        ),
        BrandDefinition(
            key = "paramount",
            displayName = "Paramount",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(4),
            namePatterns = listOf(Regex("paramount")),
            priority = 0,
            presentationName = "Paramount Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "paramount_plus",
            displayName = "Paramount+",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedNetworkIds = setOf(433),
            whitelistedProviderIds = setOf(531),
            namePatterns = listOf(Regex("paramount\\+"), Regex("paramount plus")),
            priority = 1,
            presentationName = "Paramount+",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        ),
        BrandDefinition(
            key = "mgm",
            displayName = "MGM Studios",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(21),
            namePatterns = listOf(Regex("mgm"), Regex("metro goldwyn mayer")),
            priority = 0,
            presentationName = "MGM Studios",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "amazon_mgm",
            displayName = "Amazon MGM Studios",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(210099),
            namePatterns = listOf(Regex("amazon mgm")),
            priority = 1,
            presentationName = "Amazon MGM Studios",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "20th_century",
            displayName = "20th Century",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(25, 127928),
            namePatterns = listOf(Regex("20th century")),
            presentationName = "20th Century Studios",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "marvel",
            displayName = "Marvel",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(420),
            namePatterns = listOf(Regex("marvel")),
            presentationName = "Marvel Studios",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "dc",
            displayName = "DC",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(429, 184898),
            namePatterns = listOf(Regex("^dc$"), Regex("dc studios")),
            presentationName = "DC Studios",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "a24",
            displayName = "A24",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(41077),
            namePatterns = listOf(Regex("^a24$")),
            presentationName = "A24",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "neon",
            displayName = "NEON",
            tier = BrandTier.PREMIER,
            whitelistedCompanyIds = setOf(90733),
            namePatterns = listOf(Regex("^neon$")),
            presentationName = "NEON",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "lionsgate",
            displayName = "Lionsgate",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(1632, 35, 6920),
            namePatterns = listOf(Regex("lionsgate"), Regex("lions gate")),
            presentationName = "Lionsgate",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "new_line",
            displayName = "New Line Cinema",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(12),
            namePatterns = listOf(Regex("new line")),
            presentationName = "New Line Cinema",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "dreamworks",
            displayName = "DreamWorks",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(521),
            namePatterns = listOf(Regex("dreamworks")),
            presentationName = "DreamWorks Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "legendary",
            displayName = "Legendary",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(923),
            namePatterns = listOf(Regex("legendary")),
            presentationName = "Legendary Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "blumhouse",
            displayName = "Blumhouse",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(3172),
            namePatterns = listOf(Regex("blumhouse")),
            presentationName = "Blumhouse",
            presentationStyle = BrandPresentationStyle.PRODUCTION
        ),
        BrandDefinition(
            key = "focus_features",
            displayName = "Focus Features",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(10146),
            namePatterns = listOf(Regex("focus features")),
            presentationName = "Focus Features",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "searchlight",
            displayName = "Searchlight Pictures",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(127929),
            namePatterns = listOf(Regex("searchlight")),
            presentationName = "Searchlight Pictures",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "miramax",
            displayName = "Miramax",
            tier = BrandTier.RECOGNISED,
            whitelistedCompanyIds = setOf(14),
            namePatterns = listOf(Regex("miramax")),
            presentationName = "Miramax",
            presentationStyle = BrandPresentationStyle.PRESENTS
        ),
        BrandDefinition(
            key = "nat_geo",
            displayName = "National Geographic",
            tier = BrandTier.MAJOR,
            whitelistedCompanyIds = setOf(43),
            namePatterns = listOf(Regex("national geographic")),
            presentationName = "National Geographic",
            presentationStyle = BrandPresentationStyle.PRODUCTION
        ),
        BrandDefinition(
            key = "netflix",
            displayName = "Netflix Original",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedCompanyIds = setOf(178464),
            whitelistedNetworkIds = setOf(213),
            whitelistedProviderIds = setOf(8),
            namePatterns = listOf(Regex("netflix")),
            presentationName = "Netflix",
            presentationStyle = BrandPresentationStyle.ORIGINAL
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
            networkDisplayName = "Prime Exclusive",
            presentationName = "Prime Video",
            presentationStyle = BrandPresentationStyle.ORIGINAL,
            networkPresentationName = "Prime",
            networkPresentationStyle = BrandPresentationStyle.EXCLUSIVE
        ),
        BrandDefinition(
            key = "apple",
            displayName = "Apple TV+",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedCompanyIds = setOf(194232),
            whitelistedNetworkIds = setOf(2552),
            whitelistedProviderIds = setOf(350),
            namePatterns = listOf(Regex("apple")),
            presentationName = "Apple",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        ),
        BrandDefinition(
            key = "hbo",
            displayName = "HBO",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.NETWORK,
            whitelistedNetworkIds = setOf(49),
            whitelistedProviderIds = setOf(384),
            namePatterns = listOf(Regex("^hbo$")),
            presentationName = "HBO",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        ),
        BrandDefinition(
            key = "showtime",
            displayName = "Showtime",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.NETWORK,
            whitelistedNetworkIds = setOf(67),
            namePatterns = listOf(Regex("showtime")),
            presentationName = "Showtime",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        ),
        BrandDefinition(
            key = "fx",
            displayName = "FX",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.NETWORK,
            whitelistedNetworkIds = setOf(88),
            namePatterns = listOf(Regex("^fx$")),
            presentationName = "FX",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        ),
        BrandDefinition(
            key = "hulu",
            displayName = "Hulu",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedNetworkIds = setOf(453),
            whitelistedProviderIds = setOf(15),
            namePatterns = listOf(Regex("hulu")),
            presentationName = "Hulu",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        ),
        BrandDefinition(
            key = "itv",
            displayName = "ITV",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.NETWORK,
            whitelistedCompanyIds = setOf(3290),
            whitelistedNetworkIds = setOf(9, 675),
            namePatterns = listOf(Regex("^itv$"), Regex("itv studios")),
            presentationName = "ITV",
            presentationStyle = BrandPresentationStyle.PRODUCTION
        ),
        BrandDefinition(
            key = "mgm_plus",
            displayName = "MGM+",
            tier = BrandTier.PREMIER,
            type = BrandIdentityType.STREAMER,
            whitelistedNetworkIds = setOf(233),
            whitelistedProviderIds = setOf(34),
            namePatterns = listOf(Regex("mgm\\+"), Regex("mgm plus")),
            priority = 1,
            presentationName = "MGM+",
            presentationStyle = BrandPresentationStyle.ORIGINAL
        )
    )
}
