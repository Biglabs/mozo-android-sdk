package com.biglabs.mozo.example.shopper

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.biglabs.mozo.example.shopper.databinding.ActivityDemoSignMessageBinding
import io.mozocoin.sdk.MozoTx
import io.mozocoin.sdk.utils.click
import io.mozocoin.sdk.utils.copyWithToast

class DemoSignMessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDemoSignMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoSignMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSign.setOnClickListener {

            MozoTx.getInstance().signMessages(
                    this,
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
                            .append("\n\n\n")
                }
                binding.textResult.text = stringBuilder
            }
        }

        binding.textResult.click {
            val text = it.text?.toString()
            copyWithToast(text)

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }

    override fun onResume() {
        super.onResume()

        val clipBoardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipBoardManager.addPrimaryClipChangedListener {
            val copiedString = clipBoardManager.primaryClip?.getItemAt(0)?.text?.toString()
            if (binding.inputToSign.length() == 0) {
                binding.inputToSign.setText(copiedString)
            }
        }
    }
}