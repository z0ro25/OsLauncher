package com.ezla.oslauncher.controlcenter.panel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ezla.oslauncher.controlcenter.CcPrefs
import com.ezla.oslauncher.controlcenter.data.CcTile
import com.ezla.oslauncher.controlcenter.databinding.CcPanelMainBinding
import com.ezla.oslauncher.controlcenter.databinding.CcPanelMusicBinding
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.record.ScreenRecordService
import com.ezla.oslauncher.controlcenter.record.ScreenRecordStarter
import com.ezla.oslauncher.controlcenter.skin.applySkin
import com.ezla.oslauncher.controlcenter.skin.CcAccent
import com.ezla.oslauncher.controlcenter.skin.CcSkinSlot
import com.ezla.oslauncher.controlcenter.skin.CcSkinSource
import com.ezla.oslauncher.controlcenter.system.CcActions
import com.ezla.oslauncher.controlcenter.system.CcHaptics
import com.ezla.oslauncher.controlcenter.system.CcPermissions
import com.ezla.oslauncher.controlcenter.system.FlashController
import com.ezla.oslauncher.controlcenter.system.MediaMonitor
import com.ezla.oslauncher.controlcenter.system.SystemStateMonitor
import com.ezla.oslauncher.controlcenter.widget.CcPageFrame

