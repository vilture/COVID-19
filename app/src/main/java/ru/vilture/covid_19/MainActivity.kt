package ru.vilture.covid_19

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.IOException
import java.math.RoundingMode
import java.net.URL


class MainActivity : AppCompatActivity() {

    var apiRspCountries = ""
    var apiRspAll = ""

    data class dataAll(
        @SerializedName("cases") val cases: String,
        @SerializedName("deaths") val deaths: String,
        @SerializedName("recovered") val recovered: String
    )

    data class dataCountry(
        @SerializedName("country") val country: String,
        @SerializedName("cases") val cases: String,
        @SerializedName("todayCases") val todayCases: String,
        @SerializedName("deaths") val deaths: String,
        @SerializedName("todayDeaths") val todayDeaths: String,
        @SerializedName("recovered") val recovered: String,
        @SerializedName("active") val active: String,
        @SerializedName("critical") val critical: String,
        @SerializedName("casesPerOneMillion") val casesPerOneMillion: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var ok = false

        // check internet state and load json data
        Snackbar.make(main, "Попытка загрузить статистику", Snackbar.LENGTH_INDEFINITE)
            .show()

        while (!ok) {
            ok = checkAndSaveData()
        }

        viewDataAll()
        Snackbar.make(main, "Статистика загружена", Snackbar.LENGTH_SHORT).show()
    }


    private fun viewDataAll() {
        // разбивем мировые данные на json объекты
        val dataAll = parseJsonAll()

        // покажем мировую статистику
        val dpc: Float
        country.text = "мировая статистика"
        cases.text = dataAll.cases
        death.text = dataAll.deaths
        recover.text = dataAll.recovered
        dpc = (dataAll.cases).toFloat() / (dataAll.deaths).toFloat()

        deathPerCases.text = dpc.toBigDecimal().setScale(2, RoundingMode.UP).toPlainString()
    }

    private fun parseJsonAll(): dataAll {
        return try {
            val gson = GsonBuilder().serializeNulls().create()
            gson.fromJson(apiRspAll,dataAll::class.java)
        } catch (e: Exception) {
            println(e.message.toString())
            dataAll("-1", "-1", "-1")
        }
    }

    private fun checkAndSaveData(): Boolean {
        doAsync {
            if (isOnline()) {
                // internet available
                apiRspCountries =
                    URL("https://coronavirus-19-api.herokuapp.com/countries").readText()
                if (apiRspCountries.isNotEmpty()) {
                    File(this@MainActivity.filesDir, "countries.json").writeText(apiRspCountries)
                }

                apiRspAll = URL("https://coronavirus-19-api.herokuapp.com/all").readText()
                if (apiRspAll.isNotEmpty()) {
                    File(this@MainActivity.filesDir, "all.json").writeText(apiRspAll)
                }
            } else {

                if (File(this@MainActivity.filesDir, "all.json").exists()) {
                    apiRspAll = File(this@MainActivity.filesDir, "all.json").readText()
                }

                if (File(this@MainActivity.filesDir, "countries.json").exists()) {
                    apiRspCountries = File(this@MainActivity.filesDir, "countries.json").readText()
                }
            }
        }

        return !(apiRspAll.isEmpty() and apiRspCountries.isEmpty())
    }

    private fun isOnline(): Boolean {
//        val runtime = Runtime.getRuntime()
//        try {
//            val ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
//            val exitValue = ipProcess.waitFor()
//            return exitValue == 0
//        } catch (e: IOException) {
//            e.printStackTrace()
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//        return false

        try {
            val cm =
                this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            return netInfo != null && netInfo.isConnectedOrConnecting
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }


}
