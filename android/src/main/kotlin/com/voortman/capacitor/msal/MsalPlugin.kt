package com.voortman.capacitor.msal

import com.getcapacitor.Logger

class MsalPlugin {

    fun echo(value: String): String {
        Logger.info("Echo", value)

        return value
    }
}
