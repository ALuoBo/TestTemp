package com.lifwear.testtemp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.sql.Struct

/**
 * @author WillXia
 * @date 2022/2/10.
 */
class PageViewModel : ViewModel() {
      val connectStatus = MutableLiveData(false)
      val btText = MutableLiveData("连接")
}