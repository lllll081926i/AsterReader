package com.example

import org.robolectric.annotation.Config

/** 最低支持系统复用相同的布局与交互契约。 */
@Config(sdk = [31], qualifiers = "w411dp-h891dp-xhdpi")
class SettingsApi31ConsistencyTest : SettingsConsistencyTest()
