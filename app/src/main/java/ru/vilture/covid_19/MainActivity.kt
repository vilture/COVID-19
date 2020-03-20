package ru.vilture.covid_19

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.IOException
import java.net.URL

class MainActivity : AppCompatActivity() {

    var apiRspCountries = ""
    var apiRspAll = ""
    val moshi = Moshi.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var ok = false

        // check internet state and load json data
        doAsync {
            Snackbar.make(main, "Попытка загрузить статистику", Snackbar.LENGTH_INDEFINITE)
                .show()

            while (!ok) {
                ok = checkAndSaveData()
            }
            Snackbar.make(main, "Статистика загружена", Snackbar.LENGTH_SHORT).show()
        }

        // разбивем данные на json объекты

    }

    private fun checkAndSaveData(): Boolean {
        if (isOnline()) {
            // internet available
            doAsync {
                apiRspCountries = URL("https://coronavirus-19-api.herokuapp.com/countries").readText()
                if (apiRspCountries.isNotEmpty()) {
                    File(this@MainActivity.filesDir, "countries.json").writeText(apiRspCountries)
                }

                apiRspAll = URL("https://coronavirus-19-api.herokuapp.com/all").readText()
                if (apiRspAll.isNotEmpty()) {
                    File(this@MainActivity.filesDir, "all.json").writeText(apiRspAll)
                }

            }

        } else {

            if (File(this@MainActivity.filesDir, "all.json").exists()) {
                apiRspAll = File(this@MainActivity.filesDir, "all.json").readText()
            } else {
                return false
            }

            if (File(this@MainActivity.filesDir, "countries.json").exists()) {
                apiRspCountries = File(this@MainActivity.filesDir, "countries.json").readText()
            } else {
                return false
            }
        }
        return true
    }

    private fun isOnline(): Boolean {
        val runtime = Runtime.getRuntime()
        try {
            val ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
            val exitValue = ipProcess.waitFor()
            return exitValue == 0
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return false
    }
}
