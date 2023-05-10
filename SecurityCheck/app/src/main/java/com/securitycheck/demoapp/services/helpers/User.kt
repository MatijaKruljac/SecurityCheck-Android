package com.securitycheck.demoapp.services.helpers

data class User(val id: Int, val firstName: String, val lastName: String) {

    override fun toString(): String {
        return "User(id=$id, firstName='$firstName', lastName='$lastName')"
    }
}