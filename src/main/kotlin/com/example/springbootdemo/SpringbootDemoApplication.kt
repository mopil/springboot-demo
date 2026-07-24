package com.example.springbootdemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringbootDemoApplication

fun main(args: Array<String>) {
    runApplication<SpringbootDemoApplication>(*args)
}
