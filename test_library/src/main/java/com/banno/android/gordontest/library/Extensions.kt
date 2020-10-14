package com.banno.android.gordontest.library

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.appcompat.app.AppCompatActivity

internal fun AppCompatActivity.isInPortrait() {
    resources.configuration.orientation == ORIENTATION_PORTRAIT
}
