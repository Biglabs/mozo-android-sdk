package com.biglabs.mozo.example.shopper

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.biglabs.mozo.sdk.MozoNotification
import com.biglabs.mozo.sdk.common.OnNotificationReceiveListener
import com.biglabs.mozo.sdk.common.model.Notification
import kotlinx.android.synthetic.main.fragment_notification.*
import java.text.SimpleDateFormat
import java.util.*

class NotificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_notification, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notifications = arrayListOf<Notification>()
        val adapter = NotificationAdapter(notifications)
        recycler_notification.setHasFixedSize(true)
        recycler_notification.addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        recycler_notification.adapter = adapter

        MozoNotification.getAll {
            notifications.addAll(it)
            adapter.notifyDataSetChanged()
        }

        MozoNotification.setNotificationReceiveListener(object : OnNotificationReceiveListener {
            override fun onReveiced(notification: Notification) {
                notifications.add(0, notification)
                adapter.notifyDataSetChanged()
            }
        })
    }

    class NotificationAdapter(private val notifications: List<Notification>) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false))

        override fun getItemCount(): Int = notifications.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(notifications[position])
        }

        class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            fun bind(notification: Notification) {
                view.findViewById<ImageView>(R.id.item_icon)?.setImageResource(notification.icon)
                view.findViewById<TextView>(R.id.item_title)?.text = notification.titleDisplay()
                view.findViewById<TextView>(R.id.item_content)?.text = notification.contentDisplay()

                val format = SimpleDateFormat("HH:mm:ss dd MMM yyyy", Locale.US).format(Date(notification.time))
                view.findViewById<TextView>(R.id.item_time)?.text = format

                view.setBackgroundColor(if(notification.read) Color.TRANSPARENT else Color.parseColor("#7FFFDAB0"))
            }
        }
    }
}