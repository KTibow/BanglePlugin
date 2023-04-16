package io.github.ktibow.bangleplugin.communicate

import android.app.Application
import com.juul.kable.Peripheral

object BanglePluginApp : Application() {
  var watchConnection: Peripheral? = null
  var batchSize = 12
  var batch = arrayOf<Float>()
}