// 3 trang panel (Chính / Nhạc / Kết nối) gắn vào ViewPager2 dọc. Dùng chung cho overlay thật và
// màn Custom Controls ([editMode] = true: chỉ hiển thị + sửa lưới ô, không điều khiển hệ thống).
class ControlPanelPages(
    private val context: Context,
    private val pager: ViewPager2,
    private val editMode: Boolean,
    private val host: Host
) {

    interface Host {
        // Đóng panel trước khi mở màn khác (overlay); màn sửa thì bỏ qua.
        fun closePanel()
        fun onPageChanged(page: Int) {}
        fun onAddTileRequested() {}
        fun onTilesChanged(tiles: List<CcTile>) {}
    }

    private val inflater = LayoutInflater.from(context)
    private val main = Handler(Looper.getMainLooper())
    val mainBinding = CcPanelMainBinding.inflate(inflater)
    val musicBinding = CcPanelMusicBinding.inflate(inflater)
    private val connList = RecyclerView(context)

    private val state = SystemStateMonitor(context) { refresh() }
    private val flash = FlashController(context) { refresh() }
    private val media = MediaMonitor(context) { renderMedia(it) }
    private var mediaInfo: MediaMonitor.Info? = null
    private var userSeeking = false

    // Màn sửa có header 48dp nên đẩy trang chính xuống thêm; overlay theo Figma y = 114dp.
    var pageTopOffsetDp = 114

    private var tiles: List<CcTile> = CcPrefs.tiles(context)
    private var accent: CcAccent? = null
    private val tileAdapter = TileGridAdapter(
        isOn = ::isTileOn,
        onClick = ::onTileClick,
        onRemove = ::removeTile,
        onAdd = { host.onAddTileRequested() }
    )
    private val connAdapter = ConnectionAdapter(::onConnectionClick)

    private val progressTick = object : Runnable {
        override fun run() {
            renderProgress()
            main.postDelayed(this, 1000)
        }
    }

    init {
        setupMain()
        setupMusic()
        connList.layoutManager = LinearLayoutManager(context)
        connList.adapter = connAdapter
        connList.overScrollMode = View.OVER_SCROLL_NEVER
        connList.layoutParams = ViewGroup.LayoutParams(
            context.resources.getDimensionPixelSize(R.dimen.cc_panel_width), ViewGroup.LayoutParams.WRAP_CONTENT
        )
        pager.adapter = PagesAdapter()
        pager.offscreenPageLimit = 2
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = host.onPageChanged(position)
        })
    }

    // ===== Vòng đời: chỉ nghe hệ thống khi panel đang hiện để tiết kiệm pin =====

    fun start() {
        if (!editMode) {
            state.start()
            flash.start()
            media.start()
            main.post(progressTick)
        }
        refresh()
    }

    fun stop() {
        state.stop()
        flash.stop()
        media.stop()
        main.removeCallbacks(progressTick)
    }

    // Gắn ảnh nền ô theo theme cho cả 3 trang; null = trả về nền kính mặc định.
    fun applySkin(source: CcSkinSource?) {
        val src = source ?: CcSkinSource { null }
        mainBinding.applySkin(context, src)
        tileAdapter.skin = source
        connAdapter.skin = source
        musicBinding.root.background = src.get(CcSkinSlot.MUSIC)
            ?: ContextCompat.getDrawable(context, R.drawable.cc_bg_tile_big)
    }

    // Màu "đang bật" của theme (1-3 màu) cho ô 1x1, chip kết nối, 2 thanh trượt và thanh nhạc;
    // null = màu gốc. Icon giữ nguyên, chỉ đổi nền.
    fun applyAccent(accent: CcAccent?) {
        this.accent = accent
        val m = mainBinding
        m.ccTileRotation.accent = accent
        m.ccTileSilent.accent = accent
        m.ccSliderBrightness.accent = accent
        m.ccSliderVolume.accent = accent
        tileAdapter.accent = accent
        connAdapter.accent = accent
        tintProgress(musicBinding.ccMusicProgress, accent)
        tintProgress(musicBinding.ccMusicVolume, accent)
        refresh()
    }

    // Thay lớp tiến trình của SeekBar bằng màu theme, giữ nguyên lớp nền xám của cc_bg_progress.
    private fun tintProgress(sb: SeekBar, accent: CcAccent?) {
        val layers = ContextCompat.getDrawable(context, R.drawable.cc_bg_progress)?.mutate() as? LayerDrawable ?: return
        if (accent != null) {
            val fill = ClipDrawable(accent.drawable(GradientDrawable.RECTANGLE, dp(3f)), Gravity.START, ClipDrawable.HORIZONTAL)
            layers.setDrawableByLayerId(android.R.id.progress, fill)
        }
        val p = sb.progress
        sb.progressDrawable = layers
        sb.progress = p
    }

    private fun dp(v: Float) = v * context.resources.displayMetrics.density

    fun showPage(page: Int, smooth: Boolean = true) = pager.setCurrentItem(page, smooth)

    fun reloadTiles() {
        tiles = CcPrefs.tiles(context)
        tileAdapter.tiles = tiles
    }

    fun addTile(tile: CcTile) {
        if (tiles.size >= CcTile.MAX || tile in tiles) return
        tiles = tiles + tile
        saveTiles()
    }

    fun currentTiles() = tiles

    private fun removeTile(tile: CcTile) {
        tiles = tiles - tile
        saveTiles()
    }

    private fun saveTiles() {
        CcPrefs.saveTiles(context, tiles)
        tileAdapter.tiles = tiles
        host.onTilesChanged(tiles)
    }

    // Nhấn giữ ô rồi kéo để đổi chỗ (chỉ màn Custom Controls). Thả tay mới lưu, giống nút "–" / "+":
    // lưu ngay, Back không Apply thì CcControlsActivity trả lại thứ tự cũ.
    private fun attachTileDrag(grid: RecyclerView) {
        val all = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(all, 0) {
            override fun getMovementFlags(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int =
                if (tileAdapter.isMovable(vh.bindingAdapterPosition)) makeMovementFlags(all, 0) else 0

            override fun canDropOver(rv: RecyclerView, cur: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) =
                tileAdapter.isMovable(target.bindingAdapterPosition)

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) =
                tileAdapter.move(vh.bindingAdapterPosition, target.bindingAdapterPosition)

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    // Pager dọc sẽ giành cử chỉ kéo dọc để chuyển trang: khoá trong lúc kéo ô.
                    pager.isUserInputEnabled = false
                    vh?.itemView?.animate()?.scaleX(DRAG_SCALE)?.scaleY(DRAG_SCALE)?.setDuration(120)?.start()
                    vh?.itemView?.let { CcHaptics.tap(it) }
                }
            }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                pager.isUserInputEnabled = true
                vh.itemView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                tileAdapter.endDrag()?.let { newOrder ->
                    tiles = newOrder
                    CcPrefs.saveTiles(context, tiles)
                    host.onTilesChanged(tiles)
                }
            }
        }).attachToRecyclerView(grid)
    }

    // ===== Trang chính =====

    private fun setupMain() {
        val b = mainBinding
        b.ccTileGrid.layoutManager = GridLayoutManager(context, 4)
        b.ccTileGrid.addItemDecoration(TileGridDecoration(
            context.resources.getDimensionPixelSize(R.dimen.cc_panel_width),
            context.resources.getDimensionPixelSize(R.dimen.cc_tile),
            context.resources.getDimensionPixelSize(R.dimen.cc_tile_row_gap)
        ))
        b.ccTileGrid.itemAnimator = null
        tileAdapter.editMode = editMode
        tileAdapter.tiles = tiles
        b.ccTileGrid.adapter = tileAdapter

        b.ccTileRotation.setIcon(R.drawable.cc_ic_rotation_lock)
        b.ccTileSilent.setIcon(R.drawable.cc_ic_bell_slash)
        b.ccSliderBrightness.icon = ContextCompat.getDrawable(context, R.drawable.cc_ic_sun)
        b.ccSliderVolume.icon = ContextCompat.getDrawable(context, R.drawable.cc_ic_volume)

        if (editMode) {
            attachTileDrag(b.ccTileGrid)
            // Màn sửa: khối cố định chỉ để xem, không phản hồi chạm.
            listOf(b.ccConnectivity, b.ccNowPlaying, b.ccTileRotation, b.ccTileSilent, b.ccFocus,
                b.ccSliderBrightness, b.ccSliderVolume).forEach { it.isEnabled = false }
            b.ccSliderBrightness.isLocked = true
            b.ccSliderVolume.isLocked = true
            return
        }

        b.ccChipAirplane.setOnClickListener { open { CcActions.airplane(it) } }
        b.ccChipWifi.setOnClickListener { open { CcActions.wifi(it) } }
        b.ccChipCellular.setOnClickListener { open { CcActions.cellular(it) } }
        b.ccChipBluetooth.setOnClickListener { open { CcActions.bluetooth(it) } }
        b.ccChipHotspot.setOnClickListener { open { CcActions.hotspot(it) } }
        b.ccChipSync.setOnClickListener { open { CcActions.sync(it) } }
        b.ccChipMore.setOnClickListener { tap(it); showPage(PAGE_CONNECTIVITY) }
        b.ccConnectivity.setOnClickListener { tap(it); showPage(PAGE_CONNECTIVITY) }

        b.ccNowPlaying.setOnClickListener { tap(it); showPage(PAGE_MUSIC) }
        b.ccNpPlay.setOnClickListener { tap(it); withMedia { media.playPause() } }
        b.ccNpNext.setOnClickListener { tap(it); withMedia { media.next() } }
        b.ccNpPrev.setOnClickListener { tap(it); withMedia { media.previous() } }

        b.ccTileRotation.setOnClickListener {
            tap(it)
            if (!state.toggleRotationLock()) askPermission(CcPermissions.writeSettings(context), R.string.cc_need_write_settings)
        }
        b.ccTileSilent.setOnClickListener {
            tap(it)
            if (!state.toggleSilent()) askPermission(CcPermissions.notificationPolicySettings(), 0)
            refresh()
        }
        b.ccFocus.setOnClickListener { open { CcActions.focus(it) } }

        b.ccSliderBrightness.onValueChanged = { v, fromUser ->
            if (fromUser) state.setBrightness((v * 255).toInt())
        }
        b.ccSliderBrightness.onLockedTouch = {
            askPermission(CcPermissions.writeSettings(context), R.string.cc_need_write_settings)
        }
        b.ccSliderVolume.onValueChanged = { v, fromUser -> if (fromUser) state.setVolumeFraction(v) }
    }

    private fun isTileOn(tile: CcTile): Boolean = when (tile) {
        CcTile.FLASHLIGHT -> flash.isOn
        CcTile.SCREEN_RECORD -> ScreenRecordService.isRecording
        CcTile.DARK_MODE -> state.isDarkMode()
        CcTile.LOW_POWER -> state.isPowerSave()
        else -> false
    }

    private fun onTileClick(tile: CcTile) {
        when (tile) {
            CcTile.FLASHLIGHT -> {
                flash.toggle()
                CcHaptics.tap(mainBinding.ccTileGrid)
            }
            CcTile.ALARM -> open { CcActions.alarm(it) }
            CcTile.CALCULATOR -> open { CcActions.calculator(it) }
            CcTile.CAMERA -> open { CcActions.camera(it) }
            CcTile.SCREEN_RECORD -> open {
                if (ScreenRecordService.isRecording) ScreenRecordService.stop(it) else ScreenRecordStarter.start(it)
            }
            CcTile.DARK_MODE -> open { CcActions.darkMode(it) }
            CcTile.LOW_POWER -> open { CcActions.lowPower(it) }
            CcTile.VOICE_MEMO -> open { CcActions.voiceMemo(it) }
            CcTile.SETTINGS -> open { CcActions.settings(it) }
        }
    }

    // ===== Trang nhạc =====

    private fun setupMusic() {
        val b = musicBinding
        if (editMode) {
            listOf(b.ccMusicPlay, b.ccMusicPrev, b.ccMusicNext, b.ccMusicProgress, b.ccMusicVolume)
                .forEach { it.isEnabled = false }
            return
        }
        b.ccMusicPlay.setOnClickListener { tap(it); withMedia { media.playPause() } }
        b.ccMusicNext.setOnClickListener { tap(it); withMedia { media.next() } }
        b.ccMusicPrev.setOnClickListener { tap(it); withMedia { media.previous() } }
        b.ccMusicArt.setOnClickListener { if (mediaInfo != null) open { c -> media.openPlayer(c) } }
        b.ccMusicProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) = Unit
            override fun onStartTrackingTouch(sb: SeekBar) {
                userSeeking = true
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
                userSeeking = false
                val dur = mediaInfo?.duration ?: return
                if (dur > 0) media.seekTo(dur * sb.progress / sb.max)
            }
        })
        b.ccMusicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) state.setVolumeFraction(p / sb.max.toFloat())
            }

            override fun onStartTrackingTouch(sb: SeekBar) = Unit
            override fun onStopTrackingTouch(sb: SeekBar) = Unit
        })
    }

    private fun renderMedia(info: MediaMonitor.Info?) {
        mediaInfo = info
        val m = mainBinding
        val b = musicBinding
        val title = info?.title?.takeIf { it.isNotBlank() }
        m.ccNpTitle.text = title ?: context.getString(if (info == null) R.string.cc_not_playing else R.string.cc_track)
        m.ccNpArtist.text = info?.artist ?: if (info == null) "" else context.getString(R.string.cc_artist)
        b.ccMusicTitle.text = title ?: context.getString(R.string.cc_not_playing)
        b.ccMusicApp.text = info?.let { appLabel(it.appPackage) } ?: context.getString(R.string.cc_app_name)
        if (info?.art != null) {
            m.ccNpArt.setImageBitmap(info.art)
            b.ccMusicArt.setImageBitmap(info.art)
        } else {
            m.ccNpArt.setImageDrawable(null)
            b.ccMusicArt.setImageResource(R.drawable.cc_img_default_bg)
        }
        val playIcon = if (info?.playing == true) R.drawable.cc_ic_pause else R.drawable.cc_ic_play
        m.ccNpPlay.setImageResource(playIcon)
        b.ccMusicPlay.setImageResource(playIcon)
        val dim = { v: View, enabled: Boolean -> v.alpha = if (enabled) 1f else 0.4f }
        dim(m.ccNpNext, info?.hasNext ?: false)
        dim(m.ccNpPrev, info?.hasPrev ?: false)
        dim(b.ccMusicNext, info?.hasNext ?: false)
        dim(b.ccMusicPrev, info?.hasPrev ?: false)
        renderProgress()
    }

    private fun renderProgress() {
        val b = musicBinding
        b.ccMusicVolume.progress = (state.volumeFraction() * b.ccMusicVolume.max).toInt()
        val info = media.current()
        val dur = info?.duration ?: 0L
        val pos = info?.position ?: 0L
        if (!userSeeking) {
            b.ccMusicProgress.progress = if (dur > 0) (pos * b.ccMusicProgress.max / dur).toInt() else 0
        }
        b.ccMusicPos.text = formatTime(pos)
        b.ccMusicDur.text = formatTime(dur)
    }

    private fun formatTime(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return if (s >= 3600) String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
        else String.format("%d:%02d", s / 60, s % 60)
    }

    private fun appLabel(pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    // ===== Trang kết nối =====

    private fun onConnectionClick(item: ConnectionAdapter.Item) {
        if (editMode) return
        when (item) {
            ConnectionAdapter.Item.AIRPLANE -> open { CcActions.airplane(it) }
            ConnectionAdapter.Item.WIFI -> open { CcActions.wifi(it) }
            ConnectionAdapter.Item.CELLULAR -> open { CcActions.cellular(it) }
            ConnectionAdapter.Item.BLUETOOTH -> open { CcActions.bluetooth(it) }
            ConnectionAdapter.Item.HOTSPOT -> open { CcActions.hotspot(it) }
            ConnectionAdapter.Item.SYNC -> open { CcActions.sync(it) }
        }
    }

    // ===== Trạng thái chung =====

    fun refresh() {
        val b = mainBinding
        val airplane = state.isAirplaneOn()
        val wifi = state.isWifiOn()
        val cellular = state.isCellularOn()
        val bt = state.isBluetoothOn()
        val sync = state.isSyncOn()
        chip(b.ccChipAirplane, airplane, R.drawable.cc_bg_chip_blue)
        chip(b.ccChipWifi, wifi, R.drawable.cc_bg_chip_blue)
        chip(b.ccChipCellular, cellular, R.drawable.cc_bg_chip_green)
        chip(b.ccChipBluetooth, bt, R.drawable.cc_bg_chip_blue)
        chip(b.ccChipSync, sync, R.drawable.cc_bg_chip_blue)
        // Không đọc được trạng thái hotspot khi thiếu quyền hệ thống -> luôn hiện kiểu tắt.
        chip(b.ccChipHotspot, false, R.drawable.cc_bg_chip_blue)
        b.ccTileRotation.isOn = state.isRotationLocked()
        b.ccTileSilent.isOn = state.isSilent()
        b.ccFocusDetail.setText(if (state.isDndOn()) R.string.cc_on else R.string.cc_off)
        b.ccSliderBrightness.setValue(state.brightness() / 255f)
        b.ccSliderVolume.setValue(state.volumeFraction())
        b.ccSliderBrightness.isLocked = editMode || !CcPermissions.canWriteSettings(context)
        tileAdapter.notifyDataSetChanged()
        connAdapter.states = mapOf(
            ConnectionAdapter.Item.AIRPLANE to airplane,
            ConnectionAdapter.Item.WIFI to wifi,
            ConnectionAdapter.Item.CELLULAR to cellular,
            ConnectionAdapter.Item.BLUETOOTH to bt,
            ConnectionAdapter.Item.SYNC to sync
        )
        renderProgress()
    }

    private fun chip(v: View, on: Boolean, onBg: Int) {
        val onAccent = if (on) accent else null
        if (onAccent != null) v.background = onAccent.drawable() else v.setBackgroundResource(if (on) onBg else R.drawable.cc_bg_chip_off)
        (v as? ImageView)?.imageTintList = ColorStateList.valueOf(onAccent?.iconColor ?: 0xFFFFFFFF.toInt())
    }

    private fun tap(v: View) = CcHaptics.tap(v)

    private fun open(action: (Context) -> Unit) {
        CcHaptics.tap(pager)
        host.closePanel()
        action(context)
    }

    private fun withMedia(action: () -> Unit) {
        if (!CcPermissions.isNotificationListenerEnabled(context)) {
            askPermission(CcPermissions.notificationListenerSettings(), R.string.cc_need_notification)
            return
        }
        action()
    }

    private fun askPermission(intent: android.content.Intent, message: Int) {
        host.closePanel()
        if (message != 0) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        CcActions.start(context, intent)
    }

    // ===== Adapter 3 trang (view tạo sẵn, chỉ gắn vào khung) =====

    private inner class PagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = 3
        override fun getItemViewType(position: Int) = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val frame = CcPageFrame(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                val d = resources.displayMetrics.density
                // Trang chính bám dưới nút cài đặt như Figma (y = 114dp); 2 trang kia canh giữa.
                topOffset = if (viewType == PAGE_MAIN) (pageTopOffsetDp * d).toInt() else 0
                bottomReserve = (24 * d).toInt()
            }
            val content: View = when (viewType) {
                PAGE_MAIN -> mainBinding.root
                PAGE_MUSIC -> musicBinding.root
                else -> connList
            }
            (content.parent as? ViewGroup)?.removeView(content)
            // Nội dung nuốt chạm để chạm vào vùng trống của khung mới tính là "đóng panel".
            content.isClickable = true
            frame.addView(content, ViewGroup.LayoutParams(
                parent.resources.getDimensionPixelSize(R.dimen.cc_panel_width), ViewGroup.LayoutParams.WRAP_CONTENT))
            frame.setOnClickListener { if (!editMode) host.closePanel() }
            return object : RecyclerView.ViewHolder(frame) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
    }

    companion object {
        const val PAGE_MAIN = 0
        const val PAGE_MUSIC = 1
        const val PAGE_CONNECTIVITY = 2
        private const val DRAG_SCALE = 1.12f
    }
}
