package com.example.data.remote

enum class WebDavService(val title: String, val initialAddress: String, val hint: String) {
    NUTSTORE("坚果云", "https://dav.jianguoyun.com/dav/", "在坚果云「第三方应用管理」生成应用密码。"),
    NEXTCLOUD("Nextcloud", "https://", "粘贴文件设置里的 WebDAV 地址，用应用密码登录。"),
    SYNOLOGY("群晖 NAS", "https://", "先在 NAS 上开启 WebDAV Server。"),
    CUSTOM("自定义", "https://", "填写完整的 WebDAV 目录地址。");

    companion object {
        fun fromAddress(address: String): WebDavService = when {
            address.startsWith("https://dav.jianguoyun.com/dav/") -> NUTSTORE
            address.contains("/remote.php/dav/files/") -> NEXTCLOUD
            else -> CUSTOM
        }
    }
}
