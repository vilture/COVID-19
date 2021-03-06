package ru.vilture.covid_19

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.IOException
import java.math.RoundingMode
import java.net.URL
import java.sql.Date
import java.text.SimpleDateFormat
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private var apiRspCountry = ""
    private var apiRspAll = ""
    private var loadDate = ""

    data class DataAll(
        @SerializedName("cases") val cases: String,
        @SerializedName("deaths") val deaths: String,
        @SerializedName("recovered") val recovered: String
    )

    data class DataCountry(
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

    private var responceCountry: List<DataCountry> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // проверим доступность интернета и загрузим данные
        Snackbar.make(main, "Попытка загрузить статистику", Snackbar.LENGTH_INDEFINITE)
            .show()

        // собираем все данные
        getData()

        // отобразим данные страны в зависимости от подброса
        select_country.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selCountry = select_country.selectedItem.toString()
                changeDataCountry(selCountry)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        Snackbar.make(main, "Статистика загружена $loadDate", Snackbar.LENGTH_SHORT).show()

        // свайп для обновления данных
        swipeRefreshLayout.setOnRefreshListener {
            getData()

            swipeRefreshLayout.isRefreshing = false
            Snackbar.make(main, "Статистика обновлена", Snackbar.LENGTH_SHORT).show()
        }


        // переход на карту регионов России
        rusBtn.setOnClickListener{
            val intent = Intent(this@MainActivity, Russia::class.java)
            startActivity(intent)
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun getData() {
        var ok = false

        while (!ok) {
            ok = checkAndSaveData()
        }

        Thread.sleep(150)

        //заполним список данных по странам
        val collectionType =
            object : TypeToken<List<DataCountry?>?>() {}.type

        responceCountry = Gson().fromJson(
            apiRspCountry,
            collectionType
        ) as List<DataCountry>

        rusBtn.visibility = View.GONE

        // заполним список стран
        collectCountry()

        // грузим карту
        webMap.settings.javaScriptEnabled = true
        webMap.settings.loadWithOverviewMode = true
        webMap.settings.useWideViewPort = true
        webMap.setBackgroundColor(Color.WHITE)
        webMap.clearCache(true)

        webMap.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                webMap.loadUrl(
                    "javascript:(function() { " +
                            "document.getElementById('header-wrapper').style.display='none'; " +
                            "document.getElementsByClassName('card_inner_title')[0].style.display='none'; " +
                            "document.getElementsByClassName('card card-a table_card')[0].style.display='none'; " +
                            "document.getElementsByClassName('card card-b ')[0].style.display='none'; " +
                            "document.getElementsByClassName('footer')[0].style.display='none'; " + "})()"
                )
            }
        }

        webMap.loadUrl("https://google.org/crisisresponse/covid19-map")
    }


    @SuppressLint("SetTextI18n")
    private fun changeDataCountry(selCountry: String) {

        if (selCountry == WTEXT) {
            // разбивем мировые данные на json объекты
            val dataAll = parseJsonAll()

            // покажем мировую статистику
            val dpc: Float
            country.text = WTEXT
            cases.text = dataAll.cases
            death.text = dataAll.deaths
            recover.text = dataAll.recovered
            newCases.text = "-"
            newDeath.text = "-"
            active.text = "-"
            crit.text = "-"
            dpc = (dataAll.cases).toFloat() / (dataAll.deaths).toFloat()

            deathPerCases.text = dpc.toBigDecimal().setScale(2, RoundingMode.UP).toPlainString()

            rusBtn.visibility = View.GONE
        } else {
            for (list in responceCountry) {
                if (list.country == selCountry) {
                    country.text = selCountry
                    cases.text = list.cases
                    newCases.text = list.todayCases
                    death.text = list.deaths
                    newDeath.text = list.todayDeaths
                    recover.text = list.recovered
                    active.text = list.active
                    crit.text = list.critical
                    val dpc: Float = (list.cases).toFloat() / (list.deaths).toFloat()
                    if (!dpc.isInfinite())
                        deathPerCases.text =
                            dpc.toBigDecimal().setScale(2, RoundingMode.UP).toPlainString()
                    else
                        deathPerCases.text = "0.00"

                    if (selCountry == "Russia")
                        rusBtn.visibility = View.VISIBLE
                    else
                        rusBtn.visibility = View.GONE

                    break
                }

            }
        }
    }

    private fun collectCountry() {
        try {

            val listCountry = arrayListOf<String>()
            for (list in responceCountry) {
                listCountry.add(list.country)
            }
            listCountry.add(0, WTEXT)

            val spinCountry = ArrayList(listCountry)

            val adapCountry = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item, spinCountry
            )
            select_country.adapter = adapCountry
        } catch (e: Exception) {
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private fun parseJsonAll(): DataAll {
        return try {
            val gson = GsonBuilder().serializeNulls().create()
            gson.fromJson(apiRspAll, DataAll::class.java)
        } catch (e: Exception) {
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_LONG).show()
            DataAll("-1", "-1", "-1")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun checkAndSaveData(): Boolean {
        doAsync {
            if (isOnline()) {
                // internet available
                apiRspCountry =
                    URL("https://coronavirus-19-api.herokuapp.com/countries").readText()
                if (apiRspCountry.isNotEmpty()) {
                    File(this@MainActivity.filesDir, "countries.json").writeText(apiRspCountry)
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
                    apiRspCountry = File(this@MainActivity.filesDir, "countries.json").readText()
                }

                val oldDate =
                    Date(File(this@MainActivity.filesDir, "countries.json").lastModified())
                loadDate = SimpleDateFormat("dd.MM.yyyy HH:mm").format(oldDate)
            }
        }
        return !(apiRspAll.isEmpty() and apiRspCountry.isEmpty())
    }

    private fun isOnline(): Boolean {
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

    companion object {
        const val WTEXT = "Мировая статистика"
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }

}
