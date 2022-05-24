package com.banno.gordon

import shadow.bundletool.com.android.ddmlib.IShellOutputReceiver

class ByteArrayShellOutputReceiver : IShellOutputReceiver {
    var output = ByteArray(0)
        private set

    override fun addOutput(data: ByteArray, offset: Int, length: Int) {
        val receivedBytes = ByteArray(length)
        System.arraycopy(data, offset, receivedBytes, 0, length)
        output += receivedBytes
    }

    override fun flush() {
        // NO OP
    }

    override fun isCancelled(): Boolean = false
}
