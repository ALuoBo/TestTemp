package com.lifwear.testtemp

import android.app.Application
import android.bluetooth.BluetoothGatt
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.drake.brv.utils.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lifwear.App
import com.lifwear.bluetooth.*
import com.lifwear.bluetooth.BLECommService.LocalBinder
import com.lifwear.bluetooth.fr80x.FR80xDevice
import com.lifwear.bluetooth.fr80x.data.TestTemp
import com.lifwear.testtemp.databinding.ActivityMainBinding
import com.lifwear.testtemp.db.AppDB
import com.lifwear.testtemp.db.Temp
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity(), BtStateObserver, BLEReceiver {
    private val  TAG = "MainActivity111 "
    lateinit var binding: ActivityMainBinding
    private val bleManager: BLEManager = BLEManager.getInstance(App.getContext() as Application)

    private val pageViewModel by viewModels<PageViewModel>()
    val fR80xDevice = FR80xDevice()

    private val temps: MutableList<TempData> = arrayListOf()

    private lateinit var chart: LineChart

    private val tempDao = AppDB.getInstance().tempDao()

    /**
     * 所连接设备的 mac
     */
    private lateinit var macConnect: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ActivityMainBinding这个类根据布局文件名生成(id+Binding)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        // data binding 配合使用 livedata 时，要设置 binding.lifecycleOwner
        binding.lifecycleOwner = this
        binding.vm = pageViewModel
        binding.clicker = Clicker()

        binding.rvTempData.linear().setup {
            addType<TempData>(R.layout.item_temp)
        }

        chart = binding.tempChart
        initChart(chart)
        // setData(chart, 10)
        requestPermission()

        bindBLECommService()
        fR80xDevice.addDataObserver(this)

    }

    private fun setAimData(mLineChart: LineChart, entry: Entry) {
        val values: ArrayList<Entry>
        val set: LineDataSet?

        if (mLineChart.data != null && mLineChart.data.dataSetCount > 0) {
            set = mLineChart.data.getDataSetByIndex(0) as LineDataSet?
            values = set!!.values as ArrayList<Entry>
            values.add(entry)
            mLineChart.data.notifyDataChanged()
            mLineChart.notifyDataSetChanged()
            mLineChart.invalidate()
        } else {
            values = arrayListOf()
            values.add(entry)
            set = LineDataSet(values, "目标温度")
            /*
            使线条以虚线模式绘制，例如 “ - - - - - ”。
             如果硬件加速关闭，这个工作就可以了。
            请记住，硬件加速提高了性能。
            lineLength线段的长度
            spaceLength之间的空格长度
             */
            //set.enableDashedLine(10f, 5f, 0f)
            //set.enableDashedHighlightLine(10f, 5f, 0f)
            set.setCircleColor(Color.GREEN)
            set.color = Color.GREEN
            set.lineWidth = 1f
            //将其设置为true可在每个数据圈中绘制一个圆
            set.setDrawCircleHole(false)
            set.setDrawValues(false)
            set.circleRadius = 1f
            set.valueTextSize = 5f
            //是否填充，默认false
            set.setDrawFilled(true)

            // set.fillAlpha = 0
            set.fillColor = Color.GREEN

//            set.fillFormatter = IFillFormatter { _, _ -> // change the return value here to better understand the effect
//                // return 0;
//                chart.axisLeft.axisMaximum
//            }

            if (mLineChart.data == null) {
                mLineChart.data = LineData(set)
            } else {
                mLineChart.data.addDataSet(set)
            }
            mLineChart.invalidate()
        }
    }

    private fun setEnData(mLineChart: LineChart, entry: Entry) {
        val values: ArrayList<Entry>
        val set: LineDataSet?

        if (mLineChart.data != null && mLineChart.data.dataSetCount > 1) {
            set = mLineChart.data.getDataSetByIndex(1) as LineDataSet?
            values = set!!.values as ArrayList<Entry>
            values.add(entry)
            mLineChart.data.notifyDataChanged()
            mLineChart.notifyDataSetChanged()
            mLineChart.invalidate()
        } else {
            values = arrayListOf()
            values.add(entry)
            set = LineDataSet(values, "环境温度")
            /*
            使线条以虚线模式绘制，例如 “ - - - - - ”。
             如果硬件加速关闭，这个工作就可以了。
            请记住，硬件加速提高了性能。
            lineLength线段的长度
            spaceLength之间的空格长度
             */
            set.enableDashedLine(10f, 5f, 0f)
            set.enableDashedHighlightLine(10f, 5f, 0f)
            set.setCircleColor(Color.CYAN)
            set.color = Color.CYAN
            set.lineWidth = 1f
            set.circleRadius = 1f
            //将其设置为true可在每个数据圈中绘制一个圆
            set.setDrawCircleHole(false)
            set.setDrawValues(false)
            set.valueTextSize = 5f
            //是否填充，默认false
            set.setDrawFilled(true)
            //set.fillAlpha = 0
            set.fillColor = Color.CYAN
//            set.fillFormatter = IFillFormatter { _, _ -> // change the return value here to better understand the effect
//                // return 0;
//                chart.axisLeft.axisMinimum
//            }
//
            if (mLineChart.data == null) {
                mLineChart.data = LineData(set)
            } else {
                mLineChart.data.addDataSet(set)
            }
            mLineChart.invalidate()

        }
    }


    private fun initChart(mLineChart: LineChart) {
        mLineChart.run {

            setNoDataText("")
            //设置背景颜色
            setBackgroundColor(Color.WHITE)
            //启用时，将渲染边框矩形。如果启用，则无法绘制x轴和y轴的轴线。
            setDrawBorders(false)

            //chart.setGridBackgroundColor(Color.parseColor("#48D1CC"))
            //绘制网格背景
            chart.setDrawGridBackground(false)
            // no description text
            description.isEnabled = false
            //设置背景图片
            background = Drawable.createFromPath("")

            isDragEnabled = true //是否可以拖动
            isScaleXEnabled = false// 是否可以缩放
            setTouchEnabled(true)//是否有触摸事件

//        val myMarkerView = MyMarkerView(this@LineChartActivity, R.layout.custom_marker_view)
//        myMarkerView.chartView=mLineChart
//        mLineChart!!.marker=myMarkerView

            //设置图表下面的描述
//            val description = Description()
//            description.text = "LineChart"
//            description.textColor = Color.RED
//            description.textSize = 20f
//            this.description = description
            //x轴
            mLineChart!!.defaultValueFormatter
            //x轴的位置在底部
            xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE

            axisRight.isEnabled = false
            //是否画x轴上的轴线
            xAxis.setDrawAxisLine(false)
            //是否画x轴上的网格线
            xAxis.setDrawGridLines(false)
            //是否绘制x轴上的标签(不会影响轴线和网格线)
            xAxis.setDrawLabels(true)
            //是否绘制x轴的标签(不会影响轴线和网格线)
            xAxis.setDrawLabels(true)
            //设置轴线的颜色
            xAxis.axisLineColor = Color.BLACK
            //轴线的宽度
            xAxis.axisLineWidth = 2f
            /*
            设置此轴的自定义最大值。 如果设置，则不会计算此值自动取决于
            提供的数据.使用resetAxisMaxValue（）来撤销此操作。
             */
            xAxis.axisMaximum = 1000f
            xAxis.mAxisMinimum = 0f

            //网格颜色
            xAxis.gridColor = Color.BLACK

            //左边的Y轴
            axisLeft.setDrawGridLines(true)
            //将其设置为true可绘制零线
            axisLeft.setDrawZeroLine(true)
            //是否画Y轴上的轴线
            axisLeft.setDrawAxisLine(false)
            //是否绘制Y轴的标签(不会影响轴线和网格线)
            axisLeft.setDrawLabels(true)
            //是否画x轴上的网格线
            axisLeft.setDrawGridLines(false)

            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 40f
            //xAxis.setLabelCount(20, false)
            setVisibleXRangeMaximum(100f)
            xAxis.granularity = 1f // 设置X轴坐标之间的最小间隔
            // 限制线
//            val ll1 = LimitLine(150f, "Upper Limit")
//            ll1.lineWidth = 4f
//            ll1.enableDashedLine(10f, 10f, 0f)
//            ll1.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
//            ll1.textSize = 10f
//            axisLeft.addLimitLine(ll1)
            //x轴动画
            animateX(500)

        }


    }

    private fun requestPermission() {
        XXPermissions.with(this)
            .permission(
                Permission.ACCESS_COARSE_LOCATION,
                Permission.ACCESS_FINE_LOCATION
            ).request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    Toast.makeText(this@MainActivity, "缺少定位权限，App无法正常工作", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        bleManager.EnableBT(this)
    }

    override fun stateChange(state: Int) {
        TODO("Not yet implemented")
    }

    override fun onConnectStateChanged(connectState: Boolean) {
        Log.d(TAG, "onConnectStateChanged $connectState")
        pageViewModel.connectStatus.postValue(connectState)

        if (connectState) {
            Log.d(TAG, "pageViewModel $connectState")
            pageViewModel.btText.postValue("断开连接")
        } else {
            pageViewModel.btText.postValue("连接")
            currentPointIndex = 0
        }

    }

    override fun onServiceDiscovered() {
        MainScope().launch(Dispatchers.IO) {
            delay(2000)
            fR80xDevice.setTimeStamp()
            delay(2000)
            sendLimit()
        }
    }

    var currentPointIndex = 0

    override fun onDataChanged(deviceData: DeviceData?) {

        if (deviceData is TestTemp) {

            // 增加保存数据库的操作
            MainScope().launch(Dispatchers.Default) {

                val temp = Temp(
                    0,
                    aimTemp = deviceData.aimTemp.toString(),
                    enTemp = deviceData.environmentTemp.toString(),
                    timestamp = deviceData.timestamp.toString(),
                    mac = macConnect
                )

                tempDao.insertTemp(temp)

            }


            MainScope().launch(Dispatchers.Main) {

                val tempDate = TempData(
                    deviceData.aimTemp.toString(),
                    deviceData.environmentTemp.toString(),
                    xStamp2Time(deviceData.timestamp.toString(), "MM-dd HH:mm")
                )

                Log.d(TAG, "tempDate $tempDate")
                temps.add(tempDate)

                binding.rvTempData.models = temps

                var aimTemp: Entry
                var enTemp: Entry
                withContext(Dispatchers.Default) {
                    aimTemp = Entry(currentPointIndex.toFloat(), deviceData.aimTemp)
                    enTemp = Entry(currentPointIndex.toFloat(), deviceData.environmentTemp)
                    currentPointIndex++
                }
                setAimData(chart, aimTemp)
                setEnData(chart, enTemp)
            }

        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindBLECommService()
    }

    private fun xStamp2Time(timeStamp: String, format: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val instant = Instant.ofEpochSecond(timeStamp.toLong())
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            zonedDateTime.format(DateTimeFormatter.ofPattern(format))
        } else {
            val date = Date(timeStamp)
            val simpleDateFormat = SimpleDateFormat(format)
            simpleDateFormat.format(date)
        }
    }

    /**
     * 节省时间，不处理 mcu 无关应答
     */
    fun sendLimit() {

        val limits: FloatArray = FloatArray(6).apply {
            plus(listOf(33.3f, 34f, 35f, 36f, 37f, 38f))
        }

        Log.d(TAG, "sendLimit $limits")
        fR80xDevice.setLimitTemp(limits)
    }

    //-------------------------------------------- ble 蓝牙通信 服务-----------------------------------------//
    private var bleCommService: BLECommService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            bleCommService = (service as LocalBinder).service
            bleCommService?.initialize()

        }

        override fun onServiceDisconnected(name: ComponentName) {
            bleCommService = null

        }
    }

    private fun bindBLECommService() {
        bindService(Intent(this, BLECommService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }

    private fun unbindBLECommService() {
        unbindService(serviceConnection)
    }

    //-------------------------------------------- ble 蓝牙通信 服务-----------------------------------------//

    inner class Clicker {
        fun changeConnectState(current: Boolean, mac: String) {
            Log.d(TAG, "changeConnectState $Boolean")

            if (current) {
                bleCommService?.disConnect()
                currentPointIndex = 0
                temps.clear()
            } else {
                if (TextUtils.isEmpty(mac)) return
                bleCommService?.connect(mac, fR80xDevice)
                macConnect = mac
            }
        }

        fun getTempFormDB(mac: String) {

            MainScope().launch(Dispatchers.Default) {
                val deviceTemp = tempDao.getDeviceTemp(mac)
                val tempDats = arrayListOf<TempData>()
                var aimTemp: Entry
                var enTemp: Entry

                for (temp in 0..deviceTemp.lastIndex) {
                    var tempdb = deviceTemp[temp]
                    tempDats.add(
                        TempData(
                            tempdb.enTemp ?: "",
                            tempdb.aimTemp ?: "",
                            xStamp2Time(tempdb.timestamp ?: "", "MM-dd HH:mm")
                        )

                    )

                    setAimData(chart, Entry(temp.toFloat(), tempdb.aimTemp?.toFloat() ?: 0f))
                    setEnData(chart, Entry(temp.toFloat(), tempdb.enTemp?.toFloat() ?: 0f))
                }



                withContext(Dispatchers.Main) {
                    binding.rvTempData.models = tempDats


                }


            }


        }
    }

}