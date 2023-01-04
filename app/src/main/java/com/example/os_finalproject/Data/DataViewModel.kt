package com.example.os_finalproject.Data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DataViewModel : ViewModel() {

    val userName = MutableLiveData<String>()
    fun updateUserName(userName: String) {
        this.userName.postValue(userName)
    }

    val controller = MutableLiveData<Controller>()
    fun updateController(controller: Controller) {
        this.controller.postValue(controller)
    }
}