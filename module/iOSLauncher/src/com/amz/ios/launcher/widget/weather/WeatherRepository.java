package com.amz.ios.launcher.widget.weather;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

import com.amz.ios.launcher.LauncherModel;
import com.amz.ios.launcher.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/**
 * Nguồn dữ liệu thời tiết cho 3 widget Weather (2x2 / 4x2 / 4x4).
 *
 * API: Open-Meteo (api.open-meteo.com) — miễn phí hoàn toàn, KHÔNG cần API key nên không phải quản
 * lý key và không lo lộ key trong APK. Giới hạn 10.000 lượt/ngày cho dùng phi thương mại, xa hơn
 * nhiều so với nhu cầu của một launcher.
 *
 * CƠ CHẾ ĐỌC/GHI: widget được vẽ ĐỒNG BỘ trên main thread nên không thể chờ mạng. Vì vậy:
 *   - Widget luôn đọc từ CACHE (SharedPreferences) -> hiện ngay, không bao giờ trống.
 *   - Nếu cache hết hạn ({@link #CACHE_TTL_MS}), kích hoạt một lượt gọi mạng NỀN; lần vẽ sau sẽ có
 *     dữ liệu mới. Không chặn UI lúc nào.
 *
 * Không thêm phụ thuộc nào: HttpURLConnection, org.json, LocationManager, Geocoder đều là API
 * framework có sẵn.
 */
public final class WeatherRepository {

    /** Cache còn hạn thì không gọi mạng. 30 phút — thời tiết không đổi nhanh hơn thế. */
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;

    private static final String PREF_TEMP = "weather_temp";
    private static final String PREF_HIGH = "weather_high";
    private static final String PREF_LOW = "weather_low";
    private static final String PREF_CODE = "weather_code";
    private static final String PREF_CITY = "weather_city";
    private static final String PREF_AT = "weather_fetched_at";

    /** Chặn nhiều lượt gọi mạng chồng nhau khi nhiều widget cùng vẽ lại một lúc. */
    private static volatile boolean sFetching;

    /** Handler trên worker thread của launcher; tạo một lần rồi dùng lại. */
    private static android.os.Handler sWorkerHandler;

    private WeatherRepository() {}

    /** Dữ liệu hiển thị của widget. Mọi trường đều có sẵn giá trị mặc định, không bao giờ null. */
    public static final class WeatherData {
        public final int temp;
        public final int high;
        public final int low;
        public final int code;
        public final String city;
        /** false = chưa từng lấy được dữ liệu thật -> widget nên giữ nội dung mẫu. */
        public final boolean valid;

        WeatherData(int temp, int high, int low, int code, String city, boolean valid) {
            this.temp = temp;
            this.high = high;
            this.low = low;
            this.code = code;
            this.city = city;
            this.valid = valid;
        }
    }

    // ===================== ĐỌC (main thread, chỉ chạm cache) =====================

    /** Dữ liệu để vẽ widget ngay lúc này. Chưa có gì thì {@code valid == false}. */
    public static WeatherData cached(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long at = prefs.getLong(PREF_AT, 0L);
        if (at == 0L) {
            return new WeatherData(0, 0, 0, -1, "", false);
        }
        return new WeatherData(
                prefs.getInt(PREF_TEMP, 0),
                prefs.getInt(PREF_HIGH, 0),
                prefs.getInt(PREF_LOW, 0),
                prefs.getInt(PREF_CODE, -1),
                prefs.getString(PREF_CITY, ""),
                true);
    }

