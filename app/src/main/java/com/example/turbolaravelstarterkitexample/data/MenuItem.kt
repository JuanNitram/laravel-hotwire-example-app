package com.example.turbolaravelstarterkitexample.data

data class MenuItem(
    val id: String,
    val title: String,
    val icon: String,
    val url: String
)

data class MenuResponse(
    val menu_items: List<MenuItem>,
    val version: String,
    val last_updated: String
)
