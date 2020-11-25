package com.biglabs.mozo.example.shopper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.biglabs.mozo.example.shopper.databinding.ActivityDemoSignMessageBinding
import io.mozocoin.sdk.MozoTx

class DemoSignMessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDemoSignMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoSignMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSign.setOnClickListener {
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
            MozoTx.getInstance().signMessages(
                    this,
                    binding.inputToSign.text.toString(),
                    binding.inputToSign.text.toString()
            ) { result ->

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
                binding.textResult.text = stringBuilder
            }
        }
    }
}