    /**
     * Gọi mạng lấy dữ liệu mới nếu cache đã hết hạn. Trả về ngay, việc lấy chạy ở luồng nền.
     *
     * @param force bỏ qua TTL (dùng khi người dùng vừa cấp quyền vị trí — phải cập nhật ngay).
     */
    public static void refreshIfStale(final Context context, boolean force) {
        if (!hasLocationPermission(context)) return;
        if (sFetching) return;

        long at = PreferenceManager.getDefaultSharedPreferences(context).getLong(PREF_AT, 0L);
        if (!force && at != 0L && System.currentTimeMillis() - at < CACHE_TTL_MS) {
            return;   // cache còn tươi
        }

        sFetching = true;
        final Context appContext = context.getApplicationContext();
        // Dùng worker thread sẵn có của launcher thay vì tạo thread mới. sWorker là package-private
        // nên đi qua getWorkerLooper() — API công khai duy nhất, không phải sửa LauncherModel.
        if (sWorkerHandler == null) {
            sWorkerHandler = new android.os.Handler(LauncherModel.getWorkerLooper());
        }
        sWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                try {
                    fetchAndStore(appContext);
                    ok = true;
                } catch (Throwable ignored) {
                    // Mất mạng / API lỗi / parse hỏng -> giữ nguyên cache cũ, widget vẫn hiện được.
                } finally {
                    sFetching = false;
                }
                if (ok) {
                    notifyUpdated();
                }
            }
        });
    }

    /**
     * Báo cho widget vẽ lại sau khi có dữ liệu mới.
     *
     * CẦN THIẾT vì gọi mạng là bất đồng bộ: lúc widget vẽ thì cache còn rỗng/cũ, dữ liệu về sau đó
     * vài trăm ms. Không có bước này thì phải chờ tới lần onResume kế tiếp mới thấy — vừa cấp quyền
     * xong sẽ tưởng widget hỏng.
     */
    private static void notifyUpdated() {
        final Runnable listener = sUpdateListener;
        if (listener == null) return;
        sMainHandler.post(listener);
    }

    /**
     * Đăng ký hàm được gọi (trên main thread) mỗi khi lấy được dữ liệu mới.
     * Launcher đăng ký một lần lúc khởi tạo để vẽ lại widget thời tiết.
     */
    public static void setUpdateListener(Runnable listener) {
        sUpdateListener = listener;
    }

    private static volatile Runnable sUpdateListener;
    private static final android.os.Handler sMainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    // ===================== LẤY DỮ LIỆU (luồng nền) =====================

    private static void fetchAndStore(Context context) throws Exception {
        Location location = lastKnownLocation(context);
        if (location == null) return;   // chưa có vị trí nào -> để lần sau

        double lat = location.getLatitude();
        double lon = location.getLongitude();

        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,weather_code"
                        + "&daily=temperature_2m_max,temperature_2m_min"
                        + "&timezone=auto&forecast_days=1",
                lat, lon);

        JSONObject json = new JSONObject(httpGet(url));
        JSONObject current = json.getJSONObject("current");
        JSONObject daily = json.getJSONObject("daily");
        JSONArray maxArray = daily.getJSONArray("temperature_2m_max");
        JSONArray minArray = daily.getJSONArray("temperature_2m_min");

        int temp = (int) Math.round(current.getDouble("temperature_2m"));
        int code = current.getInt("weather_code");
        int high = maxArray.length() > 0 ? (int) Math.round(maxArray.getDouble(0)) : temp;
        int low = minArray.length() > 0 ? (int) Math.round(minArray.getDouble(0)) : temp;

        String city = resolveCityName(context, lat, lon);

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(PREF_TEMP, temp)
                .putInt(PREF_HIGH, high)
                .putInt(PREF_LOW, low)
                .putInt(PREF_CODE, code)
                .putString(PREF_CITY, city)
                .putLong(PREF_AT, System.currentTimeMillis())
                .apply();
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Throwable ignored) { }
            }
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Vị trí gần nhất mà hệ thống đã biết.
     *
     * Dùng getLastKnownLocation thay vì requestLocationUpdates: thời tiết không cần vị trí realtime,
     * và đăng ký cập nhật liên tục sẽ hao pin. Nhược điểm là máy vừa khởi động lại có thể chưa có
     * vị trí nào — khi đó bỏ qua, lần refresh sau sẽ có.
     */
    private static Location lastKnownLocation(Context context) {
        if (!hasLocationPermission(context)) return null;
        try {
            LocationManager manager =
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (manager == null) return null;
            // NETWORK trước: nhanh và đủ chính xác cho thời tiết; GPS làm phương án dự phòng.
            Location location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
                location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            return location;
        } catch (Throwable ignored) {
            return null;   // SecurityException nếu quyền bị thu hồi giữa chừng
        }
    }

    /**
     * Tên thành phố từ toạ độ. Geocoder có thể không khả dụng -> trả chuỗi rỗng, widget giữ tên cũ.
     *
     * Dùng bản getFromLocation ĐỒNG BỘ (deprecated từ API 33 nhưng vẫn chạy). Bản callback mới chỉ
     * có từ API 33 mà minSdk dự án là 26; hàm này vốn đã chạy trên worker thread nên gọi đồng bộ là
     * đúng cách, không chặn UI.
     */
    @SuppressWarnings("deprecation")
    private static String resolveCityName(Context context, double lat, double lon) {
        try {
            if (!Geocoder.isPresent()) return "";
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses == null || addresses.isEmpty()) return "";
            Address address = addresses.get(0);
            // Ưu tiên tên thành phố; một số nơi chỉ có subAdminArea hoặc adminArea.
            if (address.getLocality() != null) return address.getLocality();
            if (address.getSubAdminArea() != null) return address.getSubAdminArea();
            if (address.getAdminArea() != null) return address.getAdminArea();
            return "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    // ===================== QUYỀN =====================

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ===================== MÃ THỜI TIẾT (WMO) =====================

    /**
     * Icon cho mã thời tiết WMO.
     *
     * Dự án mới có 2 icon (ic_weather_sun, ic_weather_cloud) nên map thô về 2 nhóm. Phần chữ mô tả
     * ({@link #conditionText}) vẫn phân biệt đủ ~20 mã. Bổ sung icon chi tiết sau thì chỉ sửa hàm
     * này, không phải đụng chỗ nào khác.
     */
    public static int conditionIcon(int code) {
        // 0 = trời quang, 1 = ít mây.
        return (code == 0 || code == 1) ? R.drawable.ic_weather_sun : R.drawable.ic_weather_cloud;
    }

    /** Mô tả thời tiết theo mã WMO. Trả về resource id của chuỗi. */
    public static int conditionTextRes(int code) {
        switch (code) {
            case 0:  return R.string.weather_code_clear;
            case 1:
            case 2:  return R.string.weather_code_partly_cloudy;
            case 3:  return R.string.weather_code_overcast;
            case 45:
            case 48: return R.string.weather_code_fog;
            case 51:
            case 53:
            case 55:
            case 56:
            case 57: return R.string.weather_code_drizzle;
            case 61:
            case 63:
            case 66:
            case 67:
            case 80:
            case 81: return R.string.weather_code_rain;
            case 65:
            case 82: return R.string.weather_code_heavy_rain;
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86: return R.string.weather_code_snow;
            case 95:
            case 96:
            case 99: return R.string.weather_code_thunderstorm;
            default: return R.string.weather_code_clear;
        }
    }

    /** Chuỗi mô tả thời tiết đã dịch. */
    public static String conditionText(Context context, int code) {
        return context.getString(conditionTextRes(code));
    }

    /** Định dạng nhiệt độ kiểu iOS: "28°". */
    public static String formatTemp(int value) {
        return value + "°";
    }

    /** Định dạng dải cao/thấp kiểu iOS: "H:32°  L:26°". */
    public static String formatHighLow(int high, int low) {
        return "H:" + high + "°  L:" + low + "°";
    }
}
