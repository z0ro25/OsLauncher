package com.ezla.oslauncher.compass

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.ezla.oslauncher.appbase.base.BaseActivity
import com.ezla.oslauncher.compass.databinding.CompassActivityCompassBinding
import com.ezla.oslauncher.compass.databinding.CompassSheetLocationBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** La bàn kiểu iOS (Figma "13 · Compass"): Heading, Calibrate, xin quyền vị trí, sheet My Location. */
class CompassActivity : BaseActivity<CompassActivityCompassBinding>(), CompassSensor.Listener {

    override val setViewBinding get() = CompassActivityCompassBinding.inflate(layoutInflater)

    // Toạ độ layout lấy thẳng từ frame Figma (đã gồm status bar 48) nên không để base pad thêm.
    override val padStatusBar = false

    private val b get() = binding
    private lateinit var sensor: CompassSensor
    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val geocodeExecutor = Executors.newSingleThreadExecutor()

    private var smoothed = 0f
    private var hasHeading = false
    private var declination = 0f
    private var location: Location? = null
    private var placeName: String? = null
    private var geocodedAt: Location? = null
    private var calibrationDone = false
    private var sheet: BottomSheetDialog? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) startLocation() else renderLocation()
    }

    private val locationListener = LocationListener { onLocation(it) }

    // Mọi đường vào (icon desktop, CompassEntry) đều mở thẳng màn này: lần đầu chuyển sang onboarding.
    override fun redirectBeforeCreate(): Boolean {
        if (CompassEntry.isOnboardingDone(this)) return false
        startActivity(Intent(this, CompassOnboardingActivity::class.java))
        return true
    }

    override fun initView() {
        sensor = CompassSensor(this, this)
        b.compassDial.cardinals = resources.getStringArray(R.array.compass_cardinals)
        b.compassRing.onProgress = { p ->
            b.compassTvPercent.text = getString(R.string.compass_percent, p)
            if (p >= 100) finishCalibration()
        }
        if (!sensor.isAvailable) b.compassTvHeading.text = getString(R.string.compass_no_sensor)
        renderHeading()
        renderLocation()
    }

    override fun viewListener() {
        b.compassBtnAllowWhileUsing.setOnClickListener { requestLocationPermission() }
        b.compassBtnAllowOnce.setOnClickListener { requestLocationPermission() }
        b.compassBtnDontAllow.setOnClickListener {
            prefs().edit().putBoolean(KEY_DENIED, true).apply()
            b.compassPermissionOverlay.visibility = View.GONE
        }
        b.compassPermissionOverlay.setOnClickListener { /* nuốt chạm xuống la bàn */ }
        b.compassCalibrateOverlay.setOnClickListener { finishCalibration() }
        b.compassInfo.setOnClickListener { onInfoClick() }
    }

    override fun dataObservable() {}

    override fun onStart() {
        super.onStart()
        sensor.start()
        if (hasLocationPermission()) {
            b.compassPermissionOverlay.visibility = View.GONE
            startLocation()
        } else if (!prefs().getBoolean(KEY_DENIED, false)) {
            b.compassPermissionOverlay.visibility = View.VISIBLE
        }
    }

    override fun onStop() {
        sensor.stop()
        locationManager.removeUpdates(locationListener)
        super.onStop()
    }

    override fun onDestroy() {
        sheet?.dismiss()
        geocodeExecutor.shutdownNow()
        super.onDestroy()
    }

    // ---- Sensor ----

    override fun onAzimuth(degrees: Float) {
        val trueHeading = HeadingMath.normalize(degrees + declination + displayRotationOffset())
        smoothed = if (hasHeading) HeadingMath.smooth(smoothed, trueHeading, SMOOTHING) else trueHeading
        hasHeading = true
        renderHeading()
    }

    override fun onGravity(x: Float, y: Float, z: Float) {
        val g = sqrt(x * x + y * y + z * z).takeIf { it > 0f } ?: return
        b.compassDial.setLevel(x / g, -y / g)
        if (b.compassCalibrateOverlay.visibility == View.VISIBLE) {
            // Gia tốc kế đo hướng "lên" -> hướng xuống trên màn = (-x, +y); bi lăn về mép thấp nhất.
            b.compassRing.moveBall(Math.toDegrees(atan2(-x, -y).toDouble()).toFloat())
        }
    }

    override fun onAccuracy(needsCalibration: Boolean) {
        if (needsCalibration && !calibrationDone && b.compassPermissionOverlay.visibility != View.VISIBLE) {
            if (b.compassCalibrateOverlay.visibility != View.VISIBLE) {
                b.compassRing.reset()
                b.compassCalibrateOverlay.visibility = View.VISIBLE
            }
        } else if (!needsCalibration) {
            finishCalibration()
        }
    }

    private fun finishCalibration() {
        if (b.compassCalibrateOverlay.visibility != View.VISIBLE) return
        calibrationDone = true
        b.compassCalibrateOverlay.visibility = View.GONE
    }

    @Suppress("DEPRECATION")
    private fun displayRotationOffset(): Float {
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.rotation ?: 0
        else windowManager.defaultDisplay.rotation
        return rotation * 90f
    }

    private fun renderHeading() {
        b.compassDial.heading = smoothed
        if (!sensor.isAvailable) return
        val dirs = resources.getStringArray(R.array.compass_directions)
        b.compassTvHeading.text = getString(
            R.string.compass_heading_format,
            HeadingMath.displayDegrees(smoothed), dirs[HeadingMath.directionIndex(smoothed)]
        )
        sheet?.findViewById<android.widget.TextView>(R.id.compass_sheet_subtitle)?.text = sheetSubtitle()
    }

    // ---- Location ----

    private fun prefs() = getSharedPreferences(PREFS, MODE_PRIVATE)

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        b.compassPermissionOverlay.visibility = View.GONE
        prefs().edit().putBoolean(KEY_DENIED, false).apply()
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun onInfoClick() {
        when {
            location != null -> showLocationSheet()
            hasLocationPermission() -> Unit
            // Đã chọn "Don't Allow" ở dialog hệ thống 2 lần: Android không hỏi nữa -> mở App info.
            prefs().getBoolean(KEY_DENIED, false) && !shouldShowRationale() &&
                prefs().getBoolean(KEY_ASKED, false) -> openAppSettings()
            else -> b.compassPermissionOverlay.visibility = View.VISIBLE
        }
    }

    private fun shouldShowRationale() =
        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun openAppSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        if (!hasLocationPermission()) return
        prefs().edit().putBoolean(KEY_ASKED, true).apply()
        val providers = locationManager.getProviders(true)
            .filter { it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER }
        providers.mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }?.let { onLocation(it) }
        providers.forEach {
            runCatching {
                locationManager.requestLocationUpdates(it, UPDATE_MS, UPDATE_METERS, locationListener, Looper.getMainLooper())
            }
        }
        renderLocation()
    }

    private fun onLocation(loc: Location) {
        val prev = location
        // Bản tin network kém chính xác hơn không được đè GPS vừa có.
        if (prev != null && loc.accuracy > prev.accuracy * 2 && loc.time - prev.time < STALE_MS) return
        location = loc
        declination = GeomagneticField(
            loc.latitude.toFloat(), loc.longitude.toFloat(), loc.altitude.toFloat(), System.currentTimeMillis()
        ).declination
        val last = geocodedAt
        if (last == null || last.distanceTo(loc) > GEOCODE_METERS) {
            geocodedAt = loc
            geocode(loc)
        }
        renderLocation()
    }

    private fun geocode(loc: Location) {
        if (!Geocoder.isPresent()) return
        geocodeExecutor.execute {
            val address = runCatching {
                @Suppress("DEPRECATION")
                Geocoder(this, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
            }.getOrNull()
            val name = address?.let { formatPlace(it) }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                placeName = name
                renderLocation()
            }
        }
    }

    // "Hoàn Kiếm, Hà Nội": quận/huyện + tỉnh/thành, bỏ phần trùng hoặc rỗng.
    private fun formatPlace(a: Address): String? {
        val parts = listOf(a.subAdminArea ?: a.locality ?: a.subLocality, a.adminArea)
            .filterNotNull().filter { it.isNotBlank() }.distinct()
        return parts.joinToString(", ").ifBlank { null }
    }

    private fun renderLocation() {
        val loc = location
        if (loc == null) {
            b.compassTvCoordinates.text = getString(R.string.compass_dash)
            b.compassTvPlace.text = getString(R.string.compass_location_unavailable)
            b.compassTvElevation.visibility = View.GONE
            return
        }
        val hemi = resources.getStringArray(R.array.compass_cardinals)
        b.compassTvCoordinates.text = HeadingMath.formatCoordinate(loc.latitude, loc.longitude) { hemi[it] }
        b.compassTvPlace.text = placeName ?: getString(R.string.compass_dash)
        if (loc.hasAltitude()) {
            b.compassTvElevation.visibility = View.VISIBLE
            b.compassTvElevation.text = getString(R.string.compass_elevation, loc.altitude.roundToInt())
        } else {
            b.compassTvElevation.visibility = View.GONE
        }
    }

    // ---- Sheet My Location (Figma "Compass › Open in Maps") ----

    private fun sheetSubtitle(): String {
        val heading = b.compassTvHeading.text.toString()
        val place = placeName ?: location?.let { HeadingMath.formatCoordinate(it.latitude, it.longitude) { i ->
            resources.getStringArray(R.array.compass_cardinals)[i] } } ?: ""
        return getString(R.string.compass_place_heading, place, heading)
    }

    private fun showLocationSheet() {
        val loc = location ?: return
        if (sheet?.isShowing == true) return
        val sb = CompassSheetLocationBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, R.style.CompassBottomSheetDialog)
        dialog.setContentView(sb.root)
        sb.compassSheetSubtitle.text = sheetSubtitle()
        sb.compassSheetClose.setOnClickListener { dialog.dismiss() }
        sb.compassSheetHeader.setOnClickListener { openInMaps(loc) }
        sb.compassSheetShare.setOnClickListener { shareLocation(loc) }
        dialog.setOnDismissListener { sheet = null }
        sheet = dialog
        dialog.show()
    }

    private fun mapsUrl(loc: Location) =
        String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", loc.latitude, loc.longitude)

    private fun openInMaps(loc: Location) {
        val label = Uri.encode(getString(R.string.compass_my_location))
        val geo = Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
            loc.latitude, loc.longitude, loc.latitude, loc.longitude, label))
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, geo)) }
            .onFailure { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl(loc)))) } }
    }

    private fun shareLocation(loc: Location) {
        val text = getString(R.string.compass_share_text, b.compassTvCoordinates.text, mapsUrl(loc))
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        runCatching { startActivity(Intent.createChooser(send, getString(R.string.compass_share_location))) }
    }

    companion object {
        private const val PREFS = "compass_prefs"
        private const val KEY_DENIED = "location_denied"
        private const val KEY_ASKED = "location_asked"
        private const val SMOOTHING = 0.2f
        private const val UPDATE_MS = 2000L
        private const val UPDATE_METERS = 2f
        private const val STALE_MS = 30_000L
        private const val GEOCODE_METERS = 200f
    }
}
