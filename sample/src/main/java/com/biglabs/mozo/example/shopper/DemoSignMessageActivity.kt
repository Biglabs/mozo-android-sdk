package com.biglabs.mozo.example.shopper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.mozocoin.sdk.MozoTx
import kotlinx.android.synthetic.main.activity_demo_sign_message.*

class DemoSignMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_sign_message)

        button_sign.setOnClickListener {
            /*
            MozoTx.getInstance().signMessage(this, input_to_sign.text.toString()) { message, signature, publicKey ->
                text_result.text = StringBuilder()
                        .append("message: ")
                        .append(message)
                        .append("\n\n")
                        .append("signature: ")
                        .append(signature)
                        .append("\n\n")
                        .append("publicKey: ")
                        .append(publicKey)
            }
            */
            MozoTx.getInstance().signMessages(this, input_to_sign.text.toString(), input_to_sign.text.toString()) { result ->

                val stringBuilder = StringBuilder()
                result.mapIndexed { index, value ->
                    stringBuilder.append("$index \nmessage: ")
                            .append(value.first)
                            .append("\n\n")
                            .append("signature: ")
                            .append(value.second)
                            .append("\n\n")
                            .append("publicKey: ")
                            .append(value.third)
                            .append("\n\n")
                }
                text_result.text = stringBuilder
            }
        }
    }
}