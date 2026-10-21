package com.ezla.oslauncher.themes.data

import com.ezla.oslauncher.themes.Models.PreviewCard
import com.ezla.oslauncher.themes.Models.IconPack

// Dữ liệu mẫu lấy từ Figma (assets/themes_sample) tới khi có API; phần "owned" để rỗng.
class SampleMineRepository : MineRepository {

    override suspend fun getOwnedThemes(): List<PreviewCard> = emptyList()
    override suspend fun getOwnedWallpapers(): List<PreviewCard> = emptyList()
    override suspend fun getOwnedIconPacks(): List<IconPack> = emptyList()
    override suspend fun getFavorites(): List<IconPack> = emptyList()

    override suspend fun getSuggestThemes(): List<PreviewCard> = listOf(
        PreviewCard("snow", "Snow", asset("snow.jpg")),
        PreviewCard("matcha_glass", "Matcha Glass", asset("matcha_glass.jpg")),
        PreviewCard("cute", "Cute", asset("cute.jpg")),
        PreviewCard("halloween", "Halloween", asset("halloween.jpg"))
    )

    override suspend fun getSuggestWallpapers(): List<PreviewCard> = listOf(
        PreviewCard("deer", "Deer", asset("wp_deer.jpg")),
        PreviewCard("snow_hill", "Snow Hill", asset("wp_snow_hill.jpg")),
        PreviewCard("mountain", "Mountain", asset("wp_mountain.jpg")),
        PreviewCard("daisy", "Daisy", asset("wp_daisy.jpg")),
        PreviewCard("cherry", "Cherry", asset("wp_cherry.jpg"))
    )

    override suspend fun getSuggestIconPacks(): List<IconPack> = (1..4).map {
        IconPack("yellow_$it", "Yellow", yellowIcons())
    }

    private fun yellowIcons(): List<Any> {
        val messenger = asset("icon_yellow_messenger.png")
        val contact = asset("icon_yellow_contact.png")
        return listOf(messenger, contact, contact, messenger, contact, contact)
    }

    private fun asset(name: String) = "file:///android_asset/themes_sample/$name"
}
