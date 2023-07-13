package com.gelios.configurator.ui.sensor.fuel.fragments.tarirovka

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import com.gelios.configurator.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_chart.*

class ChartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        val array = intent.extras?.getParcelableArray("data")
        configChart(array)
    }

    private fun configChart(array: Array<Parcelable>?) {
        val charData = mutableListOf<Entry>()
        for (item in array!!) {
            val it = item as DataTarirovka
            charData.add(Entry(it.sensorLevel.toFloat(), it.fuelLevel.toFloat()))
        }
        val line = LineDataSet(charData, intent.extras?.getString("name"))
        line.lineWidth = 1f
        line.circleRadius = 3f
        line.color = Color.BLACK
        line.setCircleColor(Color.BLACK)
        line.setDrawCircleHole(false)
        val lineData = LineData(line)
        chart.data = lineData
    }
}
