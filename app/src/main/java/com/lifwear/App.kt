package com.lifwear

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.drake.brv.utils.BRV
import com.lifwear.testtemp.BR

/**
 * @author WillXia
 * @date 2022/1/28.
 */
class App : Application() {



    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        // 初始化BindingAdapter的默认绑定ID, 如果不使用DataBinding并不需要初始化
        // 但是一旦声明BRV.model = BR.m你的所有BRV使用的item布局都得使用name="m"来声明数据模型, 否则会无法自动绑定
        // 当然你也可以在onBind里面手动绑定, 但是肯定比自动麻烦, 而且名称本身只是代号我建议都使用m
        BRV.modelId = BR.m
    }

    companion object{
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context
        fun getContext():Context{
            return this.context
        }
    }

}