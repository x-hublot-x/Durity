package com.example.project1.ui.wallpaper

enum class WallpaperType {
    DEFAULT,
    STATIC_PRESET,
    GIF_PRESET,
    CUSTOM
}

data class WallpaperItem(
    val id: String,
    val title: String,
    val description: String,
    val type: WallpaperType,
    val assetFileName: String? = null,
    val customFilePath: String? = null,
    val price: Int = 0
) {
    val isAnimated: Boolean
        get() = type == WallpaperType.GIF_PRESET || (customFilePath?.endsWith(".gif", ignoreCase = true) == true)

    val imageModel: Any?
        get() = when (type) {
            WallpaperType.DEFAULT -> null
            WallpaperType.STATIC_PRESET, WallpaperType.GIF_PRESET -> {
                assetFileName?.let { "file:///android_asset/wallpapers/$it" }
            }
            WallpaperType.CUSTOM -> customFilePath?.let { java.io.File(it) }
        }
}

object WallpaperCatalog {
    val DEFAULT = WallpaperItem(
        id = "default",
        title = "По умолчанию",
        description = "Классический глубокий темный фон",
        type = WallpaperType.DEFAULT,
        price = 0
    )

    const val GIF_PRICE = 799

    val staticPresets = listOf(
        WallpaperItem(
            id = "static_minimal_dark",
            title = "Минимализм",
            description = "Геометрический глубокий темный градиент",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_minimal_dark.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_neon_fog",
            title = "Неоновый туман",
            description = "Мягкое неоновое свечение в ночи",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_neon_fog.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_dark_marble",
            title = "Тёмный мрамор",
            description = "Мраморная текстура с золотистыми прожилками",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_dark_marble.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_deep_cosmos",
            title = "Глубокий космос",
            description = "Звездные скопления и фиолетовая галактика",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_deep_cosmos.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_cyber_grid",
            title = "Кибер-сетка",
            description = "3D ретро-сетка на закатном горизонте",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_cyber_grid.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_night_city",
            title = "Ночной город",
            description = "Огни ночных небоскребов мегаполиса",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_night_city.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_sunset_glow",
            title = "Пурпурный закат",
            description = "Закатные горы и сумеречный градиент",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_sunset_glow.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_aurora_borealis",
            title = "Северное сияние",
            description = "Изумрудное полярное сияние над снежными пиками",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_aurora_borealis.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_ocean_depths",
            title = "Глубины океана",
            description = "Лазурные лучи света в толще океана",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_ocean_depths.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_emerald_forest",
            title = "Изумрудный лес",
            description = "Таинственный хвойный лес в утренней дымке",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_emerald_forest.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_magma_core",
            title = "Ядро магмы",
            description = "Базальтовые разломы с пылающей лавой",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_magma_core.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_zen_garden",
            title = "Сад сакуры",
            description = "Ночная луна, ветви цветущей сакуры и лепестки",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_zen_garden.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_foggy_mountains",
            title = "Туманные вершины",
            description = "Величественные горные хребты в густом тумане",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_foggy_mountains.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_cyber_rain",
            title = "Неоновый Токио",
            description = "Мокрые улицы с кибер-вывесками и неоновым асфальтом",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_cyber_rain.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_golden_sunset",
            title = "Золотой рассвет",
            description = "Теплые золотисто-янтарные лучи солнца над облаками",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_golden_sunset.jpg",
            price = 0
        ),
        WallpaperItem(
            id = "static_abstract_prism",
            title = "Квантовая призма",
            description = "Футуристические грани кристалла с преломлением света",
            type = WallpaperType.STATIC_PRESET,
            assetFileName = "wp_static_abstract_prism.jpg",
            price = 0
        )
    )

    val gifPresets = listOf(
        WallpaperItem(
            id = "gif_cosmic_nebula",
            title = "Туманность",
            description = "Пульсирующее ядро галактики и мерцающие звезды",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_cosmic_nebula.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_pixel_rain",
            title = "Цифровой дождь",
            description = "Неоновый поток цифровых символов в стиле киберпанк",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_pixel_rain.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_neon_waves",
            title = "Неоновые волны",
            description = "Плавные светящиеся неоновые потоки",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_neon_waves.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_fire_sparks",
            title = "Огненные искры",
            description = "Взлетающие горящие искры и теплые угли",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_fire_sparks.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_shooting_stars",
            title = "Звездопад",
            description = "Звездное небо и пролетающие метеоры",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_shooting_stars.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_cyber_horizon",
            title = "Кибер-горизонт",
            description = "Движущаяся неоновая 3D-сетка горизонта",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_cyber_horizon.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_underwater",
            title = "Морская бездна",
            description = "Игра света под водой и всплывающие пузырьки",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_underwater.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_aurora_stream",
            title = "Живое сияние",
            description = "Волнообразное мерцание полярного сияния",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_aurora_stream.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_rainy_window",
            title = "Дождь за стеклом",
            description = "Стекающие капли дождя и мягкие ночные огни боке",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_rainy_window.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_campfire_night",
            title = "Ночной костёр",
            description = "Уютный теплый свет костра в ночном лесу",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_campfire_night.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_portal_vortex",
            title = "Квантовый вихрь",
            description = "Плавное гипнотическое вращение светового портала",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_portal_vortex.gif",
            price = GIF_PRICE
        ),
        WallpaperItem(
            id = "gif_floating_dust",
            title = "Магическая пыльца",
            description = "Плавно парящие и медленно мерцающие светлячки",
            type = WallpaperType.GIF_PRESET,
            assetFileName = "wp_gif_floating_dust.gif",
            price = GIF_PRICE
        )
    )

    fun findById(id: String, customPath: String? = null): WallpaperItem {
        if (id == DEFAULT.id) return DEFAULT
        if (id == "custom") {
            return WallpaperItem(
                id = "custom",
                title = "Моё фото",
                description = "Пользовательское изображение из галереи",
                type = WallpaperType.CUSTOM,
                customFilePath = customPath,
                price = 0
            )
        }
        return staticPresets.find { it.id == id }
            ?: gifPresets.find { it.id == id }
            ?: DEFAULT
    }
}