package ru.vilture.covid_19

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
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


class MainActivity : AppCompatActivity() {

    var apiRspCountry = ""
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

    var responceCountry: List<dataCountry> = listOf()

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

        //Thread.sleep(200)

        //заполним список данных по странам
        val collectionType =
            object : TypeToken<List<dataCountry?>?>() {}.type

        responceCountry = Gson().fromJson(
            apiRspCountry,
            collectionType
        ) as List<dataCountry>

        // заполним список стран
        collectCountry()


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

        Snackbar.make(main, "Статистика загружена", Snackbar.LENGTH_SHORT).show()
    }

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
            dpc = (dataAll.cases).toFloat() / (dataAll.deaths).toFloat()

            deathPerCases.text = dpc.toBigDecimal().setScale(2, RoundingMode.UP).toPlainString()
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

    private fun parseJsonAll(): dataAll {
        return try {
            val gson = GsonBuilder().serializeNulls().create()
            gson.fromJson(apiRspAll, dataAll::class.java)
        } catch (e: Exception) {
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_LONG).show()
            dataAll("-1", "-1", "-1")
        }
    }

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


}
