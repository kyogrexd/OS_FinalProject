package com.example.os_finalproject.Data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DataViewModel : ViewModel() {

    val userName = MutableLiveData<String>()
    fun updateUserName(userName: String) {
        this.userName.value = userName
        this.userName.postValue(userName)
    }

    val isJoin = MutableLiveData<Boolean>()
    fun updateIsJoin(isJoin: Boolean) {
        this.isJoin.value = isJoin
        this.isJoin.postValue(isJoin)
    }

    val controller = MutableLiveData<Controller>()
    fun updateController(controller: Controller) {
        this.controller.value = controller
        this.controller.postValue(controller)
    }
}