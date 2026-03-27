package com.example.deadmanswitch.model

data class Contact(
    val id: Int = 0,
    val name: String,
    val phone: String,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)