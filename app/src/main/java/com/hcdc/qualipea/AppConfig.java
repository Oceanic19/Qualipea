package com.hcdc.qualipea;

/**
 * Central config for talking to the QualiPea Raspberry Pi box.
 *
 * The Pi has two network interfaces now: eth0 (wired to the laptop,
 * used for SSH/terminal access) and wlan0 (connected to the phone's
 * hotspot, used for the app). This IP is the wlan0 one.
 *
 * TODO: this is a DHCP-assigned address from the phone's hotspot, so
 * it may change if the hotspot restarts or a different phone is used.
 * Longer term, make this editable from the Settings screen instead of
 * hardcoded, or give the Pi a static IP / reserve it in the hotspot.
 */
public final class AppConfig {
    public static final String PI_HOST = "10.42.0.1";
    public static final int PI_PORT = 5000;

    public static final String PI_BASE_URL = "http://" + PI_HOST + ":" + PI_PORT;
    public static final String SCAN_ENDPOINT = PI_BASE_URL + "/scan";
    public static final String VIDEO_STREAM_URL = PI_BASE_URL + "/video";
    public static final String RESULT_IMAGE_URL = PI_BASE_URL + "/result";

    private AppConfig() {}
}
