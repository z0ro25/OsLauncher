package com.ezla.oslauncher.controlcenter.overlay

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ezla.oslauncher.controlcenter.R
import com.ezla.oslauncher.controlcenter.databinding.CcItemNotificationBinding

// Danh sách thông báo nhóm theo app. Nhóm gộp hiện thẻ mới nhất + "N more", bấm thì xoè.
class NotificationAdapter(
    private val context: Context,
    private val onOpen: (StatusBarNotification) -> Unit,
    private val onDismiss: (List<StatusBarNotification>) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.Holder>() {

    private sealed class Row {
        data class Group(val pkg: String, val items: List<StatusBarNotification>) : Row()
        data class Single(val sbn: StatusBarNotification) : Row()
    }

    private val expanded = mutableSetOf<String>()
    private var groups: List<Pair<String, List<StatusBarNotification>>> = emptyList()
    private var rows: List<Row> = emptyList()

    fun submit(all: List<StatusBarNotification>) {
        // Bỏ thông báo thường trực / phiên nhạc / nhóm tóm tắt: không phải "tin" để đọc.
        val visible = all.filter { sbn ->
            val n = sbn.notification
            sbn.isClearable && n.flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
                n.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) != true
        }.sortedByDescending { it.postTime }
        groups = visible.groupBy { it.packageName }.toList()
        expanded.retainAll(groups.map { it.first }.toSet())
        rebuild()
    }

    val isEmpty get() = rows.isEmpty()

    private fun rebuild() {
        rows = groups.flatMap { (pkg, items) ->
            if (items.size > 1 && pkg !in expanded) listOf(Row.Group(pkg, items))
            else items.map { Row.Single(it) }
        }
        notifyDataSetChanged()
    }

    class Holder(val b: CcItemNotificationBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(CcItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: Holder, position: Int) {
        val b = h.b
        when (val row = rows[position]) {
            is Row.Group -> {
                bindCard(b, row.items.first())
                b.ncStack.visibility = View.VISIBLE
                b.ncMore.visibility = View.VISIBLE
                b.ncMore.text = context.getString(R.string.cc_more_notifications, row.items.size - 1)
                b.ncCard.setOnClickListener {
                    expanded += row.pkg
                    rebuild()
                }
            }
            is Row.Single -> {
                bindCard(b, row.sbn)
                b.ncStack.visibility = View.GONE
                b.ncMore.visibility = View.GONE
                b.ncCard.setOnClickListener { onOpen(row.sbn) }
            }
        }
    }

    private fun bindCard(b: CcItemNotificationBinding, sbn: StatusBarNotification) {
        val pm = context.packageManager
        val ex = sbn.notification.extras
        b.ncAppName.text = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)) }
            .getOrDefault(sbn.packageName)
        b.ncAppIcon.setImageDrawable(runCatching { pm.getApplicationIcon(sbn.packageName) }.getOrNull())
        b.ncTitle.text = ex?.getCharSequence(Notification.EXTRA_TITLE) ?: ""
        val text = ex?.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: ex?.getCharSequence(Notification.EXTRA_TEXT)
        b.ncText.text = text ?: ""
        b.ncText.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        b.ncTimeAgo.text = timeAgo(sbn.postTime)
    }

    private fun timeAgo(time: Long): String {
        val min = ((System.currentTimeMillis() - time) / 60_000).coerceAtLeast(0)
        return when {
            min < 1 -> context.getString(R.string.cc_just_now)
            min < 60 -> context.getString(R.string.cc_minutes_ago, min.toInt())
            min < 60 * 24 -> context.getString(R.string.cc_hours_ago, (min / 60).toInt())
            else -> context.getString(R.string.cc_days_ago, (min / 1440).toInt())
        }
    }

    // Vuốt ngang để xoá: nhóm đang gộp thì xoá cả nhóm.
    fun attachSwipe(list: RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                val row = rows.getOrNull(pos) ?: return
                val removed = when (row) {
                    is Row.Group -> row.items
                    is Row.Single -> listOf(row.sbn)
                }
                rows = rows.toMutableList().also { it.removeAt(pos) }
                notifyItemRemoved(pos)
                onDismiss(removed)
            }
        }).attachToRecyclerView(list)
    }
}
